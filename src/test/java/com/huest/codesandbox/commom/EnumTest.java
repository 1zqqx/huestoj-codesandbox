// -*- coding = utf-8 -*-
// @Time : 2025/2/3
// @Author : 1zqqx
// @File : EnumTest
// @Software : IntelliJ IDEA

package com.huest.codesandbox.commom;

import com.huest.codesandbox.common.JudgeStatusEnum;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

@SpringBootTest
public class EnumTest {

    @Test
    void test01() {
        JudgeStatusEnum wa = JudgeStatusEnum.getEnumByValue("WA");
        System.out.println(wa);

        String text = JudgeStatusEnum.OUTPUT_LIMIT_EXCEEDED.getText();
        String value = JudgeStatusEnum.WRONG_ANSWER.getValue();
        System.out.println(text + " " + value);

        JudgeStatusEnum[] values = JudgeStatusEnum.values();
        System.out.println(Arrays.toString(values));
    }
}
