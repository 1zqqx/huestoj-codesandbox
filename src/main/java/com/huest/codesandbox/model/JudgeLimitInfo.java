// -*- coding = utf-8 -*-
// @Time : 2025/1/30
// @Author : 1zqqx
// @File : JudgeLimitInfo
// @Software : IntelliJ IDEA

package com.huest.codesandbox.model;

import lombok.Data;

/**
 * 请求中应带有
 * 题目评测限制信息
 */
@Data
public class JudgeLimitInfo {

    /**
     * 时间限制
     * 单位 ms
     */
    private Integer timeLimit;

    /**
     * 内存限制
     * 单位 MB
     */
    private Integer memoryLimit;

    /**
     * 栈限制
     * 单位 KB
     */
    private Integer stackLimit;
}
