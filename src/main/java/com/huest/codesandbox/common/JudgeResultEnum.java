// -*- coding = utf-8 -*-
// @Time : 2025/2/3
// @Author : 1zqqx
// @File : JudgeResultEnum
// @Software : IntelliJ IDEA

package com.huest.codesandbox.common;

import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public enum JudgeResultEnum {

    // 执行通过
    ACCEPTED("Accepted", "AC"),

    // 错误解答
    WRONG_ANSWER("Wrong answer", "WA"),

    // 超出内存限制
    MEMORY_LIMIT_EXCEEDED("Memory limit exceeded", "MLE"),

    // 超出输出限制
    OUTPUT_LIMIT_EXCEEDED("Output limit exceeded", "OLE"),

    // 超出时间限制
    TIME_LIMIT_EXCEEDED("Time limit exceeded", "TLE"),

    // 执行出错
    RUNTIME_ERROR("Runtime error", "RE"),

    // 内部出错
    INTERNAL_ERROR("Internal error", "IE"),

    // 编译出错
    COMPILATION_ERROR("Compilation error", "CE");

    private final String text;

    private final String value;

    // 构造函数，用于初始化枚举实例的描述信息
    JudgeResultEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 值列表
     *
     * @return List<String>
     */
    public static List<String> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    /**
     * 根据值 获得 枚举
     * @param value val
     * @return re
     */
    public static JudgeResultEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (JudgeResultEnum anEnum : JudgeResultEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}