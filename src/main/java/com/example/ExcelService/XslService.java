package com.example.ExcelService;

import com.example.ExcelService.Entity.ErrorExcelEntity;

import java.util.List;

public interface XslService {
	void writeXsl(String parent, String hashParent,
				  String originalZipName, String hashZipName,
				   String aesKey,
				  String zipPassword);

	void writeErrorXsl(String path, String content, Double size);

	void doWrite();

	void errorExcel();

    List<ErrorExcelEntity> readErrorExcel(String path);

	void writeFinalError(String path, List<ErrorExcelEntity> entities);
}
