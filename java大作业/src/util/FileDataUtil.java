package util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件数据持久化工具类
 * 使用文本文件存储数据，无需外部数据库依赖
 *
 * 文件格式说明：
 * - 每条记录占一行
 * - 字段之间使用 \\| 分隔（管道符，防止与内容冲突）
 * - 每类数据存储在独立的 .dat 文件中
 */
public class FileDataUtil {

    /** 数据文件存储目录 */
    private static final String STR_DATA_DIR = "./data";

    /** 字段分隔符 */
    public static final String SEPARATOR = "\\|";

    static {
        ensureDataDir();
    }

    /**
     * 确保数据目录存在
     */
    private static void ensureDataDir() {
        Path pthDir = Paths.get(STR_DATA_DIR);
        if (!Files.exists(pthDir)) {
            try {
                Files.createDirectories(pthDir);
            } catch (IOException e) {
                System.err.println("[错误] 无法创建数据目录: " + e.getMessage());
            }
        }
    }

    /**
     * 获取数据文件完整路径
     */
    public static String getFilePath(String strFileName) {
        return STR_DATA_DIR + File.separator + strFileName;
    }

    /**
     * 读取文件所有行（跳过空行和 # 开头的注释行）
     * @param strFileName 文件名
     * @return 行数据列表
     */
    public static List<String> readLines(String strFileName) {
        List<String> listLines = new ArrayList<>();
        Path pthFile = Paths.get(getFilePath(strFileName));
        if (!Files.exists(pthFile)) {
            return listLines;
        }
        try {
            listLines = Files.readAllLines(pthFile, StandardCharsets.UTF_8);
            // 过滤空行和注释行
            listLines.removeIf(line -> line.trim().isEmpty() || line.trim().startsWith("#"));
        } catch (IOException e) {
            System.err.println("[错误] 读取文件失败: " + strFileName + " - " + e.getMessage());
        }
        return listLines;
    }

    /**
     * 追加一行数据到文件
     * @param strFileName 文件名
     * @param strLine 数据行
     */
    public static void appendLine(String strFileName, String strLine) {
        Path pthFile = Paths.get(getFilePath(strFileName));
        try {
            List<String> listLine = new ArrayList<>();
            listLine.add(strLine);
            Files.write(pthFile, listLine, StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("[错误] 写入文件失败: " + strFileName + " - " + e.getMessage());
        }
    }

    /**
     * 覆盖写入文件（全量）
     * @param strFileName 文件名
     * @param listLines 数据行列表
     */
    public static void writeLines(String strFileName, List<String> listLines) {
        Path pthFile = Paths.get(getFilePath(strFileName));
        try {
            Files.write(pthFile, listLines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[错误] 写入文件失败: " + strFileName + " - " + e.getMessage());
        }
    }

    /**
     * 删除文件
     */
    public static void deleteFile(String strFileName) {
        try {
            Files.deleteIfExists(Paths.get(getFilePath(strFileName)));
        } catch (IOException e) {
            System.err.println("[错误] 删除文件失败: " + strFileName + " - " + e.getMessage());
        }
    }

    /**
     * 文件是否存在
     */
    public static boolean fileExists(String strFileName) {
        return Files.exists(Paths.get(getFilePath(strFileName)));
    }

    /**
     * 从一行数据中解析字段
     * @param strLine 数据行
     * @return 字段数组
     */
    public static String[] parseFields(String strLine) {
        if (strLine == null || strLine.trim().isEmpty()) {
            return new String[0];
        }
        return strLine.split(SEPARATOR, -1);
    }

    /**
     * 将字段数组拼接为一行数据
     * @param arrFields 字段数组
     * @return 拼接后的数据行
     */
    public static String joinFields(String... arrFields) {
        return String.join("|", arrFields);
    }

    /**
     * 对象深度拷贝（通过序列化）
     */
    @SuppressWarnings("unchecked")
    public static <T> T deepCopy(T objSource) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(objSource);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (T) ois.readObject();
        } catch (Exception e) {
            return null;
        }
    }
}
