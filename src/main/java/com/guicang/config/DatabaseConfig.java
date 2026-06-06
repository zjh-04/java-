package com.guicang.config;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * SQLite 数据库初始化与连接管理
 */
public class DatabaseConfig {

    private static final String DB_FILE   = "assets.db";
    private static final String SCHEMA_SQL = "/db/schema.sql";

    /** 数据目录: 本地开发用 ./data, exe 分发用 %APPDATA%\Guicang\data */
    private static final String DATA_DIR = resolveDataDir();

    private static String resolveDataDir() {
        // 本地调试：项目目录下已有 data/assets.db 则优先使用
        Path localData = Paths.get("./data/assets.db").toAbsolutePath();
        if (Files.exists(localData)) {
            System.out.println("[归藏] 使用本地数据库: " + localData);
            return "./data";
        }
        // 打包 exe：存到用户 AppData
        String os = System.getProperty("os.name", "").toLowerCase();
        String appData;
        if (os.contains("win")) {
            appData = System.getenv("APPDATA");
            if (appData == null || appData.isBlank()) {
                appData = System.getProperty("user.home");
            }
            return appData + "\\Guicang\\data";
        }
        return System.getProperty("user.home") + "/.guicang/data";
    }

    private static volatile Connection connection;
    private static volatile boolean initialized = false;

    /**
     * 获取数据库连接（线程安全，只初始化一次）
     */
    public static synchronized Connection getConnection() {
        try {
            if (!initialized || connection == null || connection.isClosed()) {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
                connection = null;
                initialized = false;
                init();
            }
            return connection;
        } catch (Exception e) {
            throw new RuntimeException("[归藏] 获取数据库连接失败", e);
        }
    }

    private static void init() {
        try {
            Path dataPath = Paths.get(DATA_DIR).toAbsolutePath();
            if (!Files.exists(dataPath)) {
                Files.createDirectories(dataPath);
            }

            String url = "jdbc:sqlite:" + dataPath.resolve(DB_FILE).toString();
            System.out.println("[归藏] 数据库路径: " + url);

            connection = DriverManager.getConnection(url);
            connection.setAutoCommit(true);

            // 执行 DDL
            executeSchema(connection);
            initialized = true;

            System.out.println("[归藏] 数据库初始化完成");
        } catch (Exception e) {
            System.err.println("[归藏] 数据库初始化失败: " + e.getMessage());
            e.printStackTrace();
            try { if (connection != null) connection.close(); } catch (Exception ignored) {}
            connection = null;
            throw new RuntimeException("[归藏] 数据库初始化失败: " + e.getMessage(), e);
        }
    }

    private static void executeSchema(Connection conn) throws Exception {
        InputStream is = DatabaseConfig.class.getResourceAsStream(SCHEMA_SQL);
        if (is == null) {
            // 尝试不带前导 / 的路径
            is = DatabaseConfig.class.getResourceAsStream("db/schema.sql");
        }
        if (is == null) {
            System.err.println("[归藏] ❌ schema.sql 未找到，尝试的路径: " + SCHEMA_SQL);
            return;
        }
        System.out.println("[归藏] 找到 schema.sql，正在执行 DDL...");

        String content;
        try {
            content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            is.close();
        }

        // 按 ; 分割并逐条执行（SQLite 原生支持 -- 注释，无需预处理）
        String[] statements = content.split(";");
        int executed = 0;
        try (Statement stmt = conn.createStatement()) {
            for (String sql : statements) {
                sql = sql.trim();
                if (sql.isEmpty()) continue;
                // 跳过纯注释块（整段只有 -- 行）
                if (sql.lines().allMatch(line -> line.isBlank() || line.stripLeading().startsWith("--"))) {
                    continue;
                }
                try {
                    stmt.execute(sql);
                    executed++;
                } catch (Exception e) {
                    String msg = e.getMessage();
                    if (msg != null && (msg.contains("already exists") || msg.contains("UNIQUE constraint") || msg.contains("duplicate column"))) {
                        continue;
                    }
                    // 截断长 SQL 用于日志
                    String preview = sql.replace('\n', ' ').replace('\r', ' ');
                    if (preview.length() > 100) preview = preview.substring(0, 100) + "...";
                    System.err.println("[归藏] DDL warning: " + msg);
                    System.err.println("[归藏]   SQL: " + preview);
                }
            }
        }
        System.out.println("[归藏] DDL 执行完成，共执行 " + executed + " 条语句");
    }

    /**
     * 关闭连接
     */
    public static synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[归藏] 数据库连接已关闭");
            }
        } catch (Exception e) {
            System.err.println("[归藏] 关闭数据库连接失败: " + e.getMessage());
        } finally {
            connection = null;
            initialized = false;
        }
    }
}
