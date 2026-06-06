package com.guicang.util;

/**
 * 输入校验工具 — 从原项目适配
 */
public class Validator {

    /** 用户名校验：3-16位字母/数字/中文/下划线 */
    public static boolean isValidUsername(String s) {
        return s != null && s.matches("[a-zA-Z0-9_\\u4e00-\\u9fa5]{3,16}");
    }

    /** 密码校验：6-20位，含字母和数字 */
    public static boolean isValidPassword(String s) {
        return s != null && s.length() >= 6 && s.length() <= 20
                && s.matches(".*[a-zA-Z].*") && s.matches(".*[0-9].*");
    }

    /** 邮箱格式（可选字段，空也通过） */
    public static boolean isValidEmail(String s) {
        return s == null || s.isEmpty() || s.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    }

    /** 金额校验：正数，最多两位小数 */
    public static boolean isPositiveNumber(String s) {
        return s != null && s.matches("^\\d+(\\.\\d{1,2})?$") && Double.parseDouble(s) > 0;
    }

    /** 非空 */
    public static boolean isNotEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }

    /** 解析金额 */
    public static double parseAmount(String s) {
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0; }
    }
}
