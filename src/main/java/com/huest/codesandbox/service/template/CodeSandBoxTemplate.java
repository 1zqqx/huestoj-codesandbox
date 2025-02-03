// -*- coding = utf-8 -*-
// @Time : 2025/1/30
// @Author : 1zqqx
// @File : CodeSandBoxTemplate
// @Software : IntelliJ IDEA

package com.huest.codesandbox.service.template;

import cn.hutool.core.io.FileUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.huest.codesandbox.common.LanguageEnum;
import com.huest.codesandbox.model.ExecuteCodeRequest;
import com.huest.codesandbox.model.ExecuteCodeResponse;
import com.huest.codesandbox.model.JudgeLimitInfo;
import com.huest.codesandbox.service.CodeSandBox;
import com.huest.codesandbox.utils.FileUtilBox;
import com.huest.codesandbox.utils.minio.MinioUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


@Component
@Slf4j
public abstract class CodeSandBoxTemplate implements CodeSandBox {

    private static final String GLOBAL_CODE_DIR_NAME = "tmpcode";
    private static final String GLOBAL_SAMPLE_DIR_NAME = "sample";
    private static final String SAMPLE_IN = ".in";
    private static final String SAMPLE_OUT = ".out";


    protected static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";
    protected static final String GLOBAL_CPP_CLASS_NAME = "main.cc";
    protected static final String GLOBAL_C_CLASS_NAME = "main.c";
    protected static final String GLOBAL_PYTHON3_CLASS_NAME = "main.py";


    // 用户当前目录
    private final String userDir = System.getProperty("user.dir");

    // File.separator , linux / , win \\
    // tmpcode/ dir
    private final String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;

    // tmpcode/uuid
    protected String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();

    // 用户代码临时文件父目录
    // 将整个父目录映射到 docker 容器
    // tmpcode/uuid/sample
    protected final String samplePath = userCodeParentPath + File.separator + GLOBAL_SAMPLE_DIR_NAME;

    /**
     * ExecuteCodeResponse 返回 信息
     */
    protected ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();

    //@Autowired()
    protected MinioUtil minioUtil = new MinioUtil();

    /**
     * dockerClient 单例 dockerClient
     * containerId 执行的容器ID
     */
    protected final static DockerClient dockerClient;
    protected String containerId;

    static {
        dockerClient = DockerClientBuilder.getInstance().build();
        // globalCodePathName only once
        String str = System.getProperty("user.dir") + File.separator + GLOBAL_CODE_DIR_NAME;
        if (!FileUtil.exist(str)) {
            FileUtil.mkdir(str);
        }
        System.out.println("{} INFO static : " + System.currentTimeMillis());
    }

    /**
     * 主流程
     *
     * @param executeCodeRequest request
     * @return response
     */
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        String language = executeCodeRequest.getLanguage();
        String sourceCodeID = executeCodeRequest.getSourceCodeID();
        String DataIOId = executeCodeRequest.getEveIODataId();
        JudgeLimitInfo judgeLimitInfo = executeCodeRequest.getJudgeLimitInfo();
        boolean isOnlySample = executeCodeRequest.isOnlySample();
        List<String> userInputSample = executeCodeRequest.getUserInputSample();

        // 1. 从 minio 中 根据 url 获取到用户提交的源代码文件 存储到本地
        saveCode2File(sourceCodeID, language);

        // 2. 从 minio 中 根据 题目 ID 获取到评测数据 存储到本地
        // 如果仅为运行题目的样例 或者用户自定义样例
        if (isOnlySample) {
            // 把用户输入的样例写入文件
            saveSampleDara2File(userInputSample);
        } else {
            saveStandardIOData2File(DataIOId);
        }

        // 4. 启动容器 编译 执行 代码
        // bash exec.sh
        execDockerContainer();

        // 4.5 关闭容器 删除容器
        closeAndDeleteContainer(containerId);

        // 5. 收集整理输出结果
        collectOutputResults();

        // 6. 删除临时文件
        deleteTmpDir(userCodeParentPath);

        return null;
    }

    /**
     * 1. user code to file
     *
     * @param srcCodeId 桶内唯一id
     */
    public void saveCode2File(String srcCodeId, String language) {
        // 判断 用户临时代码目录 是否存在，不存在则新建
        if (!FileUtil.exist(userCodeParentPath)) {
            FileUtil.mkdir(userCodeParentPath);
        }

        // 用户存放的代码进行隔离
        String userCodePath = userCodeParentPath + File.separator + getGlobalClassFileName(language);
        //System.out.println("[=] INFO userCodeParentPath : " + userCodePath);
        //MinioUtil minioUtil = new MinioUtil();
        int i = minioUtil.saveCodeFileFromMinio(srcCodeId, userCodePath);
        if (i == 1) {
            System.out.println("[=] INFO save file success");
        } else {
            System.err.println("[=] Error saveCode2File save sample file failed");
        }
    }

    private String getGlobalClassFileName(String str) {
        if (StringUtils.isBlank(str)) {
            return null;
        }
        switch (Objects.requireNonNull(LanguageEnum.getEnumByValue(str))) {
            case C:
                return GLOBAL_C_CLASS_NAME;
            case JAVA:
                return GLOBAL_JAVA_CLASS_NAME;
            case CPP:
                return GLOBAL_CPP_CLASS_NAME;
            case PYTHON3:
                return GLOBAL_PYTHON3_CLASS_NAME;
            default:
                throw new IllegalStateException("Unexpected value: " +
                        LanguageEnum.getEnumByValue(str));
        }
    }

    /**
     * 2. 将 测试数据 文件 从 minio 中下载 到本地
     *
     * @param DataIOId 测试数据前缀
     */
    public void saveStandardIOData2File(String DataIOId) {
        // 判断全局代码目录是否存在，不存在则新建
        if (!FileUtil.exist(samplePath)) {
            FileUtil.mkdir(samplePath);
        }

        //MinioUtil minioUtil = new MinioUtil();
        int i = minioUtil.saveIOFileFromMinio(DataIOId, samplePath);
        if (i == 1) {
            System.out.println("[=] INFO save sample file success");
        } else {
            System.err.println("[=] Error saveStandardIOSample2File save sample file failed");
        }
    }

    /**
     * 2.
     * samplePath : tmpcode/uuid/sample
     *
     * @param userSample user input sample
     */
    public void saveSampleDara2File(List<String> userSample) {
        // 判断全局代码目录是否存在，不存在则新建
        if (!FileUtil.exist(samplePath)) {
            FileUtil.mkdir(samplePath);
        }

        for (int i = 0; i < userSample.size(); i++) {
            String sampleFileAbsolutePath = samplePath + File.separator + i + SAMPLE_IN;
            System.out.println("[=] INFO sampleFileAbsolutePath : " + sampleFileAbsolutePath);
            Path path = Paths.get(sampleFileAbsolutePath);
            try {
                Files.write(path, userSample.get(i).getBytes());
            } catch (IOException e) {
                System.err.println("[=] Error in saveSampleDara2File : " + e.getMessage());
            }
        }
    }

    /**
     * 4.
     * <p>
     * 启动容器 编译 执行 代码
     * 由 不同 的 语言 实现
     */
    public void execDockerContainer() {
        System.out.println("重写此方法 : execDockerContainer");
    }

    /**
     * 4.5 删除运行容器
     * <p>
     *
     * @param cId 容器 ID
     */
    public void closeAndDeleteContainer(String cId) {
        try {
            dockerClient.stopContainerCmd(cId).exec();
            dockerClient.removeContainerCmd(cId).exec();
        } catch (Exception e) {
            System.err.println("[=] Error in closeAndDeleteContainer : " + e.getMessage());
        }
    }

    /**
     * 5.
     * <p>
     * 收集执行结果 与标准输出比较
     * 默认采用严格比较策略 严格逐行 逐字符比较
     */
    public void collectOutputResults() {
        List<File> outputFiles = Arrays.asList(
                Objects.requireNonNull(new File(samplePath).listFiles(
                        file -> file.getName().endsWith(".output")
                ))
        );
        List<File> answerFiles = Arrays.asList(
                Objects.requireNonNull(new File(samplePath).listFiles(
                        file -> file.getName().endsWith(".out")
                ))
        );
        Map<String, String> diffData = new HashMap<>();
        int k = answerFiles.size();
        for (int i = 0; i < k; i++) {
            try {
                boolean compareStrict = FileUtilBox.compareStrict(outputFiles.get(i), answerFiles.get(i));
                if (compareStrict) {
                    diffData.put(i + "", "correct");
                } else {
                    diffData.put(i + "", "wrong answer");
                }
            } catch (IOException e) {
                System.out.println("[=] Error : collectOutputResults in " + i + " file compare.");
            }
        }
        System.out.println("diffData : " + diffData);
        System.out.println(5);
    }

    /**
     * 6. del dir uuid
     *
     * @param upp tmp/uuid del uuid
     */
    public void deleteTmpDir(String upp) {
        System.out.println("[=] INFO deleteTmpDir : " + upp);
        FileUtil.del(upp);
        System.out.println(6);
    }
}