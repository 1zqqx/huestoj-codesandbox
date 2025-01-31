// -*- coding = utf-8 -*-
// @Time : 2025/1/30
// @Author : 1zqqx
// @File : CcodeSandBoxTemplate
// @Software : IntelliJ IDEA

package com.huest.codesandbox.service.template;

import cn.hutool.core.io.FileUtil;
import com.huest.codesandbox.model.ExecuteCodeRequest;
import com.huest.codesandbox.model.ExecuteCodeResponse;
import com.huest.codesandbox.model.JudgeLimitInfo;
import com.huest.codesandbox.service.CodeSandBox;
import com.huest.codesandbox.utils.minio.MinioUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.UUID;


@Component
@Slf4j
public abstract class CodeSandBoxTemplate implements CodeSandBox {

    private static final String GLOBAL_CODE_DIR_NAME = "tmpcode";
    private static final String GLOBAL_SAMPLE_DIR_NAME = "sample";
    private static final String SAMPLE_IN = ".in";
    private static final String SAMPLE_OUT = ".out";


    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";
    private static final String GLOBAL_CPP_CLASS_NAME = "main.cc";


    // 用户当前目录
    private final String userDir = System.getProperty("user.dir");

    // File.separator , linux / , win \\
    private final String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;

    // tmpcode/uuid
    protected String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();


    // 用户代码临时文件父目录
    // 将整个父目录映射到 docker 容器
    // tmpcode/uuid/sample
    private final String samplePath = userCodeParentPath + File.separator + GLOBAL_SAMPLE_DIR_NAME;

    //@Autowired()
    protected MinioUtil minioUtil = new MinioUtil();

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
        Long qId = executeCodeRequest.getQuestionId();
        JudgeLimitInfo judgeLimitInfo = executeCodeRequest.getJudgeLimitInfo();

        // 1. 从 minio 中 根据 url 获取到用户提交的源代码文件 存储到本地
        saveCode2File(sourceCodeID);

        // 2. 从 minio 中 根据 题目 ID 获取到数据评测样例 存储到本地
        saveStandardIOSample2File(qId);
        saveStandardIOSample2File(qId);

        // 3. 复制编译文件模板

        // 4. 启动容器 编译 执行 代码

        // 5. 收集整理输出结果

        // 6. 删除临时文件

        return null;
    }

    /**
     * 1. user code to file
     *
     * @param srcCodeId 桶内唯一id
     */
    public void saveCode2File(String srcCodeId) {
        // 判断全局代码目录是否存在，不存在则新建
        if (!FileUtil.exist(userCodeParentPath)) {
            FileUtil.mkdir(userCodeParentPath);
        }

        // 用户存放的代码进行隔离
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_CPP_CLASS_NAME;
        System.out.println("[=] INFO userCodeParentPath : " + userCodePath);
        //MinioUtil minioUtil = new MinioUtil();
        int i = minioUtil.saveCodeFileFromMinio(srcCodeId, userCodePath);
        if (i == 1) {
            System.out.println("[=] INFO save file success");
        }
    }

    /**
     * 2.
     *
     * @param qid question ID
     */
    public void saveStandardIOSample2File(Long qid) {
        // 判断全局代码目录是否存在，不存在则新建
        if (!FileUtil.exist(samplePath)) {
            FileUtil.mkdir(samplePath);
        }

        //MinioUtil minioUtil = new MinioUtil();
        int i = minioUtil.saveIOFileFromMinio(qid, samplePath);
        if (i == 1) {
            System.out.println("[=] INFO save sample file success");
        }
    }

}

