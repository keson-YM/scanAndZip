package com.example.ExcelService;

public interface XslService {
	void writeXsl(String parent, String hashParent,
				  String originalZipName, String hashZipName,
				  String path, String originalPath, String aesKey,
				  String zipPassword);

	void writeErrorXsl(String path, String content, Double size);

	void doWrite();

	void errorExcel();
}
