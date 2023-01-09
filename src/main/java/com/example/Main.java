package com.example;

import java.io.File;
import java.util.*;

public class Main {


	private static String path = "";

	private static String zipPath = "";

	public static Map<String, List<File>> fileMap = new HashMap<>();

	private static Integer vipType = 0; //必填  0：单个文件最大4G   1： 单个文件最大10G  2：单个文件最大20G

	private static Double[] sizes = {4096D,10240D,20480D};

	private static String password = ""; //必填 压缩包密码

	public static void main(String[] args) {
		File dir = new File(path);
		if (dir.exists()) {
			scan(dir.listFiles());
		}
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
		for (Map.Entry<String,List<File>> item : keySet){
			String parent = item.getKey();
			List<File> files = item.getValue();
			doZipList = new ArrayList<>();
			for (File file: files) {
				//计算大小
				fileSize += file.length();
				if (computed(fileSize) < sizes[vipType]) {
					doZipList.add(file);
				}else {
					//压缩
					zip(parent,doZipList);
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
	 * @param parent

	 * @param files
	 * @return
	 */
	public static Map<String,Object> zip (String parent,List<File> files){
		return null;
	}

	/**
	 * 记录Excel
	 * @param parent
	 * @param hashParent
	 * @param originZipName
	 * @param hashZipName
	 */
	public static void writeXsl(String parent,String hashParent,String originZipName,String hashZipName){

	}
}