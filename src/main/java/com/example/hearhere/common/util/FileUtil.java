package com.example.hearhere.common.util;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;

@Component
public class FileUtil {
    public File downloadFile(String fileUrl) throws Exception {
        URL url = new URL(fileUrl);
        File tempFile = File.createTempFile("temp", ".wav");
        FileUtils.copyURLToFile(url, tempFile);
        return tempFile;
    }

    public static boolean isWavFile(File file) {
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".wav");
    }
}
