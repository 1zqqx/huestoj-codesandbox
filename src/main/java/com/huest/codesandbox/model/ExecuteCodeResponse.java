// -*- coding = utf-8 -*-
// @Time : 2025/1/24
// @Author : 1zqqx
// @File : ExecuteCodeResponse
// @Software : IntelliJ IDEA

package com.huest.codesandbox.model;

import com.huest.codesandbox.common.JudgeStatusEnum;
import lombok.Data;

import java.util.List;

@Data
public class ExecuteCodeResponse {

    /**
     * 该题的执行结果 状态
     */
    private JudgeStatusEnum judgeResultEnum;

    /**
     * 该题的 执行 时间
     * 为所有样例 执行 消耗时间 最长
     */
    private Long judgeExecTime;

    /**
     * 该题的 执行 内存 消耗
     * 为所有样例 执行 内训消耗 最多
     */
    private Long judgeExecMemory;

    /**
     * 编译信息
     */
    private String judgeCompileInfo;

    /**
     * 每个样例 运行 的信息
     */
    private List<JudgeCaseInfo> judgeCaseInfos;
}
