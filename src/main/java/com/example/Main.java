package com.example;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {


	private static String path = "";

	private static String zipPath = "";

	public static Map<String, List<File>> fileMap = new HashMap<>();

	private static Integer vipType = 0; //必填  0：单个文件最大4G   1： 单个文件最大10G  2：单个文件最大20G

	private static Double[] sizes = {4096D, 10240D, 20480D};

	private static String password = ""; //必填 压缩包密码

	private static String excelPath = "";

	private static String excelName = "";

	private static List<List<String>> head = new ArrayList<>();
	private static String[] heads = {"原父目录", "父目录", "原完整路径", "路径", "文件名", "原文件名"};

	private static List<ExcelEntity> dataList = new ArrayList<>();

	public static void main(String[] args) {
		for (String item : heads) {
			List<String> list = new ArrayList<>();
			list.add(item);
			head.add(list);
		}
		File dir = new File(path);
		if (dir.exists()) {
			scan(dir.listFiles());
		}
		zip();
		doWrite();
	}

	public static void scan(File[] dir) {
		for (File file : dir) {
			if (file.isDirectory()) {
				scan(file.listFiles());
			} else {
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


	public static void zip() {
		Long fileSize = 0L;

		List<File> doZipList = null;
		Set<Map.Entry<String, List<File>>> keySet = fileMap.entrySet();
		for (Map.Entry<String, List<File>> item : keySet) {
			String parent = item.getKey();
			List<File> files = item.getValue();
			doZipList = new ArrayList<>();
			for (File file : files) {
				//计算大小
				fileSize += file.length();
				if (computed(fileSize) < sizes[vipType]) {
					doZipList.add(file);
				} else {
					//压缩
					zip(parent, doZipList);
					fileSize = 0L;
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
	public static Map<String, Object> zip(String parent, List<File> files) {

		return null;
	}

	/**
	 * 记录Excel
	 *
	 * @param parent
	 * @param hashParent
	 * @param originZipName
	 * @param hashZipName
	 */
	public static void writeXsl(String parent, String hashParent,
								String originalZipName, String hashZipName,
								String path, String originalPath) {

		ExcelEntity entity = new ExcelEntity();
		entity.setOriginalParen(parent);
		entity.setParent(hashParent);
		entity.setOriginalName(originalZipName);
		entity.setName(hashZipName);
		entity.setPath(path);
		entity.setOriginalPath(originalPath);
		dataList.add(entity);
	}


	/**
	 * 写入excel文件
	 */
	public static void doWrite() {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		if (!excelPath.endsWith("/")) {
			excelPath = excelPath.replace("\\", "/");
			excelPath += "/";
		}
		if (!excelName.contains("xlsx") || !excelName.contains("excel")) excelName = excelName += ".xlsx";
		String name = excelPath + format.format(new Date()) + excelName;
		EasyExcel.write(name).head(head)
				.registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
				.sheet().doWrite(dataList);
	}
}

class ExcelEntity {
	@ExcelProperty("原父目录")
	private String originalParen;
	@ExcelProperty("父目录")
	private String parent;
	@ExcelProperty("原完整路径")
	private String originalPath;
	@ExcelProperty("路径")
	private String path;

	@ExcelProperty("文件名")
	private String name;
	@ExcelProperty("原文件名")
	private String originalName;

	public String getOriginalParen() {
		return originalParen;
	}

	public void setOriginalParen(String originalParen) {
		this.originalParen = originalParen;
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	public String getOriginalPath() {
		return originalPath;
	}

	public void setOriginalPath(String originalPath) {
		this.originalPath = originalPath;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOriginalName() {
		return originalName;
	}

	public void setOriginalName(String originalName) {
		this.originalName = originalName;
	}

	public void setPath(String path) {
		this.path = path;
	}
}