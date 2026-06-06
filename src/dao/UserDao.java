package dao;

import java.util.ArrayList;
import java.util.List;
import model.User;
import util.FileDataUtil;

/**
 * 用户数据访问层 — 精简版，仅管理认证信息。
 * 文件: data/users.dat
 * 格式: username|password|email|registerDate
 */
public class UserDao {

    private static final String FILE = "users.dat";
    private static List<User> users = new ArrayList<>();
    private static boolean loaded = false;

    private static void ensureLoaded() {
        if (loaded) return;
        loadFromFile();
        loaded = true;
    }

    private static void loadFromFile() {
        users.clear();
        for (String line : FileDataUtil.readLines(FILE)) {
            String[] f = FileDataUtil.parseFields(line);
            if (f.length >= 4) {
                User u = new User();
                u.setUsername(f[0]);
                u.setPassword(f[1]);
                u.setEmail(f[2]);
                u.setRegisterDate(f[3]);
                users.add(u);
            }
        }
    }

    private static void saveToFile() {
        List<String> lines = new ArrayList<>();
        for (User u : users) {
            lines.add(FileDataUtil.joinFields(
                    u.getUsername(), u.getPassword(), u.getEmail(), u.getRegisterDate()
            ));
        }
        FileDataUtil.writeLines(FILE, lines);
    }

    public static User findByUsername(String username) {
        ensureLoaded();
        return users.stream().filter(u -> u.getUsername().equals(username)).findFirst().orElse(null);
    }

    public static boolean existsByUsername(String username) {
        return findByUsername(username) != null;
    }

    public static User login(String username, String password) {
        User u = findByUsername(username);
        return (u != null && u.getPassword().equals(password)) ? u : null;
    }

    public static boolean register(User user) {
        ensureLoaded();
        if (existsByUsername(user.getUsername())) return false;
        users.add(user);
        saveToFile();
        return true;
    }

    public static void refresh() {
        loaded = false;
        ensureLoaded();
    }
}
