package com.example;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.example.ExcelService.XslService;
import com.example.ExcelService.XslServiceImpl;
import com.example.StreamToZip.StreamZipService;
import com.example.StreamToZip.StreamZipServiceImpl;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {

    //E:\JavaWorkSpace\测试data
    private static String path = "";

    private static String zipPath = "";

    public static Map<String, List<File>> fileMap = new HashMap<>();

    private static Integer vipType = 0; //必填  0：单个文件最大4G   1： 单个文件最大10G  2：单个文件最大20G

    private static Double[] sizes = {4096D, 10240D, 20480D};

    private static String password = "123223"; //必填 压缩包密码

    public static String excelPath = "";

    public static String excelName = "测试";

    private XslService excelService = new XslServiceImpl();
    private StreamZipService zipService = new StreamZipServiceImpl();


	public static void main(String[] args) {
        Long beginTime = System.currentTimeMillis();
        File dir = new File(path);
        List<File> dirs = Arrays.asList(dir.listFiles());
        Main main = new Main();
        if (dir.exists()) {
            main.scan(dirs);
        }
        main.zip();
        main.excelService.doWrite();
        main.excelService.errorExcel();
        Long endTime = System.currentTimeMillis();
        System.out.println((Double.valueOf(endTime) - Double.valueOf(beginTime)) / 1000D);
    }

    public void scan(List<File> dir) {
        Collections.sort(dir, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                if (o1.isDirectory() && o2.isFile()) {
                    return 1;
                }
                if (o1.isFile() && o2.isDirectory()) {
                    return -1;
                }
                return 0;
            }
        });
        for (File file : dir) {
            if (file.isDirectory()) {
                scan(Arrays.asList(file.listFiles()));
            } else {
                System.out.println(file);
                String parent = file.getParent();
                List<File> fileList = null;
                if (fileMap.get(parent) != null) {
                    fileList = fileMap.get(parent);
                    fileList.add(file);
                } else {
                    fileList = new LinkedList<>();
                    fileList.add(file);
                    fileMap.put(parent, fileList);
                }
            }
        }
    }


    public void zip() {
        Long fileSize = 0L;

        List<File> doZipList = null;
        Iterator<Map.Entry<String, List<File>>> keySet = fileMap.entrySet().iterator();
        while (keySet.hasNext()) {
            Map.Entry<String, List<File>> item = keySet.next();
            String parent = item.getKey();
            List<File> files = item.getValue();
            doZipList = new ArrayList<>();
            String path = null;
            for (File file : files) {
                if (path == null) path = file.getPath();
                Double oneFileSize = computed(file.length());
                if (oneFileSize > sizes[vipType]) {
                    //写失败Excel
                    excelService.writeErrorXsl(path, "文件太大", oneFileSize);
                } else {
                    //计算大小
                    fileSize += file.length();
                    if (computed(fileSize) < sizes[vipType] && files.indexOf(file) != files.size() - 1) {
                        doZipList.add(file);
                    } else if (computed(fileSize) < sizes[vipType] && files.indexOf(file) == files.size() - 1) {
                        doZipList.add(file);
                        //压缩
                        zip(parent, doZipList);
                        fileSize = 0L;
                    } else {
                        if (files.indexOf(file) != files.size() - 1) {
                            Integer begin = files.indexOf(file) + 1;
                            Integer end = files.size() - 1;
                            files = files.subList(begin, end);
                            fileSize = 0L;
                        } else {
                            files = null;
                            files = new ArrayList<>();
                            files.add(file);
                            zip(parent, doZipList);
                            fileSize = 0L;
                        }
                    }
                }

            }
        }
    }

    /**
     * 计算大小
     *
     * @param fileSize file.length();
     * @return 单位：MB
     */
    public static Double computed(Long fileSize) {
        return (double) fileSize / (1024 * 1024);
    }

    /**
     * 压缩并加密
     *
     * @param parent
     * @param files
     * @return
     */
    public Map<String, Object> zip(String parent,  List<File> files) {
        //1.压缩工具类build
		ZipParameters zipParameters = new ZipParameters();
		zipParameters.setEncryptFiles(true);
		zipParameters.setEncryptionMethod(EncryptionMethod.AES);
		zipParameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);

		if (!zipPath.endsWith("/")) {
			zipPath = zipPath.replace("\\", "/");
			zipPath += "/";
		}

		//1.加密parent
		//随机生成密钥
		byte[] key = SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue()).getEncoded();
		SymmetricCrypto aes = new SymmetricCrypto(SymmetricAlgorithm.AES, key);
		String hashParent = aes.encryptHex(parent);
		String zipName = zipPath + "/" + hashParent + ".zip";
		ZipFile zipFile = new ZipFile(zipName, password.toCharArray());
        zipFile.setRunInThread(true);
        FileInputStream stream = null;
        for (File file : files){
            try {
                zipParameters.setFileNameInZip(file.getName());
                stream = FileUtils.openInputStream(file);
                zipFile.addStream(stream,zipParameters);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }finally {
                try {
                    zipFile.close();
                    stream.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                System.gc();

            }
        }
        /*try {
            zipService.zipOutputStreamExample(new File(zipName),files,zipParameters,password);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }*/
        excelService.writeXsl(parent, hashParent, "", hashParent, Arrays.toString(key),password);
		return null;
	}



}

