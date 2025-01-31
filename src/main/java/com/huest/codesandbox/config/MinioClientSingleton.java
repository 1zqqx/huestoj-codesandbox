// -*- coding = utf-8 -*-
// @Time : 2025/1/30
// @Author : 1zqqx
// @File : MinioConfig
// @Software : IntelliJ IDEA

package com.huest.codesandbox.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class MinioClientSingleton {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.accessKey}")
    private String accessKey;

    @Value("${minio.secretKey}")
    private String secretKey;

    /**
     * @Value 注解无法注入静态字段：@Value 注解依赖于 Spring 的依赖注入机制，而静态字段是类级别的，
     * 不依赖于对象实例。Spring 的依赖注入是基于对象实例进行的，因此无法直接将配置值注入到静态字段中。
     * <p>
     * 静态代码块执行时机：静态代码块在类加载时执行，此时 Spring 的依赖注入还未完成，所以 endpoint、
     * accessKey 和 secretKey 字段的值为 null，在使用这些 null 值创建 MinioClient 实例时会抛出异常。
     */

    // 静态常量，在类加载时就创建实例
    private static MinioClient INSTANCE;

    @PostConstruct
    public void init() {
        try {
            // 初始化 MinioClient 实例
            INSTANCE = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();
        } catch (Exception e) {
            // todo
            System.out.println("[=] Error MinioClient : " + e.getMessage());
        }
    }

    // 私有构造函数，防止外部实例化
    private MinioClientSingleton() {
    }

    // 公共静态方法，用于获取单例实例
    public static MinioClient getInstance() {
        return INSTANCE;
    }
}
