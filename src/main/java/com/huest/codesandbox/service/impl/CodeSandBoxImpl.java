// -*- coding = utf-8 -*-
// @Time : 2025/2/7
// @Author : 1zqqx
// @File : CodeSandBoxImpl
// @Software : IntelliJ IDEA

package com.huest.codesandbox.service.impl;

import com.huest.codesandbox.common.JudgeModeEnum;
import com.huest.codesandbox.common.JudgeResultEnum;
import com.huest.codesandbox.common.LanguageEnum;
import com.huest.codesandbox.model.ExecuteCodeRequest;
import com.huest.codesandbox.model.ExecuteCodeResponse;
import com.huest.codesandbox.service.CodeSandBox;
import com.huest.codesandbox.service.template.CppCodeSandBox;
import com.huest.codesandbox.service.template.CppCodeSandBoxSPJ;
import com.huest.codesandbox.service.template.CppCodeSandBoxInteractive;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class CodeSandBoxImpl implements CodeSandBox {
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        LanguageEnum language = executeCodeRequest.getLanguage();
        JudgeModeEnum judgeModeEnum = executeCodeRequest.getJudgeMode();

        if (Objects.isNull(judgeModeEnum) || Objects.isNull(language)) {
            ExecuteCodeResponse response = new ExecuteCodeResponse();
            response.setJudgeResultEnum(JudgeResultEnum.RUNTIME_ERROR);
            return response;
        }
        switch (judgeModeEnum) {
            case COM:
                do {
                    switch (language) {
                        case C:
                        case JAVA:
                        case PYTHON3:
                            return new ExecuteCodeResponse();
                        case CPP:
                            return new CppCodeSandBox().executeCode(executeCodeRequest);
                        default:
                            throw new IllegalStateException("IllegalStateException " + language);
                    }
                } while (false);
            case INTER:
                do {
                    switch (language) {
                        case C:
                        case JAVA:
                        case PYTHON3:
                            return new ExecuteCodeResponse();
                        case CPP:
                            return new CppCodeSandBoxInteractive().executeCode(executeCodeRequest);
                        default:
                            throw new IllegalStateException("IllegalStateException " + language);
                    }
                } while (false);
            case SPJ:
                do {
                    // SPJ
                    switch (language) {
                        case C:
                        case JAVA:
                        case PYTHON3:
                            return new ExecuteCodeResponse();
                        case CPP:
                            return new CppCodeSandBoxSPJ().executeCode(executeCodeRequest);
                        default:
                            throw new IllegalStateException("IllegalStateException " + language);
                    }
                } while (false);
            default:
                throw new IllegalStateException("IllegalStateException " + judgeModeEnum);
        }
    }
}
