package com.example.ExcelService.Entity;

import com.alibaba.excel.annotation.ExcelProperty;

public class ExcelEntity {
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
