// -*- coding = utf-8 -*-
// @Time : 2025/2/27
// @Author : 1zqqx
// @File : judgeService
// @Software : IntelliJ IDEA

package com.huest.codesandbox.service;

import com.huest.codesandbox.common.JudgeStatusEnum;
import com.huest.codesandbox.common.LanguageEnum;
import com.huest.codesandbox.comparator.OutputComparator;
import com.huest.codesandbox.compiler.CompileStrategy;
import com.huest.codesandbox.compiler.CompileStrategyFactory;
import com.huest.codesandbox.exception.JudgeException;
import com.huest.codesandbox.model.*;
import com.huest.codesandbox.sandbox.JavaSandbox;
import com.huest.codesandbox.utils.TestDataHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class JudgeService {

    @Value("${judge.work-dir}")
    private String workDir;

    @Resource
    private CompileStrategyFactory compileStrategyFactory;

    @Resource
    private JavaSandbox sandbox;

    @Resource
    private TestDataHandler testDataHandler;

    @Resource
    private OutputComparator outputComparator;

    @Resource
    private JudgeResultService resultService;

    public JudgeResult judge(ExecuteCodeRequest request) {
        /**
         * 创建评测目录
         * /tmp/judge/judge_1_1001_10086
         */
        Path workPath = createWorkDirectory(request);

        try {
            // 保存源代码
            Path sourcePath = saveSourceCode(workPath, request);

            // 编译代码
            Path execPath = compile(sourcePath, request.getLanguage());

            // 下载并准备测试数据
            Path testDataPath = testDataHandler.prepareTestData(workPath, request.getQueID());

            // 执行所有测试用例
            List<TestCaseResult> testResults = runTestCases(execPath, testDataPath, request.getJudgeLimitInfo());

            // 收集结果
            return resultService.summarize(testResults);
        } catch (JudgeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Judge error", e);
            throw new JudgeException(JudgeStatusEnum.SYSTEM_ERROR, "Judge failed", e.getMessage());
        }
    }

    /**
     * 用户提交代码存放目录
     *
     * @param request req
     * @return re
     */
    private Path createWorkDirectory(ExecuteCodeRequest request) {
        try {
            Path path = Paths.get(workDir,
                    String.format("judge_%s_%s_%d",
                            request.getUserID(),
                            request.getQueID(),
                            System.currentTimeMillis()));
            Files.createDirectories(path);
            return path;
        } catch (Exception e) {
            throw new JudgeException(JudgeStatusEnum.SYSTEM_ERROR, "Failed to create work directory");
        }
    }

    /**
     * 保存 代码 到本地
     *
     * @param workPath w
     * @param request  r
     * @return re
     */
    private Path saveSourceCode(Path workPath, ExecuteCodeRequest request) {
        try {
            Path sourcePath = workPath.resolve("Main." + getFileExtension(request.getLanguage()));
            Files.writeString(sourcePath, request.getSourceCode());
            return sourcePath;
        } catch (Exception e) {
            throw new JudgeException(JudgeStatusEnum.SYSTEM_ERROR, "Failed to save source code");
        }
    }

    /**
     * Utils
     *
     * @param language
     * @return
     */
    private String getFileExtension(LanguageEnum language) {
        switch (language) {
            case C:
                return "c";
            case CPP:
                return "cpp";
            case JAVA:
                return "java";
            case PYTHON3:
                return "py";
            default:
                throw new IllegalArgumentException("Unsupported language");
        }
    }

    /**
     * 编译代码
     *
     * @param sourcePath
     * @param language
     * @return
     * @throws IOException
     */
    private Path compile(Path sourcePath, LanguageEnum language) throws IOException {
        CompileStrategy strategy = compileStrategyFactory.getStrategy(language);
        String command = strategy.getCompileCommand(
                sourcePath.toString(),
                sourcePath.getParent().resolve("Main").toString()
        );

        System.err.println("[=] ERROR command " + command);
        System.err.println("[=] ERROR sourcePath " + sourcePath);

        int result = sandbox.compile(sourcePath.toString(), command);
        if (result != 0) {
            System.err.println("result " + result);
            throw new JudgeException(JudgeStatusEnum.COMPILE_ERROR, "Compilation failed");
        }

        return sourcePath.getParent().resolve("Main");
    }

    /**
     * 执行 用例 数据
     *
     * @param execPath
     * @param testDataPath
     * @param limitInfo
     * @return
     * @throws IOException
     */
    private List<TestCaseResult> runTestCases(Path execPath, Path testDataPath, JudgeLimitInfo limitInfo) throws IOException {
        // results
        List<TestCaseResult> results = new ArrayList<>();

        // 所有输入 文件 Path
        List<Path> testCases = getTestCases(testDataPath);

        for (int i = 0; i < testCases.size(); i++) {
            // 标准输入
            Path inputFile = testCases.get(i);
            // 用户输出
            Path outputFile = execPath.getParent().resolve(i + ".output");
            // 标准输出
            Path expectedOutput = Paths.get(inputFile.toString().replace(".in", ".out"));

            // 运行程序并获取资源使用情况
            ResourceUsage usage = sandbox.runWithStats(
                    execPath.toString(),
                    inputFile.toString(),
                    outputFile.toString(),
                    limitInfo
            );

            // 比较输出
            JudgeStatusEnum status = outputComparator.compare(outputFile, expectedOutput);

            results.add(TestCaseResult.builder()
                    .testCaseId(i + 1)
                    .status(status)
                    .time((int) usage.getCpuTime())
                    .memory((int) usage.getMemory())
                    .build());
        }

        return results;
    }

    private List<Path> getTestCases(Path testDataPath) {
        try {
            List<Path> testCases = new ArrayList<>();
            Files.list(testDataPath)
                    .filter(path -> path.toString().endsWith(".in"))
                    .forEach(testCases::add);
            return testCases;
        } catch (Exception e) {
            throw new JudgeException(JudgeStatusEnum.SYSTEM_ERROR, "Failed to list test cases");
        }
    }
}
