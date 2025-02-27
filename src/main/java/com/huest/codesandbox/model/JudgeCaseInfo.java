// -*- coding = utf-8 -*-
// @Time : 2025/2/3
// @Author : 1zqqx
// @File : JudgeCaseInfo
// @Software : IntelliJ IDEA

package com.huest.codesandbox.model;

import com.huest.codesandbox.common.JudgeStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 每个 样例 执行
 * 时间 Long ms
 * 内存 Long KB
 * 状态 judgeResultEnum
 * 样例编号 String
 * 运行信息 String
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class JudgeCaseInfo {

    private String caseExecName;

    private JudgeStatusEnum caseExecEnum;

    private Long caseExecTime;

    private Long caseExecMemory;

    private String caseExecInfo;
}
