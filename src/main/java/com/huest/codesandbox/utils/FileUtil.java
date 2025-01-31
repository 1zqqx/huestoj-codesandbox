// -*- coding = utf-8 -*-
// @Time : 2025/1/31
// @Author : 1zqqx
// @File : FileUtil
// @Software : IntelliJ IDEA

package com.huest.codesandbox.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtil {
    public static void writeToFile(String filePath, String content) {
        try {
            Path path = Paths.get(filePath);
            Files.write(path, content.getBytes());
            System.out.println("[=] Data written to file successfully!");
        } catch (IOException e) {
            System.err.println("[=] Error writing to file: " + e.getMessage());
        }
    }
}
