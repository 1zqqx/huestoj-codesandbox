// -*- coding = utf-8 -*-
// @Time : 2025/1/30
// @Author : 1zqqx
// @File : CppCodeSandBox
// @Software : IntelliJ IDEA

package com.huest.codesandbox.service.template;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.huest.codesandbox.common.JudgeResultEnum;
import com.huest.codesandbox.model.JudgeCaseInfo;
import com.huest.codesandbox.model.TimeMetrics;
import com.huest.codesandbox.utils.FileUtilBox;
import com.huest.codesandbox.utils.TimeUtil;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class CppCodeSandBox extends CodeSandBoxTemplate {
    public CppCodeSandBox() {
        super();
    }

    // frolvlad/alpine-gxx:latest
    private final String imageName = "8c31dd5dfa94";

    /**
     * 文档
     * <p>
     * <a href="https://javadoc.io/static/com.github.docker-java/docker-java-api/3.2.8/com/github/dockerjava/api/model/HostConfig.html">...</a>
     * <p>
     * time 命令输出的时间统计精度基本在 10 毫秒级
     */
    @Override
    public void execDockerContainer() {
        HostConfig config = new HostConfig()
                .withBinds(new Bind(userCodeParentPath, new Volume("/app")))
                .withMemory((long) (thisLimitRequest.getMemoryLimit() * 1024 * 1024 * 1.1))    // 内存限制 MB * 1024 * 1024 * 1.1
                .withCpuCount(1L)
                .withCpuPeriod(10000L)                                  // CPU 时间片周期 10ms
                .withCpuQuota(thisLimitRequest.getTimeLimit() * 1000L)   // CPU 时间限制 微秒
                .withMemorySwap(0L);                                     // 禁用 Swap

        try {
            // 1. 创建并启动容器
            CreateContainerResponse container =
                    dockerClient.createContainerCmd(imageName)
                            .withHostConfig(config)
                            .withTty(true)
                            .withAttachStdin(true)
                            .withAttachStdout(true)
                            .withAttachStderr(true)
                            .exec();

            containerId = container.getId();
            dockerClient.startContainerCmd(containerId).exec();

            // 2. 将本地文件复制到容器中
            // 卷映射 无需 cp

            String compileCmd = String.format("g++ -std=c++11 -O2 -o /app/program /app/%s",
                    new File(userCodeParentPath + File.separator + GLOBAL_CPP_CLASS_NAME).getName());
            executeCommand(containerId, compileCmd.split(" "));

            // 4. 遍历所有 .in 文件并执行
            File[] inputFiles = Objects.requireNonNull(
                    new File(samplePath).listFiles(
                            file -> file.getName().endsWith(POSTFIX_IN))
            );

            // 内存监控
            statsCallback = new StatsCallback();
            dockerClient.statsCmd(containerId).exec(statsCallback);
            // 程序执行 异步 + 超时、内存 限制
            for (File inputFile : inputFiles) {
                String inputName = inputFile.getName();
                String outputName = inputName.replace(POSTFIX_IN, POSTFIX_OUTPUT);    // tmpcode/uuid/sample/*.output
                String timeLogName = inputName.replace(POSTFIX_IN, POSTFIX_TIME_LOG); // tmpcode/uuid/sample/*.time.log

                JudgeCaseInfo judgeCaseInfo = new JudgeCaseInfo();
                judgeCaseInfo.setCaseExecName(inputName);

                // 执行程序并重定向输入输出
                String runCmd = String.format(
                        "/usr/bin/time -v -o /app/sample/%s /app/program < /app/sample/%s > /app/sample/%s",
                        timeLogName, inputName, outputName // 1.in 1.output
                );

                // 提交任务到线程池 设置超时
                Future<?> future = executor.submit(() ->
                        executeCommand(containerId, "sh", "-c", runCmd));

                try {
                    future.get(thisLimitRequest.getTimeLimit(), TimeUnit.MILLISECONDS); // ms
                } catch (TimeoutException e) {
                    future.cancel(true); // 终止命令执行
                    // set
                    judgeCaseInfo.setCaseExecTime(thisLimitRequest.getTimeLimit());
                    judgeCaseInfo.setCaseExecEnum(JudgeResultEnum.TIME_LIMIT_EXCEEDED);
                    System.err.println("[=] Error in running sample " + inputName + e.getMessage());
                } catch (Exception e) {
                    judgeCaseInfo.setCaseExecEnum(JudgeResultEnum.RUNTIME_ERROR);
                    judgeCaseInfo.setCaseExecInfo(e.getMessage());
                    System.err.println("[=] Error in running sample " + inputName + e.getMessage());
                    continue;
                }

                // todo set time ms 从 *.time.log 文件 解析 时间数据
                TimeMetrics timeMetrics = TimeUtil.parseTimeLog(Path.of(samplePath + File.separator + timeLogName));
                judgeCaseInfo.setCaseExecTime(timeMetrics.getWallTimeMillis());

                // set memory KB
                //long caseMemCs = Math.max(statsCallback.getMaxMemoryUsage(), timeMetrics.getMaxMemoryKB());
                long caseMemCs = statsCallback.getMaxMemoryUsage();
                if (caseMemCs > thisLimitRequest.getMemoryLimit() * 1024 * 1024) // B > MB * 1024 * 1024
                {
                    judgeCaseInfo.setCaseExecMemory(caseMemCs / 1024); // KB
                    if (judgeCaseInfo.getCaseExecEnum() == null)
                        judgeCaseInfo.setCaseExecEnum(JudgeResultEnum.MEMORY_LIMIT_EXCEEDED);
                } else {
                    judgeCaseInfo.setCaseExecMemory(caseMemCs / 1024); // KB
                }

                // set ac
                // ms < ms
                // KB < MB * 1024
                if (judgeCaseInfo.getCaseExecTime() < thisLimitRequest.getTimeLimit() &&
                        judgeCaseInfo.getCaseExecMemory() < thisLimitRequest.getMemoryLimit() * 1024) {
                    boolean compareStrict = FileUtilBox.compareStrict(
                            new File(samplePath + File.separator + inputName.replace(POSTFIX_IN, POSTFIX_OUTPUT)),
                            new File(samplePath + File.separator + inputName.replace(POSTFIX_IN, POSTFIX_OUT))
                    );
                    if (compareStrict) {
                        judgeCaseInfo.setCaseExecEnum(JudgeResultEnum.ACCEPTED);
                    } else {
                        judgeCaseInfo.setCaseExecEnum(JudgeResultEnum.WRONG_ANSWER);
                    }
                }

                execCaseInfo.add(judgeCaseInfo);
                executeCodeResponse.setJudgeCaseInfos(execCaseInfo);
                System.out.println("[=] INFO in CppCodeSandBox : " + judgeCaseInfo);
            }
        } catch (Exception e) {
            System.err.println("[=] Error execDockerContainer : " + e.getMessage());
        }
    }

}
