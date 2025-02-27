// -*- coding = utf-8 -*-
// @Time : 2025/1/30
// @Author : 1zqqx
// @File : CppCodeSandBox
// @Software : IntelliJ IDEA

package com.huest.codesandbox.service.template;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.huest.codesandbox.common.JudgeResultEnum;
import com.huest.codesandbox.model.JudgeCaseInfo;
import com.huest.codesandbox.utils.FileUtilBox;
import com.huest.codesandbox.utils.TimeUtil;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class CppCodeSandBoxSPJ extends CodeSandBoxTemplate {
    public CppCodeSandBoxSPJ() {
        super();
    }

    // alpine-gxx:latest
    private final String imageName = "5be96961e83d";

    @Override
    public void execDockerContainer() {
        HostConfig config = new HostConfig()
                .withBinds(new Bind(userCodeParentPath, new Volume("/app")))
                .withMemory(thisLimitRequest.getMemoryLimit() * 1024 * 1024) // 内存限制(单位 B) MB * 1024 * 1024
                // withUlimits set max container run time include compile time
                //.withUlimits(new Ulimit[]{
                //        //  单位 s
                //        new Ulimit("cpu", thisLimitRequest.getTimeLimit(), thisLimitRequest.getTimeLimit())
                //})
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

            // Also compile SPJ program if it exists
            String spjCompileCmd = String.format("g++ -std=c++11 -O2 -o /app/spj /app/spj.cpp");

            final boolean[] isCompileFail = {false};
            // Compile user program
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

            // Compile SPJ program if it exists
            if (!isCompileFail[0] && new File(userCodeParentPath + File.separator + "spj.cpp").exists()) {
                executeCommand(
                        containerId,
                        new ExecStartResultCallback() {
                            @Override
                            public void onNext(Frame frame) {
                                StreamType streamType = frame.getStreamType();
                                String re = String.valueOf(frame);
                                if (StreamType.STDERR.equals(streamType) && re.contains("error")) {
                                    isCompileFail[0] = true;
                                    executeCodeResponse.setJudgeResultEnum(JudgeResultEnum.INTERNAL_ERROR);
                                    executeCodeResponse.setJudgeCompileInfo("Special judge compilation error: " + re);
                                }
                                System.out.println("{} INFO in SPJ compile : " + frame);
                                super.onNext(frame);
                            }
                        },
                        spjCompileCmd.split(" ")
                );
            }

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
                        "date +%%s%%N >> /app/sample/%s && timeout %ds /app/program < /app/sample/%s > /app/sample/%s && date +%%s%%N >> /app/sample/%s",
                        timeLogName, thisLimitRequest.getTimeLimit(), inputName, outputName, timeLogName // *.time.log *.in *.output *.time.log
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
                    CompletableFuture.supplyAsync(
                            () -> dockerClient.waitContainerCmd(containerId).exec(
                                    new WaitContainerResultCallback() {
                                        @Override
                                        public void onNext(WaitResponse waitResponse) {
                                            System.out.println("[=] INFO waitResponse : " + waitResponse.toString());
                                            super.onNext(waitResponse);
                                        }
                                    }
                            )
                    ).get(thisLimitRequest.getTimeLimit(), TimeUnit.SECONDS);
                    future.get(thisLimitRequest.getTimeLimit(), TimeUnit.SECONDS); // s
                } catch (TimeoutException e) {
                    future.cancel(true); // 终止命令执行
                    dockerClient.killContainerCmd(containerId).exec(); // kill container
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
                    // set memory MB
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

                    // set time ms 从 *.time.log 文件 解析 时间数据
                    Long caseExecTime = TimeUtil.calcSN2Nanosecond(
                            Path.of(samplePath + File.separator + timeLogName)
                    );
                    judgeCaseInfo.setCaseExecTime(caseExecTime / 1000000 + 1);
                    // set ac
                    // ms < ms
                    // KB < MB * 1024
                    // Run SPJ to check the answer if time and memory are within limits
                    if (judgeCaseInfo.getCaseExecTime() < thisLimitRequest.getTimeLimit() * 1000 &&
                            judgeCaseInfo.getCaseExecMemory() < thisLimitRequest.getMemoryLimit() * 1024) {
                        
                        if (new File(userCodeParentPath + File.separator + "spj").exists()) {
                            // Run special judge program
                            String spjCmd = String.format("/app/spj /app/sample/%s /app/sample/%s /app/sample/%s",
                                    inputName,
                                    outputName,
                                    inputName.replace(POSTFIX_IN, POSTFIX_OUT));

                            final boolean[] spjResult = {false};
                            executeCommand(
                                    containerId,
                                    new ExecStartResultCallback() {
                                        @Override
                                        public void onNext(Frame frame) {
                                            String result = new String(frame.getPayload(), StandardCharsets.UTF_8).trim();
                                            if ("1".equals(result)) {
                                                spjResult[0] = true;
                                            }
                                            super.onNext(frame);
                                        }
                                    },
                                    "sh", "-c", spjCmd
                            );

                            if (spjResult[0]) {
                                judgeCaseInfo.setCaseExecEnum(JudgeResultEnum.ACCEPTED);
                            } else {
                                judgeCaseInfo.setCaseExecEnum(JudgeResultEnum.WRONG_ANSWER);
                            }
                        } else {
                            // Fallback to strict comparison if SPJ doesn't exist
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
