package com.example.StreamToZip;

import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * NIO压缩
 */
public interface StreamZipService {

    void zipOutputStreamExample(File outputZipFile, List<File> filesToAdd, ZipParameters zipParameters, String password)
            throws IOException;


    ZipParameters buildZipParameters(boolean encrypt,
                                     EncryptionMethod encryptionMethod, AesKeyStrength aesKeyStrength);
}
