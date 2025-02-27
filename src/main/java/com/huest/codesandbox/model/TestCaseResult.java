// -*- coding = utf-8 -*-
// @Time : 2025/2/27
// @Author : 1zqqx
// @File : TestCaseResult
// @Software : IntelliJ IDEA

package com.huest.codesandbox.model;

import com.huest.codesandbox.common.JudgeStatusEnum;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TestCaseResult {
    private int testCaseId;          // 测试点编号
    private JudgeStatusEnum status;      // 该测试点的状态
    private int time;                // 运行时间(ms)
    private int memory;              // 内存使用(KB)
    private String errorMessage;     // 错误信息
}
