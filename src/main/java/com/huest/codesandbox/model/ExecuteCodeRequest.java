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
     * question ID 用与在桶内 根据题号查找该题的输入输出
     */
    private Long questionId;

    /**
     * 标准输入列表
     * minio List<inputId>
     * 传过来的 inputId/outputId 格式 : 题目id/uuid.in.out
     * ×
     * minio 可以根据 question 前缀 查询文件
     * 标准输入输出列表就不需要了
     */
    //private List<String> standardInputList;

    /**
     * 标准输出列表
     * minio List<outputId>
     */
    //private List<String> standardOutputList;

    /**
     * 题目限制信息对象
     */
    private JudgeLimitInfo judgeLimitInfo;
}
