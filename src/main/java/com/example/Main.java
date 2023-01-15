package com.example;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import com.example.ExcelService.XslService;
import com.example.ExcelService.XslServiceImpl;
import com.example.StreamToZip.StreamZipService;
import com.example.StreamToZip.StreamZipServiceImpl;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.apache.commons.io.FileUtils;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Main {

    //E:\JavaWorkSpace\测试data
    private static String path = "D:\\测试data\\data";

    private static String zipPath = "D:\\测试data";

    public static Map<String, List<File>> fileMap = new HashMap<>();

    private static Integer vipType = 0; //必填  0：单个文件最大4G   1： 单个文件最大10G  2：单个文件最大20G

    private static final List<BigDecimal> sizes = Arrays.asList(new BigDecimal("4096"), new BigDecimal("10240"), new BigDecimal("20480"));

    private static String password = "123223"; //必填 压缩包密码`

    public static String excelPath = "D:\\测试data";

    public static String excelName = "测试";

    private XslService excelService = new XslServiceImpl();
    private StreamZipService zipService = new StreamZipServiceImpl();
    //随机生成密钥
    static final byte[] key = SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue()).getEncoded();

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
            Integer size = files.size();
            for (int i = 0; i < size; i++) {
                if (path == null) path = files.get(i).getPath();
                BigDecimal oneFileSize = computed(files.get(i).length());
                String hashParent = parentCrypto(parent);
                if (oneFileSize.compareTo(sizes.get(vipType)) == 1) {
                    //写失败Excel
                    excelService.writeErrorXsl(path, "文件太大", oneFileSize.doubleValue());
                } else {
                    //计算大小
                    fileSize += files.get(i).length();
                    if (computed(fileSize).compareTo(sizes.get(vipType)) == -1) {
                        doZipList.add(files.get(i));
                    }else{
                        excelService.writeErrorXsl(path,"加上这个文件就太大了", oneFileSize.doubleValue());
                        zip(hashParent, doZipList);
                        excelService.writeXsl(parent, hashParent, "", hashParent, Arrays.toString(key), password);
                        doZipList = new ArrayList<>();
                        fileSize = 0L;
                    }
                    if (i == files.size() - 1) {
                        //压缩
                        zip(hashParent, doZipList);
                        doZipList = new ArrayList<>();
                        fileSize = 0L;

                    } /*else {
                        if (files.indexOf(files.get(i)) != files.size() - 1) {
                            Integer begin = i + 1;
                            Integer end = files.size() - 1;
                            files = files.subList(begin, end);
                            fileSize = 0L;
                            size -= fileMap.size();

                            i = 0;
                        } else {
                            files = null;
                            files = new ArrayList<>();
                            files.add(files.get(i));
                            zip(parent, doZipList);
                            fileSize = 0L;
                        }
                    }*/
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
    public static BigDecimal computed(Long fileSize) {
        Double value = (double) fileSize / (1024 * 1024);
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 压缩并加密
     *
     * @param parent
     * @param files
     * @return
     */
    public Map<String, Object> zip(String hashParent, List<File> files) {
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
        String zipName = zipPath + "/" + hashParent + ".zip";
        ZipFile zipFile = new ZipFile(zipName, password.toCharArray());

        try {
            zipFile.addFiles(files, zipParameters);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                zipFile.close();

                System.gc();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return null;
    }

    private static String parentCrypto(String parent){
        SymmetricCrypto aes = new SymmetricCrypto(SymmetricAlgorithm.AES, key);
        return aes.encryptHex(parent);
    }

}

