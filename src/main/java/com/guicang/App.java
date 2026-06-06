package com.guicang;

import com.google.gson.Gson;
import com.guicang.bridge.JavaBackend;
import com.guicang.config.AppConfig;
import com.guicang.config.DatabaseConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * 「归藏」主入口
 * 启动顺序: ①初始化 SQLite → ②启动内嵌 HTTP 服务器 → ③创建 Swing JFrame + JCEF 窗口
 */
public class App {

    private static final Gson gson = new Gson();
    private static final JavaBackend backend = new JavaBackend();

    public static void main(String[] args) {
        // ① 初始化 SQLite
        System.out.println("[归藏] 正在初始化数据库...");
        DatabaseConfig.getConnection();

        // ② 启动内嵌 HTTP 服务器
        HttpServer server = startHttpServer();
        System.out.println("[归藏] HTTP 服务器已启动 http://localhost:" + AppConfig.HTTP_PORT);

        // ③ 创建 Swing 窗口 + JCEF 浏览器
        SwingUtilities.invokeLater(() -> createWindow(server));
    }

    // ==================== HTTP 服务器 ====================

    private static HttpServer startHttpServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(AppConfig.HTTP_PORT), 0);
            server.setExecutor(Executors.newFixedThreadPool(4));

            // API 路由
            server.createContext("/api/", new ApiHandler());

            // 静态文件
            server.createContext("/", new StaticFileHandler());

            server.start();
            return server;
        } catch (IOException e) {
            throw new RuntimeException("启动 HTTP 服务器失败", e);
        }
    }

    /** API 请求分发 */
    static class ApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

            if ("OPTIONS".equals(ex.getRequestMethod())) {
                ex.sendResponseHeaders(204, -1);
                return;
            }

            String path = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod();
            String body = readBody(ex);

            try {
                String result = dispatch(path, method, body);
                byte[] bytes = result.getBytes(StandardCharsets.UTF_8);
                ex.sendResponseHeaders(200, bytes.length);
                ex.getResponseBody().write(bytes);
            } catch (Exception e) {
                String err = gson.toJson(Map.of("success", false, "message", e.getMessage()));
                byte[] bytes = err.getBytes(StandardCharsets.UTF_8);
                ex.sendResponseHeaders(400, bytes.length);
                ex.getResponseBody().write(bytes);
            } finally {
                ex.close();
            }
        }

        private String dispatch(String path, String method, String body) {
            return switch (path) {
                // 认证
                case "/api/login"      -> backend.login(body);
                case "/api/register"   -> backend.register(body);
                case "/api/session"    -> backend.getSessionUser();

                // 资产 CRUD
                case "/api/assets"     -> "GET".equals(method) ? backend.getAllAssets() : backend.createAsset(body);
                case "/api/assets/archived" -> backend.getArchivedAssets();

                // 仪表盘 / 成就 / 洞察
                case "/api/dashboard"    -> backend.getDashboard();
                case "/api/achievements"          -> backend.getAchievements();
                case "/api/achievements/pending" -> backend.getPendingAchievements();
                case "/api/achievements/acknowledge" -> backend.acknowledgeAchievements(body);
                case "/api/insights"     -> backend.getInsights();

                // 导出
                case "/api/export"       -> backend.exportData();

                default -> {
                    // 动态路由: /api/assets/{id}/...
                    String[] parts = path.split("/");
                    if (parts.length >= 4 && "api".equals(parts[1]) && "assets".equals(parts[2])) {
                        String assetId = parts[3];
                        if (parts.length == 4) {
                            // /api/assets/{id}
                            yield switch (method) {
                                case "GET"    -> backend.getAssetById(assetId);
                                case "PUT"    -> backend.updateAsset(assetId, body);
                                case "DELETE" -> backend.deleteAsset(assetId);
                                default -> gson.toJson(Map.of("success", false, "message", "unknown method"));
                            };
                        } else if (parts.length == 5) {
                            yield switch (parts[4]) {
                                case "check-in" -> backend.checkIn(assetId);
                                case "consume"  -> backend.consumeStock(body);
                                case "restock"  -> backend.restock(body);
                                case "punch"    -> backend.punchCard(assetId);
                                case "topup"    -> backend.topup(body);
                                case "spend"    -> backend.spend(body);
                                case "status"   -> backend.updateCollectStatus(assetId);
                                case "compare"  -> backend.comparePrice(body);
                                case "history"  -> backend.getHistory(assetId);
                                case "archive"  -> backend.archiveAsset(assetId);
                                case "restore"  -> backend.restoreAsset(assetId);
                                default -> gson.toJson(Map.of("success", false, "message", "unknown action"));
                            };
                        }
                    }
                    // 设置
                    if ("/api/config".equals(path)) {
                        yield "GET".equals(method) ? backend.getConfig("theme") : backend.setConfig(body);
                    }
                    yield gson.toJson(Map.of("success", false, "message", "未知 API: " + path));
                }
            };
        }
    }

    /** 静态文件服务 */
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String path = ex.getRequestURI().getPath();
            if ("/".equals(path)) path = "/index.html";

            String resourcePath = "/web" + path;
            InputStream is = App.class.getResourceAsStream(resourcePath);

            if (is == null) {
                String notFound = "404 Not Found: " + path;
                ex.sendResponseHeaders(404, notFound.length());
                ex.getResponseBody().write(notFound.getBytes());
                ex.close();
                return;
            }

            // MIME type
            String mime = "text/html";
            if (path.endsWith(".css")) mime = "text/css";
            else if (path.endsWith(".js")) mime = "application/javascript";
            else if (path.endsWith(".woff2")) mime = "font/woff2";
            else if (path.endsWith(".json")) mime = "application/json";
            else if (path.endsWith(".svg")) mime = "image/svg+xml";

            ex.getResponseHeaders().set("Content-Type", mime + "; charset=UTF-8");
            byte[] bytes = is.readAllBytes();
            is.close();
            ex.sendResponseHeaders(200, bytes.length);
            ex.getResponseBody().write(bytes);
            ex.close();
        }
    }

    private static String readBody(HttpExchange ex) throws IOException {
        InputStream is = ex.getRequestBody();
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    // ==================== JCEF 窗口 ====================

    private static void createWindow(HttpServer server) {
        JFrame frame = new JFrame(AppConfig.WINDOW_TITLE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(AppConfig.WINDOW_WIDTH, AppConfig.WINDOW_HEIGHT);
        frame.setMinimumSize(new Dimension(AppConfig.WINDOW_MIN_WIDTH, AppConfig.WINDOW_MIN_HEIGHT));
        frame.setLocationRelativeTo(null);

        // 尝试初始化 JCEF；如果 JCEF 不可用则降级为系统浏览器
        try {
            initJcef(frame);
        } catch (Exception | NoClassDefFoundError e) {
            System.err.println("[归藏] JCEF 初始化失败，降级为系统浏览器: " + e.getMessage());
            fallbackToDesktop(frame);
        }
    }

    private static void initJcef(JFrame frame) throws Exception {
        // JCEF 初始化（使用 jcefmaven 自动管理原生库）
        me.friwi.jcefmaven.CefAppBuilder builder = new me.friwi.jcefmaven.CefAppBuilder();
        builder.getCefSettings().windowless_rendering_enabled = false;

        org.cef.CefApp cefApp = builder.build();
        org.cef.CefClient client = cefApp.createClient();

        String url = "http://localhost:" + AppConfig.HTTP_PORT + "/index.html";
        org.cef.browser.CefBrowser browser = client.createBrowser(url, false, false);

        // 将 CefBrowser 嵌入 Swing 面板
        java.awt.Component browserUI = browser.getUIComponent();
        frame.add(browserUI, BorderLayout.CENTER);

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                browser.close(true);
                client.dispose();
                cefApp.dispose();
                DatabaseConfig.close();
            }
        });

        frame.setVisible(true);
    }

    /** JCEF 不可用时：用系统默认浏览器打开 */
    private static void fallbackToDesktop(JFrame frame) {
        try {
            Desktop.getDesktop().browse(new URI("http://localhost:" + AppConfig.HTTP_PORT));
        } catch (Exception ex) {
            System.err.println("[归藏] 无法打开浏览器: " + ex.getMessage());
        }

        JLabel label = new JLabel("<html><center><h2>归藏</h2><p>服务已启动<br>请在浏览器中打开: http://localhost:"
                + AppConfig.HTTP_PORT + "</p></center></html>", SwingConstants.CENTER);
        frame.add(label);
        frame.setVisible(true);
    }
}
