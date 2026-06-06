package dao;

import java.util.*;
import model.Achievement;
import model.UserAchievement;
import util.FileDataUtil;
import util.DateUtil;

/**
 * 成就数据访问层 — 18个精心设计的成就。
 * 文件: data/achievements.dat (定义)
 *       data/user_achievements_{username}.dat (进度)
 */
public class AchievementDao {

    private static final String DEF_FILE = "achievements.dat";
    private static final String PROG_PREFIX = "user_achievements_";
    private static final String PROG_SUFFIX = ".dat";

    private static List<Achievement> definitions;
    private static final Map<String, List<UserAchievement>> progressCache = new HashMap<>();

    /* ========== 定义 ========== */

    private static void ensureDefs() {
        if (definitions != null) return;
        if (FileDataUtil.fileExists(DEF_FILE)) {
            loadDefs();
        } else {
            createDefaults();
            saveDefs();
        }
    }

    private static void createDefaults() {
        definitions = new ArrayList<>();
        // ═══ 收集总数 ═══
        definitions.add(new Achievement("A01", "初次记录", "记录第一件物品", "📝", 1));
        definitions.add(new Achievement("A02", "物品收藏家", "累计记录10件物品", "📦", 2));
        definitions.add(new Achievement("A03", "物品博物馆", "累计记录30件物品", "🏛️", 3));
        definitions.add(new Achievement("A04", "百物之王", "累计记录100件物品", "👑", 3));

        // ═══ 长期主义 ═══
        definitions.add(new Achievement("A05", "长期主义者", "记录3件长期物品", "📅", 1));
        definitions.add(new Achievement("A06", "远见卓识", "5件长期物品且都设了预期年限", "🔭", 2));
        definitions.add(new Achievement("A07", "十年规划", "记录10件长期物品", "🗓️", 3));

        // ═══ 囤货模式 ═══
        definitions.add(new Achievement("A08", "囤货新手", "创建3个囤货物品", "🧻", 1));
        definitions.add(new Achievement("A09", "库存管理师", "3个囤货物料库存高于安全线", "📊", 2));
        definitions.add(new Achievement("A10", "囤货专家", "创建10个囤货物品", "📋", 3));

        // ═══ 纯记录 ═══
        definitions.add(new Achievement("A11", "记录达人", "记录10件纯记录物品", "🏷️", 2));
        definitions.add(new Achievement("A12", "万物留痕", "记录30件纯记录物品", "📚", 3));

        // ═══ 总价值 ═══
        definitions.add(new Achievement("A13", "千元起步", "所有物品总价值超1000元", "💰", 1));
        definitions.add(new Achievement("A14", "万元收藏", "所有物品总价值超10000元", "💎", 2));
        definitions.add(new Achievement("A15", "豪门珍藏", "所有物品总价值超50000元", "🏆", 3));

        // ═══ 分类 ═══
        definitions.add(new Achievement("A16", "分类有序", "使用5种不同分类", "🗂️", 1));
        definitions.add(new Achievement("A17", "极致收纳", "使用10种不同分类", "📑", 3));

        // ═══ 物超所值 ═══
        definitions.add(new Achievement("A18", "日省一分", "有3件物品日均成本低于1元", "🪙", 2));
    }

    private static void loadDefs() {
        definitions = new ArrayList<>();
        for (String line : FileDataUtil.readLines(DEF_FILE)) {
            String[] f = FileDataUtil.parseFields(line);
            if (f.length >= 5) {
                definitions.add(new Achievement(f[0], f[1], f[2], f[3], Integer.parseInt(f[4])));
            }
        }
    }

    private static void saveDefs() {
        List<String> lines = new ArrayList<>();
        for (Achievement a : definitions) {
            lines.add(FileDataUtil.joinFields(a.getId(), a.getName(), a.getDescription(),
                    a.getIcon(), String.valueOf(a.getLevel())));
        }
        FileDataUtil.writeLines(DEF_FILE, lines);
    }

    public static List<Achievement> getAllDefinitions() {
        ensureDefs();
        return new ArrayList<>(definitions);
    }

    public static Achievement getDefinition(String id) {
        ensureDefs();
        return definitions.stream().filter(a -> a.getId().equals(id)).findFirst().orElse(null);
    }

    /* ========== 用户进度 ========== */

    private static String pFile(String username) { return PROG_PREFIX + username + PROG_SUFFIX; }

    private static void ensureProg(String username) {
        if (progressCache.containsKey(username)) return;
        List<UserAchievement> list = new ArrayList<>();
        for (Achievement a : getAllDefinitions()) {
            list.add(new UserAchievement(username, a.getId()));
        }
        if (FileDataUtil.fileExists(pFile(username))) {
            for (String line : FileDataUtil.readLines(pFile(username))) {
                String[] f = FileDataUtil.parseFields(line);
                if (f.length >= 3) {
                    for (UserAchievement ua : list) {
                        if (ua.getAchievementId().equals(f[0])) {
                            ua.setProgress(Double.parseDouble(f[1]));
                            ua.setCompleted("1".equals(f[2]));
                            ua.setCompletedDate(f.length >= 4 && !f[3].isEmpty() ? f[3] : null);
                        }
                    }
                }
            }
        }
        progressCache.put(username, list);
    }

    private static void saveProg(String username) {
        List<String> lines = new ArrayList<>();
        for (UserAchievement ua : progressCache.getOrDefault(username, Collections.emptyList())) {
            lines.add(FileDataUtil.joinFields(
                    ua.getAchievementId(),
                    String.valueOf(ua.getProgress()),
                    ua.isCompleted() ? "1" : "0",
                    ua.getCompletedDate() != null ? ua.getCompletedDate() : ""));
        }
        FileDataUtil.writeLines(pFile(username), lines);
    }

    public static List<UserAchievement> getUserAchievements(String username) {
        ensureProg(username);
        return new ArrayList<>(progressCache.get(username));
    }

    public static void updateProgress(String username, String achId, double progress) {
        ensureProg(username);
        for (UserAchievement ua : progressCache.get(username)) {
            if (ua.getAchievementId().equals(achId)) {
                ua.setProgress(Math.max(ua.getProgress(), progress));
                Achievement def = getDefinition(achId);
                if (def != null && ua.getProgress() >= 1 && !ua.isCompleted()) {
                    ua.setCompleted(true);
                    ua.setCompletedDate(DateUtil.getCurrentDate());
                }
            }
        }
        saveProg(username);
    }

    public static int getCompletedCount(String username) {
        ensureProg(username);
        return (int) progressCache.get(username).stream().filter(UserAchievement::isCompleted).count();
    }

    public static Set<String> getCompletedIds(String username) {
        ensureProg(username);
        Set<String> ids = new HashSet<>();
        for (UserAchievement ua : progressCache.get(username)) {
            if (ua.isCompleted()) ids.add(ua.getAchievementId());
        }
        return ids;
    }

    public static void refresh(String username) {
        progressCache.remove(username);
    }
}
