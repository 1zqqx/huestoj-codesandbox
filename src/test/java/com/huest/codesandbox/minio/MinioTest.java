// -*- coding = utf-8 -*-
// @Time : 2025/1/30
// @Author : 1zqqx
// @File : MinioTest
// @Software : IntelliJ IDEA

package com.huest.codesandbox.minio;

import com.huest.codesandbox.config.MinioClientSingleton;
import com.huest.codesandbox.utils.MinioUtil;
import com.huest.codesandbox.utils.TimeUtil;
import io.minio.messages.Bucket;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
    }

    @Test
    void test04() {

    }
}