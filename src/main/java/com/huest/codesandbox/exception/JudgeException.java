// -*- coding = utf-8 -*-
// @Time : 2025/2/27
// @Author : 1zqqx
// @File : JudgeException
// @Software : IntelliJ IDEA

package com.huest.codesandbox.exception;

import com.huest.codesandbox.common.JudgeStatusEnum;
import lombok.Getter;

@Getter
public class JudgeException extends RuntimeException {
    private final JudgeStatusEnum status;
    private final String detail;

    public JudgeException(JudgeStatusEnum status, String message, String detail) {
        super(message);
        this.status = status;
        this.detail = detail;
    }

    public JudgeException(JudgeStatusEnum status, String message) {
        this(status, message, null);
    }
}
