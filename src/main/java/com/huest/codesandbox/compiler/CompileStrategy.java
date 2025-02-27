// -*- coding = utf-8 -*-
// @Time : 2025/2/27
// @Author : 1zqqx
// @File : CompileStrategy
// @Software : IntelliJ IDEA

package com.huest.codesandbox.compiler;

public interface CompileStrategy {
    /**
     * 生成编译命令
     * @param sourceFile 源文件路径
     * @param targetFile 目标文件路径
     * @return 编译命令
     */
    String getCompileCommand(String sourceFile, String targetFile);
}
