// -*- coding = utf-8 -*-
// @Time : 2025/2/27
// @Author : 1zqqx
// @File : TestDataHandler
// @Software : IntelliJ IDEA

package com.huest.codesandbox.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
public class TestDataHandler {

    @Resource
    private MinioUtil minioUtil;

    /**
     * 准备测试数据
     */
    public Path prepareTestData(Path workPath, String problemId) throws Exception {
        /*
          创建题目工作目录
          /tmp/judge/judge_1_1001_10086/1001_data
         */
        Path problemDataDir = Paths.get(String.valueOf(workPath), problemId + "_data");
        Files.createDirectories(problemDataDir);

        // 下载测试数据
        minioUtil.saveIOFileFromMinio(problemId, String.valueOf(problemDataDir));

        return problemDataDir;
    }
}
