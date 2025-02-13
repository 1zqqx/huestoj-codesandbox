// -*- coding = utf-8 -*-
// @Time : 2025/1/30
// @Author : 1zqqx
// @File : CppCodeSandBox
// @Software : IntelliJ IDEA

package com.huest.codesandbox.service.template;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.huest.codesandbox.common.JudgeResultEnum;
import com.huest.codesandbox.model.JudgeCaseInfo;
import com.huest.codesandbox.utils.FileUtilBox;
import com.huest.codesandbox.utils.TimeUtil;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class CppCodeSandBox extends CodeSandBoxTemplate {
    public CppCodeSandBox() {
        super();
    }

    // alpine-gxx:latest
    private final String imageName = "5be96961e83d";

    /**
     * 文档
     * <p>
     * <a href="https://javadoc.io/static/com.github.docker-java/docker-java-api/3.2.8/com/github/dockerjava/api/model/HostConfig.html">...</a>
     * <p>
     * time 命令输出的时间统计精度基本在 10 毫秒级
     * <p>
     * use date
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

            final boolean[] isCompileFail = {false};
            executeCommand(
                    containerId,
                    new ExecStartResultCallback() {
                        @Override
                        public void onNext(Frame frame) {
                            StreamType streamType = frame.getStreamType();
                            String re = String.valueOf(frame);
                            executeCodeResponse.setJudgeCompileInfo(re);
                            if (StreamType.STDERR.equals(streamType) && re.contains("error")) {
                                isCompileFail[0] = true;
                                executeCodeResponse.setJudgeResultEnum(JudgeResultEnum.COMPILATION_ERROR);
                            }
                            System.out.println("{} INFO in compile : " + frame);
                            super.onNext(frame);
                        }

                        @Override
                        public void onComplete() {
                            super.onComplete();
                        }
                    },
                    compileCmd.split(" ")
            );
            if (isCompileFail[0]) {
                System.err.println("{} ERROR Compile Fail!!!");
                return;
            }

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
                        "date +%%s%%N >> /app/sample/%s && /app/program < /app/sample/%s > /app/sample/%s && date +%%s%%N >> /app/sample/%s",
                        timeLogName, inputName, outputName, timeLogName // *.time.log *.in *.output *.time.log
                );

                final boolean[] isExecFail = {false, false}; // execFail timeout
                // 提交任务到线程池 设置超时
                Future<?> future = executor.submit(() ->
                        executeCommand(
                                containerId,
                                new ExecStartResultCallback() {
                                    @Override
                                    public void onNext(Frame frame) {
                                        String str = new String(frame.getPayload(), StandardCharsets.UTF_8);
                                        if (StreamType.STDERR.equals(frame.getStreamType())) {
                                            isExecFail[0] = true;
                                            judgeCaseInfo.setCaseExecEnum(JudgeResultEnum.RUNTIME_ERROR);
                                            System.err.println("{} ERROR in running " + inputName + " : " + str);
                                        }
                                        judgeCaseInfo.setCaseExecInfo(str);
                                        super.onNext(frame);
                                    }
                                },
                                "sh", "-c", runCmd)
                );

                // todo bug
                try {
                    future.get(thisLimitRequest.getTimeLimit(), TimeUnit.MILLISECONDS); // ms
                } catch (TimeoutException e) {
                    future.cancel(true); // 终止命令执行
                    isExecFail[1] = true;
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

                if (!(isExecFail[0] | isExecFail[1])) {
                    // set time ms 从 *.time.log 文件 解析 时间数据
                    Long caseExecTime = TimeUtil.calcSN2Nanosecond(
                            Path.of(samplePath + File.separator + timeLogName)
                    );
                    judgeCaseInfo.setCaseExecTime(caseExecTime / 1000000 + 1);

                    // set memory KB
                    //long caseMemCs = Math.max(statsCallback.getMaxMemoryUsage(), timeMetrics.getMaxMemoryKB());
                    long caseMemCs = statsCallback.getMaxMemoryUsage();
                    if (caseMemCs > thisLimitRequest.getMemoryLimit() * 1024 * 1024) { // B > MB * 1024 * 1024
                        judgeCaseInfo.setCaseExecMemory(caseMemCs / 1024); // KB
                        if (judgeCaseInfo.getCaseExecEnum() == null) {
                            judgeCaseInfo.setCaseExecEnum(JudgeResultEnum.MEMORY_LIMIT_EXCEEDED);
                        }
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
