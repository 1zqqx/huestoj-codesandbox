// -*- coding = utf-8 -*-
// @Time : 2025/1/31
// @Author : 1zqqx
// @File : DockerService
// @Software : IntelliJ IDEA

package com.huest.codesandbox.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import org.jvnet.hk2.annotations.Service;

@Service
public class DockerService {
    private final DockerClient dockerClient;

    public DockerService() {
        // 连接到本地 Docker 守护进程
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        this.dockerClient = DockerClientBuilder.getInstance(config).build();
    }
}
