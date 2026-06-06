package util;

/**
 * 输入验证工具类
 * 对用户输入进行合法性校验
 */
public class Validator {

    /**
     * 验证用户名格式：3-16位字母、数字、下划线
     */
    public static boolean isValidUsername(String strUsername) {
        if (strUsername == null) return false;
        return strUsername.matches("^[a-zA-Z0-9_\\u4e00-\\u9fa5]{3,16}$");
    }

    /**
     * 验证密码格式：6-20位，至少包含字母和数字
     */
    public static boolean isValidPassword(String strPassword) {
        if (strPassword == null) return false;
        return strPassword.length() >= 6 && strPassword.length() <= 20
                && strPassword.matches(".*[a-zA-Z].*")
                && strPassword.matches(".*[0-9].*");
    }

    /**
     * 验证邮箱格式
     */
    public static boolean isValidEmail(String strEmail) {
        if (strEmail == null || strEmail.isEmpty()) return true; // 邮箱可选
        return strEmail.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    }

    /**
     * 验证金额：正数，最多两位小数
     */
    public static boolean isValidAmount(String strAmount) {
        if (strAmount == null) return false;
        try {
            double dblVal = Double.parseDouble(strAmount);
            return dblVal > 0 && dblVal <= 99999999.99;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 解析金额字符串为 double
     */
    public static double parseAmount(String strAmount) {
        try {
            return Double.parseDouble(strAmount);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * 检查字符串是否非空
     */
    public static boolean isNotEmpty(String strValue) {
        return strValue != null && !strValue.trim().isEmpty();
    }
}
