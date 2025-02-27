// -*- coding = utf-8 -*-
// @Time : 2025/2/27
// @Author : 1zqqx
// @File : ResourceUsage
// @Software : IntelliJ IDEA

package com.huest.codesandbox.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResourceUsage {
    private long cpuTime;      // CPU使用时间(ms)
    private long realTime;     // 实际运行时间(ms)
    private long memory;       // 内存使用峰值(KB)
    private long fileSize;     // 输出文件大小(bytes)
    private int exitCode;      // 进程退出码
    private String error;      // 错误信息
}
