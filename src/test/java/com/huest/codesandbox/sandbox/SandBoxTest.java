// -*- coding = utf-8 -*-
// @Time : 2025/1/31
// @Author : 1zqqx
// @File : sandBoxTest
// @Software : IntelliJ IDEA

package com.huest.codesandbox.sandbox;

import com.huest.codesandbox.common.LanguageEnum;
import com.huest.codesandbox.model.ExecuteCodeRequest;
import com.huest.codesandbox.model.JudgeLimitInfo;
import com.huest.codesandbox.model.TimeMetrics;
import com.huest.codesandbox.service.template.CppCodeSandBox;
import com.huest.codesandbox.utils.TimeUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Path;

@SpringBootTest
public class SandBoxTest {

    @Autowired
    private CppCodeSandBox cppCodeSandBox;

    @Test
    void test01() {

        ExecuteCodeRequest req = new ExecuteCodeRequest();
        req.setLanguage(LanguageEnum.CPP);
        req.setSourceCode("L1-096.cpp");
        req.setOnlySample(false);
        req.setQueDataID("1096");

        cppCodeSandBox.executeCode(req);
    }

    @Test
    void test02() {

        ExecuteCodeRequest req = new ExecuteCodeRequest();

        req.setLanguage(LanguageEnum.CPP);
        req.setSourceCode("L1-096.cpp");
        req.setOnlySample(false);
        req.setQueDataID("1096");

        req.setJudgeLimitInfo(new JudgeLimitInfo(
                1000L,
                256L,
                64L
        ));

        cppCodeSandBox.executeCode(req);
    }

    @Test
    void test03(){
        String str = "/home/liuqiqi/huestoj/huestoj-codesandbox/tmpcode/8ef7febf-9b6e-451f-8493-b5fcbe5d40aa/sample/1.time.log";
        try {
            TimeMetrics timeMetrics = TimeUtil.parseTimeLog(Path.of(str));
            System.out.println(timeMetrics);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}