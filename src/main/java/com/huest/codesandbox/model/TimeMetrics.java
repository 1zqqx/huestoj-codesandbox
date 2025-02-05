package com.huest.codesandbox.model;

import lombok.Data;

/**
 * 时间测量结果封装类
 */
@Data
public class TimeMetrics {
    /**
     * 挂钟时间 单位 ms
     * <p>
     * 实际经过的时间，即从命令开始到结束所经过的墙钟时间，包括所有等待时间。
     */
    private long wallTimeMillis;

    /**
     * 用户态 CPU 时间
     */
    private double userTimeSeconds;

    /**
     * 内核态 CPU 时间
     */
    private double systemTimeSeconds;

    /**
     * CPU 时间（User + System，毫秒）
     */
    private long cpuTimeMillis;

    /**
     * 峰值内存（KB）
     */
    private long maxMemoryKB;
}