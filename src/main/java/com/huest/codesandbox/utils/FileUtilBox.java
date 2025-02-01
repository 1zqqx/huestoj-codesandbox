// -*- coding = utf-8 -*-
// @Time : 2025/2/1
// @Author : 1zqqx
// @File : FileUtil
// @Software : IntelliJ IDEA

package com.huest.codesandbox.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

public class FileUtilBox {

    /**
     * 严格逐行 逐字符比较
     * <p/>
     *
     * @param userFile   用户输出
     * @param answerFile 标准答案
     * @return Boolean
     * @throws IOException exception
     */
    public static boolean compareStrict(File userFile, File answerFile) throws IOException {
        try (BufferedReader userReader = new BufferedReader(new FileReader(userFile));
             BufferedReader answerReader = new BufferedReader(new FileReader(answerFile))) {

            String userLine, answerLine;
            while ((answerLine = answerReader.readLine()) != null) {
                userLine = userReader.readLine();
                // 行数不足或内容不一致
                if (userLine == null || !userLine.equals(answerLine)) {
                    return false;
                }
            }
            // 检查用户文件是否有额外行
            return userReader.readLine() == null;
        }
    }

    /**
     * 容错比较
     * 允许格式上的微小差异。
     * <p/>
     *
     * @param userFile   用户输出
     * @param answerFile 标准答案
     * @return Boolean
     * @throws IOException exception
     */
    public static boolean compareLenient(File userFile, File answerFile) throws IOException {
        try (BufferedReader userReader = new BufferedReader(new FileReader(userFile));
             BufferedReader answerReader = new BufferedReader(new FileReader(answerFile))) {

            String userLine, answerLine;
            while (true) {
                answerLine = readNextNonEmptyLine(answerReader);
                userLine = readNextNonEmptyLine(userReader);
                if (answerLine == null && userLine == null) return true;
                if (answerLine == null || userLine == null) return false;
                if (!answerLine.equals(userLine)) return false;
            }
        }
    }

    private static String readNextNonEmptyLine(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim(); // 去除行首尾空格
            if (!line.isEmpty()) return line;
        }
        return null;
    }

    /**
     * 特殊数据类型处理 浮点数容错比较
     * 允许浮点数输出存在一定误差 如 1e-5
     * <p/>
     *
     * @param userFile   用户输出
     * @param answerFile 标准输出
     * @param epsilon    误差
     * @return Boolean
     */
    public static boolean compareFloats(File userFile, File answerFile, double epsilon) {
        try (Scanner userScanner = new Scanner(userFile);
             Scanner answerScanner = new Scanner(answerFile)) {

            while (answerScanner.hasNext()) {
                if (!userScanner.hasNext()) return false;

                if (answerScanner.hasNextDouble()) {
                    double expected = answerScanner.nextDouble();
                    if (!userScanner.hasNextDouble()) return false;
                    double actual = userScanner.nextDouble();

                    if (Math.abs(expected - actual) > epsilon) {
                        return false;
                    }
                } else {
                    String expectedToken = answerScanner.next();
                    String actualToken = userScanner.next();
                    if (!expectedToken.equals(actualToken)) {
                        return false;
                    }
                }
            }
            return !userScanner.hasNext();
        } catch (Exception e) {
            System.err.println("[=] Error : " + e.getMessage());
            return false;
        }
    }
}
