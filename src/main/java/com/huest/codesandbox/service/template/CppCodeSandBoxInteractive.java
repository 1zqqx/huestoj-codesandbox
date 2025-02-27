package com.huest.codesandbox.service.template;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.huest.codesandbox.common.JudgeResultEnum;
import com.huest.codesandbox.model.JudgeCaseInfo;
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
public class CppCodeSandBoxInteractive extends CodeSandBoxTemplate {
    public CppCodeSandBoxInteractive() {
        super();
    }

    private final String imageName = "5be96961e83d";

    @Override
    public void execDockerContainer() {
        HostConfig config = new HostConfig()
                .withBinds(new Bind(userCodeParentPath, new Volume("/app")))
                .withMemory(thisLimitRequest.getMemoryLimit() * 1024 * 1024)
                .withCpuCount(1L)
                .withCpuPeriod(10000L)
                .withCpuQuota(thisLimitRequest.getTimeLimit() * 1000L)
                .withMemorySwap(0L);

        try {
            // Create and start container
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

            // Compile both user program and interactive judge program
            String userCompileCmd = String.format("g++ -std=c++11 -O2 -o /app/program /app/%s",
                    new File(userCodeParentPath + File.separator + GLOBAL_CPP_CLASS_NAME).getName());
            String interactiveCompileCmd = "g++ -std=c++11 -O2 -o /app/interactive /app/interactive.cpp";

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
                    },
                    userCompileCmd.split(" ")
            );

            // Compile interactive judge program
            if (!isCompileFail[0] && new File(userCodeParentPath + File.separator + "interactive.cpp").exists()) {
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
                                    executeCodeResponse.setJudgeCompileInfo("Interactive judge compilation error: " + re);
                                }
                                System.out.println("{} INFO in interactive compile : " + frame);
                                super.onNext(frame);
                            }
                        },
                        interactiveCompileCmd.split(" ")
                );
            }

            if (isCompileFail[0]) {
                System.err.println("{} ERROR Compile Fail!!!");
                return;
            }

            // Process each test case
            File[] inputFiles = Objects.requireNonNull(
                    new File(samplePath).listFiles(
                            file -> file.getName().endsWith(POSTFIX_IN))
            );

            // Memory monitoring
            statsCallback = new StatsCallback();
            dockerClient.statsCmd(containerId).exec(statsCallback);

            for (File inputFile : inputFiles) {
                String inputName = inputFile.getName();
                String timeLogName = inputName.replace(POSTFIX_IN, POSTFIX_TIME_LOG);

                JudgeCaseInfo judgeCaseInfo = new JudgeCaseInfo();
                judgeCaseInfo.setCaseExecName(inputName);

                // Create named pipes for communication
                String setupPipesCmd = "mkfifo /app/pipe_to_user /app/pipe_to_judge";
                executeCommand(containerId, new ExecStartResultCallback(), "sh", "-c", setupPipesCmd);

                // Run both programs with pipes connected
                String runCmd = String.format(
                        "date +%%s%%N >> /app/sample/%s && " +
                        "(timeout %ds /app/interactive < /app/pipe_to_judge > /app/pipe_to_user & " +
                        "timeout %ds /app/program < /app/pipe_to_user > /app/pipe_to_judge) && " +
                        "date +%%s%%N >> /app/sample/%s",
                        timeLogName, 
                        thisLimitRequest.getTimeLimit(),
                        thisLimitRequest.getTimeLimit(),
                        timeLogName
                );

                final boolean[] isExecFail = {false, false};
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
                    future.get(thisLimitRequest.getTimeLimit(), TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    future.cancel(true);
                    dockerClient.killContainerCmd(containerId).exec();
                    isExecFail[1] = true;
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
                    // Set memory usage
                    long caseMemCs = statsCallback.getMaxMemoryUsage();
                    if (caseMemCs > thisLimitRequest.getMemoryLimit() * 1024 * 1024) {
                        judgeCaseInfo.setCaseExecMemory(caseMemCs / 1024);
                        if (judgeCaseInfo.getCaseExecEnum() == null) {
                            judgeCaseInfo.setCaseExecEnum(JudgeResultEnum.MEMORY_LIMIT_EXCEEDED);
                        }
                    } else {
                        judgeCaseInfo.setCaseExecMemory(caseMemCs / 1024);
                    }

                    // Set execution time
                    Long caseExecTime = TimeUtil.calcSN2Nanosecond(
                            Path.of(samplePath + File.separator + timeLogName)
                    );
                    judgeCaseInfo.setCaseExecTime(caseExecTime / 1000000 + 1);

                    // Check result from interactive judge
                    if (judgeCaseInfo.getCaseExecTime() < thisLimitRequest.getTimeLimit() * 1000 &&
                            judgeCaseInfo.getCaseExecMemory() < thisLimitRequest.getMemoryLimit() * 1024) {
                        // The interactive judge program should output 1 for AC, 0 for WA
                        String resultCmd = "cat /app/sample/" + inputName.replace(POSTFIX_IN, POSTFIX_OUTPUT);
                        final boolean[] interactiveResult = {false};
                        executeCommand(
                                containerId,
                                new ExecStartResultCallback() {
                                    @Override
                                    public void onNext(Frame frame) {
                                        String result = new String(frame.getPayload(), StandardCharsets.UTF_8).trim();
                                        if ("1".equals(result)) {
                                            interactiveResult[0] = true;
                                        }
                                        super.onNext(frame);
                                    }
                                },
                                "sh", "-c", resultCmd
                        );

                        if (interactiveResult[0]) {
                            judgeCaseInfo.setCaseExecEnum(JudgeResultEnum.ACCEPTED);
                        } else {
                            judgeCaseInfo.setCaseExecEnum(JudgeResultEnum.WRONG_ANSWER);
                        }
                    }
                }

                execCaseInfo.add(judgeCaseInfo);
                executeCodeResponse.setJudgeCaseInfos(execCaseInfo);
                System.out.println("[=] INFO in CppCodeSandBoxInteractive : " + judgeCaseInfo);
            }
        } catch (Exception e) {
            System.err.println("[=] Error execDockerContainer : " + e.getMessage());
        }
    }
} 