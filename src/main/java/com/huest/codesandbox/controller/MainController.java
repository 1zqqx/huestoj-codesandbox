// -*- coding = utf-8 -*-
// @Time : 2025/1/24
// @Author : 1zqqx
// @File : MainController
// @Software : IntelliJ IDEA

package com.huest.codesandbox.controller;

import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import com.huest.codesandbox.model.ExecuteCodeRequest;
import com.huest.codesandbox.model.ExecuteCodeResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/")
public class MainController {

    // 定义鉴权请求头和密钥 内网 API 调用
    private static final String AUTH_REQUEST_HEADER = "HUESTOJ";
    private static final String AUTH_REQUEST_SECRET = "huest_1955";

    @GetMapping("/ok")
    public String healthCheck() {
        return "ok";
    }

    @PostMapping("/exec")
    public ExecuteCodeResponse executeCode(
            @RequestBody ExecuteCodeRequest executeRequest,
            HttpServerRequest httpServerRequest,
            HttpServerResponse httpServerResponse
    ) {
        String authHeader = httpServerRequest.getHeader(AUTH_REQUEST_HEADER);
        if (AUTH_REQUEST_SECRET.equals(authHeader)) {
            httpServerResponse.send(403);
        }

        if (executeRequest == null) {
            throw new RuntimeException("[=] ERROR Parameter of request is empty.");
        }

        return new ExecuteCodeResponse();
    }

}
