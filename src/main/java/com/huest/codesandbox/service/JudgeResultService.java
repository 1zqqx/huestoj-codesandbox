// -*- coding = utf-8 -*-
// @Time : 2025/2/27
// @Author : 1zqqx
// @File : JudgeResultService
// @Software : IntelliJ IDEA

package com.huest.codesandbox.service;

import com.huest.codesandbox.common.JudgeStatusEnum;
import com.huest.codesandbox.model.JudgeResult;
import com.huest.codesandbox.model.TestCaseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class JudgeResultService {
    /**
     * 汇总所有测试点的结果
     * @param testCaseResults 所有测试点的结果
     * @return 最终的评测结果
     */
    public JudgeResult summarize(List<TestCaseResult> testCaseResults) {
        JudgeResult result = new JudgeResult();

        // 统计通过的测试点数量
        long passedCount = testCaseResults.stream()
                .filter(r -> r.getStatus() == JudgeStatusEnum.ACCEPTED)
                .count();

        result.setPassedTestCase((int) passedCount);
        result.setTotalTestCase(testCaseResults.size());

        // 获取最大时间和内存使用
        result.setTime(testCaseResults.stream()
                .mapToInt(TestCaseResult::getTime)
                .max()
                .orElse(0));

        result.setMemory(testCaseResults.stream()
                .mapToInt(TestCaseResult::getMemory)
                .max()
                .orElse(0));

        // 确定最终状态
        result.setStatus(determineStatus(testCaseResults));

        // 设置错误信息
        setErrorMessage(result, testCaseResults);

        return result;
    }

    private JudgeStatusEnum determineStatus(List<TestCaseResult> results) {
        // 按优先级检查各种状态
        if (results.stream().anyMatch(r -> r.getStatus() == JudgeStatusEnum.SYSTEM_ERROR)) {
            return JudgeStatusEnum.SYSTEM_ERROR;
        }
        if (results.stream().anyMatch(r -> r.getStatus() == JudgeStatusEnum.RUNTIME_ERROR)) {
            return JudgeStatusEnum.RUNTIME_ERROR;
        }
        if (results.stream().anyMatch(r -> r.getStatus() == JudgeStatusEnum.TIME_LIMIT_EXCEEDED)) {
            return JudgeStatusEnum.TIME_LIMIT_EXCEEDED;
        }
        if (results.stream().anyMatch(r -> r.getStatus() == JudgeStatusEnum.MEMORY_LIMIT_EXCEEDED)) {
            return JudgeStatusEnum.MEMORY_LIMIT_EXCEEDED;
        }
        if (results.stream().anyMatch(r -> r.getStatus() == JudgeStatusEnum.OUTPUT_LIMIT_EXCEEDED)) {
            return JudgeStatusEnum.OUTPUT_LIMIT_EXCEEDED;
        }
        if (results.stream().anyMatch(r -> r.getStatus() == JudgeStatusEnum.WRONG_ANSWER)) {
            return JudgeStatusEnum.WRONG_ANSWER;
        }
        if (results.stream().anyMatch(r -> r.getStatus() == JudgeStatusEnum.PRESENTATION_ERROR)) {
            return JudgeStatusEnum.PRESENTATION_ERROR;
        }
        return JudgeStatusEnum.ACCEPTED;
    }

    private void setErrorMessage(JudgeResult result, List<TestCaseResult> testCaseResults) {
        // 如果不是AC，设置第一个非AC测试点的错误信息
        if (result.getStatus() != JudgeStatusEnum.ACCEPTED) {
            testCaseResults.stream()
                    .filter(r -> r.getStatus() != JudgeStatusEnum.ACCEPTED)
                    .findFirst()
                    .ifPresent(r -> {
                        String message = String.format("Test case #%d: %s",
                                r.getTestCaseId(), r.getErrorMessage());
                        result.setErrorMessage(message);
                    });
        }
    }
}
