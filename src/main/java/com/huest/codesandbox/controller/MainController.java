// -*- coding = utf-8 -*-
// @Time : 2025/1/24
// @Author : 1zqqx
// @File : MainController
// @Software : IntelliJ IDEA

package com.huest.codesandbox.controller;

import com.huest.codesandbox.common.JudgeModeEnum;
import com.huest.codesandbox.common.LanguageEnum;
import com.huest.codesandbox.model.ExecuteCodeRequest;
import com.huest.codesandbox.model.ExecuteCodeResponse;
import com.huest.codesandbox.model.JudgeLimitInfo;
import com.huest.codesandbox.model.JudgeResult;
import com.huest.codesandbox.service.JudgeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/")
public class MainController {

    // 定义鉴权请求头和密钥 内网 API 调用
    private static final String AUTH_REQUEST_HEADER = "HUESTOJ";
    private static final String AUTH_REQUEST_SECRET = "huest_1955";

    @Resource
    private JudgeService judgeService;

    @GetMapping("/ok")
    public String healthCheck() {
        return "ok";
    }

    @PostMapping("/exec")
    public JudgeResult executeCode(
            @RequestBody ExecuteCodeRequest executeRequest,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse
    ) {
        log.info("executeRequest info : {}", executeRequest);

        String authHeader = httpServletRequest.getHeader(AUTH_REQUEST_HEADER);
        if (!AUTH_REQUEST_SECRET.equals(authHeader)) {
            httpServletResponse.setStatus(403);
        }

        if (executeRequest == null) {
            throw new RuntimeException("[=] ERROR Parameter of request is empty.");
        }

        LanguageEnum language = executeRequest.getLanguage();
        JudgeModeEnum judgeMode = executeRequest.getJudgeMode();
        String sourceCodeID = executeRequest.getSourceCode();
        JudgeLimitInfo judgeLimitInfo = executeRequest.getJudgeLimitInfo();
        String queDataID = executeRequest.getQueDataID();
        String userID = executeRequest.getUserID();
        String queID = executeRequest.getQueID();
        String judgeID = executeRequest.getJudgeID();
        if (
                Objects.isNull(language) ||
                        Objects.isNull(judgeMode) ||
                        Objects.isNull(sourceCodeID) ||
                        Objects.isNull(judgeLimitInfo) ||
                        Objects.isNull(queDataID) ||
                        Objects.isNull(userID) ||
                        Objects.isNull(queID) ||
                        Objects.isNull(judgeID)
        ) {
            httpServletResponse.setStatus(400);
            JudgeResult re = new JudgeResult();
            re.setErrorMessage("error");
            return re;
        }

        log.info("Received judge request for problem {}, user {}",
                executeRequest.getQueID(), executeRequest.getUserID());
        JudgeResult judgeResult = judgeService.judge(executeRequest);
        log.info("Judge completed with status: {}", judgeResult);

        return judgeResult;
    }

}