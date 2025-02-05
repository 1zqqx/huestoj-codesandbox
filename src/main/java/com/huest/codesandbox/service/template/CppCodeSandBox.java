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
import com.huest.codesandbox.utils.FileUtilBox;
import org.springframework.stereotype.Component;

import java.io.File;
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
     */
    @Override
    public void execDockerContainer() {
        HostConfig config = new HostConfig()
                .withBinds(new Bind(userCodeParentPath, new Volume("/app")))
                .withMemory((long) (thisLimitRequest.getMemoryLimit() * 1024 * 1024 * 1.1))    // 内存限制 MB * 1024 * 1024 * 1.1
                .withCpuCount(1L)
                .withCpuPeriod(100000L)                                  // CPU 时间片周期 100ms
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
                    new File(userCodeParentPath + File.separator + "main.cc").getName());
            executeCommand(containerId, compileCmd.split(" "));

            // 4. 遍历所有 .in 文件并执行
            File[] inputFiles = Objects.requireNonNull(
                    new File(samplePath).listFiles(
                            file -> file.getName().endsWith(".in"))
            );

            // 内存监控
            statsCallback = new StatsCallback();
            dockerClient.statsCmd(containerId).exec(statsCallback);
            // 程序执行 异步 + 超时、内存 限制
            for (File inputFile : inputFiles) {
                String inputName = inputFile.getName();
                String outputName = inputName.replace(".in", ".output");
                JudgeCaseInfo judgeCaseInfo = new JudgeCaseInfo();
                judgeCaseInfo.setCaseExecName(inputName);

                // 执行程序并重定向输入输出
                String runCmd = String.format(
                        "/app/program < /app/sample/%s > /app/sample/%s",
                        inputName, outputName // 1.in 1.output
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

                // todo set time ms
                long caseTimCs = 36;
                judgeCaseInfo.setCaseExecTime(caseTimCs);

                // set memory KB
                long caseMemCs = statsCallback.getMaxMemoryUsage();
                //judgeCaseInfo.setCaseExecMemory(statsCallback.getMaxMemoryUsage() / 1024); // KB
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
                    String nameStr = inputFile.getName();
                    File upAnswer = new File(
                            samplePath + File.separator +
                                    nameStr.replace(".in", ".output")
                    );
                    File acAnswer = new File(
                            samplePath + File.separator +
                                    nameStr.replace(".in", ".out")
                    );
                    boolean compareStrict = FileUtilBox.compareStrict(acAnswer, upAnswer);
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
