package com.guicang.config;

/**
 * 应用配置常量 — 窗口 / 路径 / HTTP 端口
 */
public class AppConfig {
    private AppConfig() {}

    /** 窗口初始宽度 */
    public static final int WINDOW_WIDTH  = 1040;
    /** 窗口初始高度 */
    public static final int WINDOW_HEIGHT = 700;
    /** 窗口最小宽度 */
    public static final int WINDOW_MIN_WIDTH  = 900;
    /** 窗口最小高度 */
    public static final int WINDOW_MIN_HEIGHT = 640;
    /** 窗口标题 */
    public static final String WINDOW_TITLE = "归藏";

    /** 内嵌 HTTP 服务器端口 */
    public static final int HTTP_PORT = 18080;

    /** 前端资源在 classpath 下的根路径 */
    public static final String WEB_ROOT = "/web";

    /** 首页文件 */
    public static final String INDEX_FILE = "index.html";
}
