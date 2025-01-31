// -*- coding = utf-8 -*-
// @Time : 2025/1/30
// @Author : 1zqqx
// @File : MinioTest
// @Software : IntelliJ IDEA

package com.huest.codesandbox.minio;

import com.huest.codesandbox.common.LanguageEnum;
import com.huest.codesandbox.config.MinioClientSingleton;
import com.huest.codesandbox.model.ExecuteCodeRequest;
import com.huest.codesandbox.service.CodeSandBox;
import com.huest.codesandbox.service.template.CodeSandBoxTemplate;
import com.huest.codesandbox.service.template.CppCodeSandBox;
import com.huest.codesandbox.utils.TimeUtil;
import com.huest.codesandbox.utils.minio.MinioUtil;
import io.minio.MinioClient;
import io.minio.messages.Bucket;
import org.bouncycastle.jcajce.provider.asymmetric.NTRU;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

@SpringBootTest
public class MinioTest {
    @Autowired
    private MinioUtil minioUtil;

    @Test
    void test01() {
        try {
            List<Bucket> buckets = MinioClientSingleton.getInstance().listBuckets();
            for (Bucket bucket : buckets) {
                System.out.println(bucket.name() + " " +
                        TimeUtil.parseDate(bucket.creationDate(), "yy-MM-dd"));
            }
        } catch (Exception e) {
            System.out.println("Error : " + e.getMessage());
        }
    }


    @Test
    void test02() {
        //int i = minioUtil.saveCodeFileFromMinio("L1-096.cpp",
        //        "/home/liuqiqi/huestoj/huestoj-codesandbox/tmpcode/L1-096.cpp");

        int i = minioUtil.saveIOFileFromMinio("1096", "");
        System.out.println(i);
    }

    @Test
    void test03() {
        CppCodeSandBox cppCodeSandBox = new CppCodeSandBox();
        cppCodeSandBox.saveCode2File("L1-096.cpp");
        cppCodeSandBox.saveStandardIOData2File("1096");

        CppCodeSandBox cppCodeSandBox1 = new CppCodeSandBox();
        cppCodeSandBox1.saveCode2File("L1-096.cpp");
        cppCodeSandBox1.saveStandardIOData2File("1096");
    }

    @Test
    void test04() {
        CppCodeSandBox cppCodeSandBox = new CppCodeSandBox();

        ExecuteCodeRequest req = new ExecuteCodeRequest();
        req.setLanguage(LanguageEnum.CPP.getValue());
        req.setSourceCodeID("L1-096.cpp");
        req.setOnlySample(true);
        req.setUserInputSample(Arrays.asList("1 2", "2 3", "3 4"));

        cppCodeSandBox.executeCode(req);
    }
}