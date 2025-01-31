// -*- coding = utf-8 -*-
// @Time : 2025/1/24
// @Author : 1zqqx
// @File : ExecuteCodeRequest
// @Software : IntelliJ IDEA

package com.huest.codesandbox.model;

import lombok.Data;

import java.util.List;

@Data
public class ExecuteCodeRequest {

    /**
     * 使用语言
     */
    private String language;

    /**
     * 源代码
     * minio 桶中的 唯一 文件名称
     */
    private String sourceCodeID;

    /**
     * EveRunId 用与在桶内 根据 前缀 查找该题的输入输出
     * 该前缀与题目的题号绑定
     */
    private String EveRunId;

    /**
     * 题目所给样例 或者 用户输入样例运行
     */
    private boolean isOnlySample;

    private List<String> userInputSample;

    /**
     * 题目限制信息对象
     */
    private JudgeLimitInfo judgeLimitInfo;
}
