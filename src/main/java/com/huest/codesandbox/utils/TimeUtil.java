// -*- coding = utf-8 -*-
// @Time : 2025/1/30
// @Author : 1zqqx
// @File : TimeUtil
// @Software : IntelliJ IDEA

package com.huest.codesandbox.utils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 时间转换
 */
public class TimeUtil {

    /**
     * 转换成中国标准时间
     * @param zonedDateTime 推荐
     * @param format 字符串 yyyy-MM-dd HH:mm:ss
     * @return String
     */
    public static String parseDate(ZonedDateTime zonedDateTime, String format) {
        // "yyyy-MM-dd HH:mm:ss"
        return zonedDateTime.withZoneSameInstant(ZoneId.of("Asia/Shanghai"))
                .format(DateTimeFormatter.ofPattern(format));
    }
}
