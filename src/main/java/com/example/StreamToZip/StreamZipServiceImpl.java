package com.example.StreamToZip;

import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import java.io.*;
import java.util.List;

public class StreamZipServiceImpl implements StreamZipService {


    @Override
    public void zipOutputStreamExample(File outputZipFile, List<File> filesToAdd, ZipParameters zipParameters, String password)
            throws IOException {


        byte[] buff = new byte[8192];
        int readLen;

        try (ZipOutputStream zos = initializeZipOutputStream(outputZipFile, password)) {
            for (File fileToAdd : filesToAdd) {

                // Entry size has to be set if you want to add entries of STORE compression method (no compression)
                // This is not required for deflate compression


                zipParameters.setFileNameInZip(fileToAdd.getName());
                zos.putNextEntry(zipParameters);

                try (InputStream inputStream = new FileInputStream(fileToAdd)) {
                    while ((readLen = inputStream.read(buff)) != -1) {
                        zos.write(buff, 0, readLen);
                    }
                }
                zos.closeEntry();
                System.gc();
            }
        }
    }


    public ZipOutputStream initializeZipOutputStream(File outputZipFile, String password)
            throws IOException {

        FileOutputStream fos = new FileOutputStream(outputZipFile);


        return new ZipOutputStream(fos, password.toCharArray());

    }

    @Override
    public ZipParameters buildZipParameters(boolean encrypt,
                                            EncryptionMethod encryptionMethod, AesKeyStrength aesKeyStrength) {
        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setEncryptionMethod(encryptionMethod);
        zipParameters.setAesKeyStrength(aesKeyStrength);
        zipParameters.setEncryptFiles(encrypt);
        return zipParameters;
    }
}
