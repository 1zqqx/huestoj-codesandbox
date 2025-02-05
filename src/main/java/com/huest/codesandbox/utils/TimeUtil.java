// -*- coding = utf-8 -*-
// @Time : 2025/1/30
// @Author : 1zqqx
// @File : TimeUtil
// @Software : IntelliJ IDEA

package com.huest.codesandbox.utils;

import com.huest.codesandbox.model.TimeMetrics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 时间转换
 */
public class TimeUtil {

    /**
     * 转换成中国标准时间
     *
     * @param zonedDateTime 推荐
     * @param format        字符串 yyyy-MM-dd HH:mm:ss
     * @return String
     */
    public static String parseDate(ZonedDateTime zonedDateTime, String format) {
        // "yyyy-MM-dd HH:mm:ss"
        return zonedDateTime.withZoneSameInstant(ZoneId.of("Asia/Shanghai"))
                .format(DateTimeFormatter.ofPattern(format));
    }

    /**
     * 工具类
     * <p>
     * 解析 GNU time 生成的日志文件
     * <p>
     * a. Elapsed (wall clock) time：实际经过的时间，即从命令开始到结束所经过的墙钟时间，包括所有等待时间。
     * <p>
     * b. User time (seconds)：用户态 CPU 时间，即程序在用户态执行所花费的时间，单位是秒。
     * <p>
     * c. System time (seconds)：内核态 CPU 时间，即程序在内核态执行所花费的时间，单位是秒。
     * <p>
     * d. Percent of CPU this job got：CPU 占用率，即程序执行期间所占用的 CPU 百分比。
     * <p>
     * e. Maximum resident set size：最大驻留集大小，即程序在执行过程中占用的最大物理内存量，单位是 KB。
     * <p>
     * f. Average stack size (kB)：平均栈大小，单位是千字节(kB)。
     * <p>
     * g. Exit status：命令的退出状态码。0 表示命令成功执行，非 0 表示有错误发生。
     * <p>
     * <p>
     * 实际上 可能仅用到
     * Elapsed (wall clock) time
     * &
     * Maximum resident set size
     */
    public static TimeMetrics parseTimeLog(Path timeLogPath) throws IOException {
        List<String> lines = Files.readAllLines(timeLogPath);
        TimeMetrics metrics = new TimeMetrics();

        for (String line : lines) {
            if (line.contains("Elapsed (wall clock) time")) {
                String timeStr = line.split(":")[1].trim();
                metrics.setWallTimeMillis(parseTimeToMillis(timeStr));
            } else if (line.contains("User time (seconds)")) {
                metrics.setUserTimeSeconds(Double.parseDouble(line.split(":")[1].trim()));
            } else if (line.contains("System time (seconds)")) {
                metrics.setSystemTimeSeconds(Double.parseDouble(line.split(":")[1].trim()));
            } else if (line.contains("Maximum resident set size")) {
                long memoryKB = Long.parseLong(line.split(":")[1].trim().replace(" kB", ""));
                metrics.setMaxMemoryKB(memoryKB);
            }
        }

        // CPU 时间 = User + System
        metrics.setCpuTimeMillis(
                (long) ((metrics.getUserTimeSeconds() + metrics.getSystemTimeSeconds()) * 1000)
        );

        return metrics;
    }

    /**
     * 将时间字符串（格式如 "0:02.34"）转换为毫秒
     */
    private static long parseTimeToMillis(String timeStr) {
        System.out.println("timeStr : " + timeStr);
        String[] parts = timeStr.split("[:.]");
        long minutes = Long.parseLong(parts[0]);
        long seconds = Long.parseLong(parts[1]);
        long hundredths = Long.parseLong(parts[2]);
        return (minutes * 60 + seconds) * 1000 + hundredths * 10;
    }
}
