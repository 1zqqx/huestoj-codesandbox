// -*- coding = utf-8 -*-
// @Time : 2025/1/24
// @Author : 1zqqx
// @File : ApiController
// @Software : IntelliJ IDEA

package com.huest.codesandbox.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/")
public class ApiController {

    @GetMapping("/")
    public String ok() {
        return "ok";
    }

}
