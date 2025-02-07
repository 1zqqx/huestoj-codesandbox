// -*- coding = utf-8 -*-
// @Time : 2025/2/7
// @Author : 1zqqx
// @File : JudgeModeEnum
// @Software : IntelliJ IDEA

package com.huest.codesandbox.common;

import lombok.Getter;

@Getter
public enum JudgeModeEnum {
    SPJ("special judge"),
    COM("common judge"),
    INTER("interactively");

    private final String describe;

    JudgeModeEnum(String describe) {
        this.describe = describe;
    }
}