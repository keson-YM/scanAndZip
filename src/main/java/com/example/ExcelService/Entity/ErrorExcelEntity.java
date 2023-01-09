package com.example.ExcelService.Entity;

import com.alibaba.excel.annotation.ExcelProperty;

public class ErrorExcelEntity {

	@ExcelProperty("路径文件")
	String path;

	@ExcelProperty("失败原因")
	String content;

	@ExcelProperty("大小")
	Double size;
	@ExcelProperty("单位")
	String volume = "MB";


	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Double getSize() {
		return size;
	}

	public void setSize(Double size) {
		this.size = size;
	}
}
