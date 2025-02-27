// -*- coding = utf-8 -*-
// @Time : 2025/1/24
// @Author : 1zqqx
// @File : ExecuteCodeRequest
// @Software : IntelliJ IDEA

package com.huest.codesandbox.model;

import com.huest.codesandbox.common.JudgeModeEnum;
import com.huest.codesandbox.common.LanguageEnum;
import lombok.Data;

import java.util.List;

@Data
public class ExecuteCodeRequest {

    /**
     * 使用语言
     */
    private LanguageEnum language;

    /**
     * 评测方式 SPJ INTER COM
     */
    private JudgeModeEnum judgeMode;

    /**
     * 源代码
     */
    private String sourceCode;

    /**
     * EveRunId 用与在桶内 根据 前缀 查找该题的输入输出
     * 该前缀与题目的题号绑定
     * bug 注意 变量名
     */
    private String queDataID;

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
