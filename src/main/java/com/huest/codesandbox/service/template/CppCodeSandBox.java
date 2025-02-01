// -*- coding = utf-8 -*-
// @Time : 2025/1/30
// @Author : 1zqqx
// @File : CppCodeSandBox
// @Software : IntelliJ IDEA

package com.huest.codesandbox.service.template;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component
public class CppCodeSandBox extends CodeSandBoxTemplate {
    public CppCodeSandBox() {
        super();
    }

    // frolvlad/alpine-gxx:latest
    private final String imageName = "8c31dd5dfa94";

    private DockerClient dockerClient;

    @Override
    public void execDockerContainer() {
        // 连接到本地 Docker 守护进程
        dockerClient = DockerClientBuilder.getInstance().build();
        HostConfig config = new HostConfig()
                .withBinds(new Bind(userCodeParentPath, new Volume("/app")));

        String containerId;

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

            for (File inputFile : inputFiles) {
                String inputName = inputFile.getName();
                String outputName = inputName.replace(".in", ".output");

                // 执行程序并重定向输入输出
                String runCmd = String.format(
                        "/app/program < /app/sample/%s > /app/sample/%s",
                        inputName, outputName // 1.in 1.output
                );
                executeCommand(containerId, "sh", "-c", runCmd);
                // todo 每次执行一组样例时 统计时间 内存 消耗
            }

            // 5. 将输出文件复制回宿主机
            // 输出文件直接映射到本机 无需从容器内 cp

            dockerClient.stopContainerCmd(containerId).exec();
            dockerClient.close();
        } catch (Exception e) {
            System.err.println("[=] Error execDockerContainer : " + e.getMessage());
        }
    }

    @Override
    public void collectOutputResults() {
        super.collectOutputResults();
    }

    /**
     * 在容器内执行命令
     */
    private void executeCommand(String containerId, String... cmd) {
        ExecCreateCmdResponse exec = dockerClient.execCreateCmd(containerId)
                .withCmd(cmd)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .exec();

        try {
            dockerClient.execStartCmd(exec.getId())
                    .exec(new ExecStartResultCallback(System.out, System.err))
                    .awaitCompletion();
        } catch (InterruptedException e) {
            System.err.println("[=] ERROR executeCommand : " + e.getMessage());
        }
    }
}
