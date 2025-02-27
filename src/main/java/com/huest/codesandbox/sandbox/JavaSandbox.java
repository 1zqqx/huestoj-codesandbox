// -*- coding = utf-8 -*-
// @Time : 2025/2/27
// @Author : 1zqqx
// @File : JavaSandbox
// @Software : IntelliJ IDEA

package com.huest.codesandbox.sandbox;

import com.huest.codesandbox.model.JudgeLimitInfo;
import com.huest.codesandbox.model.ResourceUsage;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JavaSandbox {

    interface CLibrary extends Library {
        CLibrary INSTANCE = Native.load("c", CLibrary.class);

        int unshare(int flags);

        int mount(String source, String target, String filesystemtype, long mountflags, String data);

        int chroot(String path);

        int setrlimit(int resource, rlimit rlim);

        int prctl(int option, long arg2, long arg3, long arg4, long arg5);

        int pivot_root(String new_root, String put_old);

        int getrusage(int who, rusage usage);

        int wait4(int pid, int[] status, int options, rusage usage);
    }

    // 资源限制结构体
    public static class rlimit extends Structure {
        public long rlim_cur;
        public long rlim_max;

        @Override
        protected java.util.List<String> getFieldOrder() {
            return Arrays.asList("rlim_cur", "rlim_max");
        }
    }

    // 资源使用统计结构体
    public static class rusage extends Structure {
        public timeval ru_utime;    // 用户CPU时间
        public timeval ru_stime;    // 系统CPU时间
        public long ru_maxrss;      // 最大常驻集大小
        public long ru_ixrss;       // 共享内存大小
        public long ru_idrss;       // 非共享内存大小
        public long ru_isrss;       // 栈大小
        public long ru_minflt;      // 页面错误数
        public long ru_majflt;      // 主要页面错误数
        public long ru_nswap;       // 交换次数
        public long ru_inblock;     // 块输入操作
        public long ru_oublock;     // 块输出操作
        public long ru_msgsnd;      // 发送的消息数
        public long ru_msgrcv;      // 接收的消息数
        public long ru_nsignals;    // 收到的信号数
        public long ru_nvcsw;       // 自愿上下文切换
        public long ru_nivcsw;      // 非自愿上下文切换

        @Override
        protected java.util.List<String> getFieldOrder() {
            return Arrays.asList(
                    "ru_utime", "ru_stime", "ru_maxrss", "ru_ixrss",
                    "ru_idrss", "ru_isrss", "ru_minflt", "ru_majflt",
                    "ru_nswap", "ru_inblock", "ru_oublock", "ru_msgsnd",
                    "ru_msgrcv", "ru_nsignals", "ru_nvcsw", "ru_nivcsw"
            );
        }
    }

    public static class timeval extends Structure {
        public long tv_sec;     // 秒
        public long tv_usec;    // 微秒

        @Override
        protected java.util.List<String> getFieldOrder() {
            return Arrays.asList("tv_sec", "tv_usec");
        }
    }

    // 常量定义
    private static final int CLONE_NEWNS = 0x00020000;   // 挂载命名空间
    private static final int CLONE_NEWUTS = 0x04000000;  // UTS命名空间
    private static final int CLONE_NEWPID = 0x20000000;  // PID命名空间
    private static final int CLONE_NEWNET = 0x40000000;  // 网络命名空间
    private static final int CLONE_NEWIPC = 0x08000000;  // IPC命名空间

    private static final int RLIMIT_CPU = 0;    // CPU时间限制
    private static final int RLIMIT_AS = 9;     // 虚拟内存限制
    private static final int RLIMIT_STACK = 3;  // 栈大小限制
    private static final int RLIMIT_FSIZE = 1;  // 文件大小限制
    private static final int RLIMIT_NOFILE = 7; // 文件描述符限制
    private static final int RLIMIT_NPROC = 6;  // 进程数限制

    private static final int PR_SET_NO_NEW_PRIVS = 38;  // 禁止获取新权限
    private static final int PR_SET_SECCOMP = 22;       // 设置seccomp


    /**
     * 编译源代码
     */
    public int compile(String sourceCodePath, String command) throws IOException {
        // 检查是否具有足够权限
        if (!hasRequiredPrivileges()) {
            log.warn("Sandbox requires root privileges or CAP_SYS_ADMIN capability");
            return -1;
        }

        // 创建新的命名空间
        int flags = CLONE_NEWNS | CLONE_NEWUTS | CLONE_NEWNET | CLONE_NEWIPC;
        if (CLibrary.INSTANCE.unshare(flags) != 0) {
            log.error("Failed to create new namespace. Error code: {}", Native.getLastError());
            return -1;
        }

        // 设置安全限制
        setupSecurityLimits();

        // 设置资源限制
        setupCompileResourceLimits();

        // 创建沙箱环境
        setupSandboxEnvironment();

        // 执行编译命令
        Process process = new ProcessBuilder()
                .command("sh", "-c", command)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start();

        try {
            return process.waitFor();
        } catch (InterruptedException e) {
            throw new IOException("Compilation interrupted", e);
        }
    }

    private boolean hasRequiredPrivileges() {
        // 检查是否为root用户
        if (System.getProperty("user.name").equals("root")) {
            return true;
        }

        // 尝试执行一个需要特权的操作来测试权限
        try {
            return CLibrary.INSTANCE.unshare(CLONE_NEWNS) == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void setupCompileResourceLimits() {
        // 编译时间限制
        rlimit cpuLimit = new rlimit();
        cpuLimit.rlim_cur = cpuLimit.rlim_max = 10; // 10秒
        CLibrary.INSTANCE.setrlimit(RLIMIT_CPU, cpuLimit);

        // 编译内存限制
        rlimit memLimit = new rlimit();
        memLimit.rlim_cur = memLimit.rlim_max = 512 * 1024 * 1024L; // 512MB
        CLibrary.INSTANCE.setrlimit(RLIMIT_AS, memLimit);

        // 文件大小限制
        rlimit fsizeLimit = new rlimit();
        fsizeLimit.rlim_cur = fsizeLimit.rlim_max = 50 * 1024 * 1024L; // 50MB
        CLibrary.INSTANCE.setrlimit(RLIMIT_FSIZE, fsizeLimit);

        // 进程数限制
        rlimit procLimit = new rlimit();
        procLimit.rlim_cur = procLimit.rlim_max = 10; // 最多10个进程
        CLibrary.INSTANCE.setrlimit(RLIMIT_NPROC, procLimit);
    }

    private void setupSandboxEnvironment() throws IOException {
        // 创建临时根目录
        Path tempRoot = Files.createTempDirectory("sandbox");

        // 创建必要的目录
        Files.createDirectories(tempRoot.resolve("bin"));
        Files.createDirectories(tempRoot.resolve("lib"));
        Files.createDirectories(tempRoot.resolve("lib64"));
        Files.createDirectories(tempRoot.resolve("usr"));

        // 复制必要的文件
        copyExecutables(tempRoot);

        // 切换根目录
        CLibrary.INSTANCE.chroot(tempRoot.toString());
    }

    private void copyExecutables(Path tempRoot) throws IOException {
        // 复制基本命令
        String[] commands = {"/bin/sh", "/usr/bin/gcc", "/usr/bin/g++", "/usr/bin/javac", "/usr/bin/java"};
        for (String cmd : commands) {
            Path path = Paths.get(cmd);
            if (Files.exists(path)) {
                Files.copy(path, tempRoot.resolve("bin").resolve(path.getFileName()));
            }
        }

        // 复制必要的库文件
        String[] libDirs = {"/lib", "/lib64", "/usr/lib", "/usr/lib64"};
        for (String libDir : libDirs) {
            Path path = Paths.get(libDir);
            if (Files.exists(path)) {
                Files.walk(path)
                        .filter(Files::isRegularFile)
                        .forEach(source -> {
                            try {
                                Path target = tempRoot.resolve(tempRoot.relativize(source));
                                Files.createDirectories(target.getParent());
                                Files.copy(source, target);
                            } catch (IOException e) {
                                log.warn("Failed to copy library: {}", source);
                            }
                        });
            }
        }
    }

    /**
     * 运行程序测试用例
     * <p>
     * 运行已编译的程序并统计资源使用
     */
    public ResourceUsage runWithStats(String command, String inputFile, String outputFile, JudgeLimitInfo limitInfo) throws IOException {
        // 检查是否具有足够权限
        if (!hasRequiredPrivileges()) {
            log.warn("Sandbox requires root privileges or CAP_SYS_ADMIN capability");
            return null;
        }

        long startTime = System.currentTimeMillis();

        // 创建新的命名空间
        if (CLibrary.INSTANCE.unshare(CLONE_NEWNS | CLONE_NEWUTS | CLONE_NEWPID | CLONE_NEWNET | CLONE_NEWIPC) != 0) {
            throw new IOException("Failed to create new namespace");
        }

        // 设置资源限制
        setupResourceLimits(limitInfo);
        setupSecurityLimits();
        setupCgroups(limitInfo);

        // 执行程序
        Process process = new ProcessBuilder()
                .command("sh", "-c", command)
                .redirectInput(new File(inputFile))
                .redirectOutput(new File(outputFile))
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start();

        // 获取进程ID
        long pid = process.pid();

        // 准备资源统计
        rusage usage = new rusage();
        int[] status = new int[1];

        try {
            // 等待进程结束并获取资源使用情况
            CLibrary.INSTANCE.wait4((int) pid, status, 0, usage);

            long endTime = System.currentTimeMillis();

            // 计算CPU时间（用户态 + 内核态）
            long cpuTime = (usage.ru_utime.tv_sec * 1000 + usage.ru_utime.tv_usec / 1000) +
                    (usage.ru_stime.tv_sec * 1000 + usage.ru_stime.tv_usec / 1000);

            // 获取错误输出
            String error = new BufferedReader(new InputStreamReader(process.getErrorStream()))
                    .lines()
                    .collect(Collectors.joining("\n"));

            // 获取输出文件大小
            long fileSize = Files.size(Paths.get(outputFile));

            return ResourceUsage.builder()
                    .cpuTime(cpuTime)
                    .realTime(endTime - startTime)
                    .memory(usage.ru_maxrss)
                    .fileSize(fileSize)
                    .exitCode(status[0])
                    .error(error)
                    .build();

        } catch (Exception e) {
            log.error("Error while running program", e);
            throw new IOException("Failed to run program", e);
        }
    }

    private void setupResourceLimits(JudgeLimitInfo limitInfo) {
        // CPU时间限制
        rlimit cpuLimit = new rlimit();
        cpuLimit.rlim_cur = cpuLimit.rlim_max = limitInfo.getTimeLimit() / 1000 + 1;
        CLibrary.INSTANCE.setrlimit(RLIMIT_CPU, cpuLimit);

        // 内存限制
        rlimit memLimit = new rlimit();
        memLimit.rlim_cur = memLimit.rlim_max = limitInfo.getMemoryLimit() * 1024L * 1024L;
        CLibrary.INSTANCE.setrlimit(RLIMIT_AS, memLimit);

        // 栈限制
        rlimit stackLimit = new rlimit();
        stackLimit.rlim_cur = stackLimit.rlim_max = limitInfo.getStackLimit() * 1024L * 1024L;
        CLibrary.INSTANCE.setrlimit(RLIMIT_STACK, stackLimit);
    }

    private void setupSecurityLimits() {
        // 禁止获取新权限
        CLibrary.INSTANCE.prctl(PR_SET_NO_NEW_PRIVS, 1, 0, 0, 0);

        // 禁止访问特定目录
        String[] restrictedDirs = {"/proc", "/sys", "/dev", "/tmp"};
        for (String dir : restrictedDirs) {
            try {
                CLibrary.INSTANCE.mount("none", dir, "tmpfs", 0, "");
            } catch (Exception e) {
                log.warn("Failed to restrict access to {}: {}", dir, e.getMessage());
            }
        }
    }

    private void setupCgroups(JudgeLimitInfo limitInfo) throws IOException {
        String cgroupPath = "/sys/fs/cgroup";

        // 创建内存cgroup
        Path memoryPath = Paths.get(cgroupPath, "memory", "judge");
        Files.createDirectories(memoryPath);
        Files.write(memoryPath.resolve("memory.limit_in_bytes"),
                String.valueOf(limitInfo.getMemoryLimit() * 1024L * 1024L).getBytes());

        // 创建CPU cgroup
        Path cpuPath = Paths.get(cgroupPath, "cpu", "judge");
        Files.createDirectories(cpuPath);
        Files.write(cpuPath.resolve("cpu.cfs_quota_us"),
                String.valueOf(limitInfo.getTimeLimit() * 1000).getBytes());
        Files.write(cpuPath.resolve("cpu.cfs_period_us"),
                "1000000".getBytes());
    }
}
