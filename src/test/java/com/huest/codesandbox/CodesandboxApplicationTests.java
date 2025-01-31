package com.huest.codesandbox;

import com.huest.codesandbox.common.LanguageEnum;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class CodesandboxApplicationTests {

    @Test
    void test01() {
        LanguageEnum cpp = LanguageEnum.CPP;

        System.out.println(cpp.getText());
        System.out.println(cpp.getValue());

        List<String> values = LanguageEnum.getValues();
        for (String v : values) {
            System.out.println(v);
        }
    }

}
