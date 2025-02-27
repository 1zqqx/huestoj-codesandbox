// -*- coding = utf-8 -*-
// @Time : 2025/2/27
// @Author : 1zqqx
// @File : CompileStrategyFactory
// @Software : IntelliJ IDEA

package com.huest.codesandbox.compiler;

import com.huest.codesandbox.common.LanguageEnum;
import com.huest.codesandbox.compiler.impl.CCompileStrategy;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CompileStrategyFactory {
    private final Map<LanguageEnum, CompileStrategy> strategies = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 初始化时注册所有支持的编译策略
        registerStrategy(LanguageEnum.C, new CCompileStrategy());
        //registerStrategy(Language.JAVA, new JavaCompileStrategy());
        //registerStrategy(Language.PYTHON, new PythonCompileStrategy());
        //registerStrategy(Language.CPP, new CppCompileStrategy());
        // 可以继续添加其他语言的支持
    }

    public CompileStrategy getStrategy(LanguageEnum language) {
        CompileStrategy strategy = strategies.get(language);
        if (strategy == null) {
            throw new UnsupportedOperationException("Unsupported language: " + language);
        }
        return strategy;
    }

    public void registerStrategy(LanguageEnum language, CompileStrategy strategy) {
        strategies.put(language, strategy);
    }
}
