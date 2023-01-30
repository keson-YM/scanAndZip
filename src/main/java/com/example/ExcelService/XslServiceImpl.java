package com.example.ExcelService;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.metadata.Sheet;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;

import com.example.ExcelService.Entity.ErrorExcelEntity;
import com.example.ExcelService.Entity.ExcelEntity;
import com.example.Main;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class XslServiceImpl implements XslService {
    private static String[] heads = {"原父目录", "父目录", "原完整路径", "路径", "文件名", "原文件名", "AESkey", "密码"};
    private static String[] errorHeads = {"路径文件", "失败原因", "大小", "单位"};

    private static List<List<String>> head = new ArrayList<>();
    private static List<List<String>> errorHead = new ArrayList<>();

    private static List<ExcelEntity> dataList = new ArrayList<>();
    private static List<ErrorExcelEntity> errorDataList = new ArrayList<>();

    /**
     * 记录Excel
     *
     * @param parent
     * @param hashParent
     * @param originZipName
     * @param hashZipName
     */
    @Override
    public void writeXsl(String parent, String hashParent,
                         String originalZipName, String hashZipName,
                         String aesKey,
                         String zipPassword) {

        ExcelEntity entity = new ExcelEntity();
        entity.setOriginalParen(parent);
        entity.setParent(hashParent);
        entity.setOriginalName(originalZipName);
        entity.setName(hashZipName);
        entity.setAesKey(aesKey);
        entity.setZipPassword(zipPassword);
        dataList.add(entity);
    }

    /**
     * 记录出错的Excel
     *
     * @param path
     * @param content
     * @param size
     */
    @Override
    public void writeErrorXsl(String path, String content, Double size) {
        ErrorExcelEntity entity = new ErrorExcelEntity();
        entity.setPath(path);
        entity.setContent(content);
        entity.setSize(size);
        errorDataList.add(entity);
    }


    /**
     * 写入excel文件
     */
    @Override
    public void doWrite() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        if (!Main.excelPath.endsWith("/")) {
            Main.excelPath = Main.excelPath.replace("\\", "/");
            Main.excelPath += "/";
        }
        if (!Main.excelName.contains("xlsx") || !Main.excelName.contains("excel"))
            Main.excelName = Main.excelName += ".xlsx";
        String name = Main.excelPath + format.format(new Date()) + Main.excelName;
        EasyExcel.write(name).head(head)
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .sheet().doWrite(dataList);
    }

    /**
     * 无法压缩的文件写入 失败Excel
     */
    @Override
    public void errorExcel() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        if (!Main.excelPath.endsWith("/")) {
            Main.excelPath = Main.excelPath.replace("\\", "/");
            Main.excelPath += "/";
        }
        String name = Main.excelPath + "/" + format.format(new Date()) + "压缩失败文件.xlsx";
        EasyExcel.write(name).head(errorHead)
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .sheet().doWrite(errorDataList);
    }


    @Override
    public List<ErrorExcelEntity> readErrorExcel(String path) {
        if (!path.endsWith("/")) path+="/";
        path += "压缩失败文件.xlsx";
        Sheet sheet = new Sheet(0);
        List<ErrorExcelEntity> entities = null;
        try {
            List<Object> objects = EasyExcelFactory.read(new FileInputStream(path), sheet);
            entities = new ArrayList<>();
            for (Object o : objects) {
                entities.add((ErrorExcelEntity) o);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        return entities;
    }
    @Override
    public void writeFinalError(String path,List<ErrorExcelEntity> entities){
        if (!path.endsWith("/")) path+="/";
        path+="最终压缩失败.xlsx";
        EasyExcel.write(path).head(errorHead)
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .sheet().doWrite(entities);
    }


    public XslServiceImpl() {
        for (String item : heads) {
            List<String> list = new ArrayList<>();
            list.add(item);
            head.add(list);
        }
        for (String item : errorHeads) {
            List<String> list = new ArrayList<>();
            list.add(item);
            errorHead.add(list);
        }
    }

}
