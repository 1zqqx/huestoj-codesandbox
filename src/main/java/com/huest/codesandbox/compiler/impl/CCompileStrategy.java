// -*- coding = utf-8 -*-
// @Time : 2025/2/27
// @Author : 1zqqx
// @File : CCompileStrategy
// @Software : IntelliJ IDEA

package com.huest.codesandbox.compiler.impl;

import com.huest.codesandbox.compiler.CompileStrategy;
import org.springframework.stereotype.Component;

@Component
public class CCompileStrategy implements CompileStrategy {
    @Override
    public String getCompileCommand(String sourceFile, String targetFile) {
        return String.format("gcc -O2 -fmax-errors=3 %s -o %s -lm", sourceFile, targetFile);
    }
}
