// -*- coding = utf-8 -*-
// @Time : 2025/1/31
// @Author : 1zqqx
// @File : sandBoxTest
// @Software : IntelliJ IDEA

package com.huest.codesandbox.sandbox;

import com.huest.codesandbox.common.LanguageEnum;
import com.huest.codesandbox.model.ExecuteCodeRequest;
import com.huest.codesandbox.model.JudgeLimitInfo;
import com.huest.codesandbox.service.template.CppCodeSandBox;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class SandBoxTest {

    @Autowired
    private CppCodeSandBox cppCodeSandBox;

    @Test
    void test01() {

        ExecuteCodeRequest req = new ExecuteCodeRequest();
        req.setLanguage(LanguageEnum.CPP.getValue());
        req.setSourceCodeID("L1-096.cpp");
        req.setOnlySample(false);
        req.setEveIODataId("1096");

        cppCodeSandBox.executeCode(req);
    }

    @Test
    void test02() {

        ExecuteCodeRequest req = new ExecuteCodeRequest();

        req.setLanguage(LanguageEnum.CPP.getValue());
        req.setSourceCodeID("L1-096.cpp");
        req.setOnlySample(false);
        req.setEveIODataId("1096");

        req.setJudgeLimitInfo(new JudgeLimitInfo(
                1000L,
                256L,
                64L
        ));

        cppCodeSandBox.executeCode(req);
    }

}