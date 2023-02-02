package com.example;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import com.example.ExcelService.Entity.ErrorExcelEntity;
import com.example.ExcelService.XslService;
import com.example.ExcelService.XslServiceImpl;
import com.example.StreamToZip.StreamZipService;
import com.example.StreamToZip.StreamZipServiceImpl;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.formula.functions.T;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Main {

    //E:\JavaWorkSpace\测试data
    private static String path = "E:\\JavaWorkSpace\\测试data";

    private static String zipPath = "E:\\JavaWorkSpace\\";

    public static Map<String, List<File>> fileMap = new HashMap<>();

    public static Map<String, List<File>> bigFileMap = new HashMap<>();

    private static Integer vipType = 0; //必填  0：单个文件最大4G   1： 单个文件最大10G  2：单个文件最大20G

    private static final List<BigDecimal> sizes = Arrays.asList(
            new BigDecimal("4096"),
            new BigDecimal("10240"),
            new BigDecimal("20480"));

    private static String password = "123223"; //必填 压缩包密码`

    public static String excelPath = "E:\\JavaWorkSpace\\test";

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
        main.zip(fileMap);
        main.excelService.doWrite();
        main.excelService.errorExcel();
        //主要压缩流程走完以后走ErrorExcel流程
     /*   List<ErrorExcelEntity> entities = main.excelService.readErrorExcel(excelPath);
        main.zip(entities);*/
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


    public void zip(Map<String, List<File>> didMap) {
        List<File> doZipList = null;
        Iterator<Map.Entry<String, List<File>>> keySet = didMap.entrySet().iterator();
        while (keySet.hasNext()) {
            Long fileSize = 0L;
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
                Integer length = hashParent.length();
                hashParent = hashParent.substring(0, (length - 1) / 4);

                if (oneFileSize.compareTo(sizes.get(vipType)) == 1) {
                    //写失败Excel
                    excelService.writeErrorXsl(path, "文件太大", oneFileSize.doubleValue());
                    files.remove(i);
                    size -= 1;
                    i--;

                } else {
                    //计算大小
                    fileSize += files.get(i).length();
                    //如果当 （前压缩文件大小 + files[i]个文件大小 ) < 百度网盘限制大小 就添加files[i]个文件进入队列
                    if (computed(fileSize).compareTo(sizes.get(vipType)) == -1) {
                        doZipList.add(files.get(i));
                    } else {
                        List<File> finalDoZipList = doZipList;
                        nioZip(genTheZipFile(hashParent), finalDoZipList);
                        excelService.writeXsl(parent, hashParent, "", hashParent, Arrays.toString(key), password);
                        files = files.subList(i, files.size() - 1);
                        doZipList.clear();
                        size = files.size();
                        fileSize = 0L;
                        i = 0;
                        /*files.remove(i);
                        size--;
                        */
                    }
                    if (i == files.size() - 1) {
                        //压缩
                        File zipFile = genTheZipFile(hashParent);
                        nioZip(zipFile, doZipList);
                        // zip(hashParent, doZipList);
                        excelService.writeXsl(parent, zipFile.getName(), "", hashParent, Arrays.toString(key), password);
                        doZipList.clear();
                        fileSize = 0L;
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
    public static BigDecimal computed(Long fileSize) {
        Double value = (double) fileSize / (1024 * 1024);
        return new BigDecimal(value);
    }

    /**
     * 压缩并加密
     *
     * @param parent
     * @param files
     * @return
     */
    public Map<String, Object> zip(String hashParent, List<File> files) {

        if (files != null && files.size() > 0) {

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
        }
        return null;
    }

    private void nioZip(File zipFile, List<File> files) {
        if (files != null && files.size() > 0) {

            //1.压缩工具类build
            ZipParameters zipParameters = new ZipParameters();
            zipParameters.setEncryptFiles(true);
            zipParameters.setEncryptionMethod(EncryptionMethod.AES);
            zipParameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
//            zipParameters.setCompressionMethod(CompressionMethod.STORE);
            if (!zipPath.endsWith("/")) {
                zipPath = zipPath.replace("\\", "/");
                zipPath += "/";
            }

            //1.加密parent

            try {
                zipService.zipOutputStreamExample(zipFile, files, zipParameters, password);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static AtomicInteger integer = null;

    private static File genTheZipFile(String hashParent) {
        String zipName = zipPath + "/" + hashParent + ".zip";
        File zipFile = new File(zipName);
        if (zipFile.exists()) {
            if (integer == null) integer = new AtomicInteger(1);
            zipName = zipName.replace(hashParent, hashParent += "(" + integer.get() + ")");
        }
        zipFile = new File(zipName);
        return zipFile;
    }

    private static String parentCrypto(String parent) {
        SymmetricCrypto aes = new SymmetricCrypto(SymmetricAlgorithm.RC2, key);
        return aes.encryptHex(parent);
    }
}