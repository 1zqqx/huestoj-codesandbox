// -*- coding = utf-8 -*-
// @Time : 2025/2/3
// @Author : 1zqqx
// @File : EnumTest
// @Software : IntelliJ IDEA

package com.huest.codesandbox.commom;

import com.huest.codesandbox.common.JudgeResultEnum;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

@SpringBootTest
public class EnumTest {

    @Test
    void test01() {
        JudgeResultEnum wa = JudgeResultEnum.getEnumByValue("WA");
        System.out.println(wa);

        String text = JudgeResultEnum.OUTPUT_LIMIT_EXCEEDED.getText();
        String value = JudgeResultEnum.WRONG_ANSWER.getValue();
        System.out.println(text + " " + value);

        JudgeResultEnum[] values = JudgeResultEnum.values();
        System.out.println(Arrays.toString(values));
    }
}
