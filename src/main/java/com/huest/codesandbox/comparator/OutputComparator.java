// -*- coding = utf-8 -*-
// @Time : 2025/2/27
// @Author : 1zqqx
// @File : OutputComparator
// @Software : IntelliJ IDEA

package com.huest.codesandbox.comparator;

import com.huest.codesandbox.common.JudgeStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
public class OutputComparator {
    private static final long MAX_OUTPUT_SIZE = 100 * 1024 * 1024; // 100MB

    /**
     * 比较用户输出和标准输出
     *
     * @param userOutput     用户输出文件路径
     * @param standardOutput 标准输出文件路径
     * @return 评测状态
     */
    public JudgeStatusEnum compare(Path userOutput, Path standardOutput) {
        try {
            // 检查输出文件大小
            if (Files.size(userOutput) > MAX_OUTPUT_SIZE) {
                return JudgeStatusEnum.OUTPUT_LIMIT_EXCEEDED;
            }

            try (BufferedReader userReader = new BufferedReader(new FileReader(userOutput.toFile()));
                 BufferedReader stdReader = new BufferedReader(new FileReader(standardOutput.toFile()))) {

                String userLine;
                String stdLine;
                boolean presentationError = false;

                while ((stdLine = stdReader.readLine()) != null) {
                    userLine = userReader.readLine();

                    if (userLine == null) {
                        return JudgeStatusEnum.WRONG_ANSWER;
                    }

                    // 去除行尾空白字符
                    String trimmedUserLine = userLine.stripTrailing();
                    String trimmedStdLine = stdLine.stripTrailing();

                    if (!trimmedUserLine.equals(trimmedStdLine)) {
                        // 检查是否只是空格或换行符的差异
                        if (trimmedUserLine.replaceAll("\\s+", " ")
                                .equals(trimmedStdLine.replaceAll("\\s+", " "))) {
                            presentationError = true;
                        } else {
                            return JudgeStatusEnum.WRONG_ANSWER;
                        }
                    }
                }

                // 检查用户输出是否还有多余的行
                if (userReader.readLine() != null) {
                    return JudgeStatusEnum.WRONG_ANSWER;
                }

                return presentationError ? JudgeStatusEnum.PRESENTATION_ERROR : JudgeStatusEnum.ACCEPTED;

            } catch (IOException e) {
                log.error("Error reading output files", e);
                return JudgeStatusEnum.SYSTEM_ERROR;
            }
        } catch (IOException e) {
            log.error("Error checking output file size", e);
            return JudgeStatusEnum.SYSTEM_ERROR;
        }
    }
}