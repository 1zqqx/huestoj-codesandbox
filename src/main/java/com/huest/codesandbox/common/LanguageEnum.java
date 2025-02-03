// -*- coding = utf-8 -*-
// @Time : 2025/1/30
// @Author : 1zqqx
// @File : LanguageEnum
// @Software : IntelliJ IDEA

package com.huest.codesandbox.common;

import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 编程语言枚举
 */
@Getter
public enum LanguageEnum {
    JAVA("java (jdk 11.0.2)", "java"),
    CPP("c++ (gcc 12.3)", "cpp"),
    C("c (gcc 12.3)", "c"),
    PYTHON3("python3", "python3");

    private final String text;

    private final String value;

    LanguageEnum(String text, String value) {
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
     * 根据 value 获得枚举
     *
     * @param value c cpp java python3
     * @return C JAVA CPP PYTHON3
     */
    public static LanguageEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (LanguageEnum anEnum : LanguageEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

}
