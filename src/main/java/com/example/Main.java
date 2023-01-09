package com.example;

import cn.hutool.core.util.HashUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import cn.hutool.*;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import java.io.File;
import java.io.IOException;
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
	private static String[] heads = {"原父目录", "父目录", "原完整路径", "路径", "文件名", "原文件名", "AESkey", "密码"};

	private static List<ExcelEntity> dataList = new ArrayList<>();

	public static void main(String[] args) {
		for (String item : heads) {
			List<String> list = new ArrayList<>();
			list.add(item);
			head.add(list);
		}
		File dir = new File(path);
		List<File> dirs = Arrays.asList(dir.listFiles());

		if (dir.exists()) {
			scan(dirs);
		}
		zip();
		doWrite();
	}

	public static void scan(List<File> dir) {
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
		Iterator<Map.Entry<String, List<File>>> keySet = fileMap.entrySet().iterator();
		while (keySet.hasNext()) {
			Map.Entry<String, List<File>> item = keySet.next();
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
		ZipFile zipFile = new ZipFile(zipPath + parent + "/" + hashParent + ".zip", password.toCharArray());
		try {
			for (File file : files) {
				String filePath = file.getPath();
				String hashPath = aes.encryptHex(filePath);

				zipFile.addFile(file, zipParameters);

				writeXsl(parent, hashParent, "", hashParent, hashPath, filePath, Arrays.toString(key));
			}
		} catch (ZipException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				zipFile.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			System.gc();
		}
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
								String path, String originalPath, String aesKey) {

		ExcelEntity entity = new ExcelEntity();
		entity.setOriginalParen(parent);
		entity.setParent(hashParent);
		entity.setOriginalName(originalZipName);
		entity.setName(hashZipName);
		entity.setPath(path);
		entity.setOriginalPath(originalPath);
		entity.setAesKey(aesKey);
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

	@ExcelProperty("AESkey")
	private String aesKey;

	@ExcelProperty("密码")
	private String zipPassword;

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

	public String getPath() {
		return path;
	}

	public String getAesKey() {
		return aesKey;
	}

	public void setAesKey(String aesKey) {
		this.aesKey = aesKey;

	}

	public String getZipPassword() {
		return zipPassword;
	}

	public void setZipPassword(String zipPassword) {
		this.zipPassword = zipPassword;
	}
}