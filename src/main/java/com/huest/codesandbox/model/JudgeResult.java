// -*- coding = utf-8 -*-
// @Time : 2025/2/27
// @Author : 1zqqx
// @File : JudgeResult
// @Software : IntelliJ IDEA

package com.huest.codesandbox.model;

import com.huest.codesandbox.common.JudgeStatusEnum;
import lombok.Data;

@Data
public class JudgeResult {
    private JudgeStatusEnum status;      // 评测状态
    private Integer time;            // 实际执行时间(ms)
    private Integer memory;          // 实际使用内存(MB)
    private String errorMessage;     // 错误信息
    private Integer passedTestCase;  // 通过的测试点数
    private Integer totalTestCase;   // 总测试点数
}