// -*- coding = utf-8 -*-
// @Time : 2025/1/30
// @Author : 1zqqx
// @File : CodeSandBox
// @Software : IntelliJ IDEA

package com.huest.codesandbox.service;

import com.huest.codesandbox.model.ExecuteCodeRequest;
import com.huest.codesandbox.model.ExecuteCodeResponse;

/**
 * 沙箱接口
 */
public interface CodeSandBox {

    /**
     * 执行代码
     * @param executeCodeRequest request
     * @return response
     */
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
