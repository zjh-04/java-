package com.guicang.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 日期工具类 — 从原项目适配，去掉 Swing 无关方法
 */
public class DateUtil {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 获取当前日期字符串 yyyy-MM-dd */
    public static String today() {
        return LocalDate.now().format(FMT);
    }

    /** 计算两个日期之间的天数差 (date1到date2) */
    public static long daysBetween(String date1, String date2) {
        LocalDate d1 = LocalDate.parse(date1, FMT);
        LocalDate d2 = LocalDate.parse(date2, FMT);
        return ChronoUnit.DAYS.between(d1, d2);
    }
}
