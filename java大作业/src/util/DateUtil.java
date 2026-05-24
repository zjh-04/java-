package util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 日期工具类
 * 提供统一的日期格式化与计算操作
 */
public class DateUtil {

    /** 标准日期格式 */
    private static final DateTimeFormatter FORMAT_STANDARD = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FORMAT_CHINESE  = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
    private static final DateTimeFormatter FORMAT_MONTH    = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * 获取当前日期字符串 (yyyy-MM-dd)
     */
    public static String getCurrentDate() {
        return LocalDate.now().format(FORMAT_STANDARD);
    }

    /**
     * 获取当前日期中文格式字符串
     */
    public static String getCurrentDateChinese() {
        return LocalDate.now().format(FORMAT_CHINESE);
    }

    /**
     * 获取当前年月 (yyyy-MM)
     */
    public static String getCurrentMonth() {
        return LocalDate.now().format(FORMAT_MONTH);
    }

    /**
     * 格式化日期为 yyyy-MM-dd
     */
    public static String formatDate(LocalDate dtDate) {
        if (dtDate == null) return "";
        return dtDate.format(FORMAT_STANDARD);
    }

    /**
     * 解析日期字符串为 LocalDate
     */
    public static LocalDate parseDate(String strDate) {
        if (strDate == null || strDate.isEmpty()) return LocalDate.now();
        return LocalDate.parse(strDate, FORMAT_STANDARD);
    }

    /**
     * 获取本月第一天的日期字符串
     */
    public static String getFirstDayOfMonth() {
        return LocalDate.now().withDayOfMonth(1).format(FORMAT_STANDARD);
    }

    /**
     * 获取上月月份标识 (yyyy-MM)
     */
    public static String getLastMonth() {
        return LocalDate.now().minusMonths(1).format(FORMAT_MONTH);
    }

    /**
     * 计算两个日期之间的天数差
     */
    public static long daysBetween(String strDate1, String strDate2) {
        LocalDate dt1 = parseDate(strDate1);
        LocalDate dt2 = parseDate(strDate2);
        return ChronoUnit.DAYS.between(dt1, dt2);
    }

    /**
     * 判断两个日期是否为同一天
     */
    public static boolean isSameDay(String strDate1, String strDate2) {
        return strDate1 != null && strDate1.equals(strDate2);
    }

    /**
     * 判断日期是否在当前月份内
     */
    public static boolean isCurrentMonth(String strDate) {
        if (strDate == null || strDate.isEmpty()) return false;
        return strDate.substring(0, 7).equals(getCurrentMonth());
    }

    /**
     * 获取最近N个月的月份标识列表
     * @param intCount 月份数量
     * @return 月份标识数组 (yyyy-MM)
     */
    public static String[] getLastNMonths(int intCount) {
        String[] arrMonths = new String[intCount];
        LocalDate dtNow = LocalDate.now();
        for (int i = 0; i < intCount; i++) {
            arrMonths[intCount - 1 - i] = dtNow.minusMonths(i).format(FORMAT_MONTH);
        }
        return arrMonths;
    }
}
