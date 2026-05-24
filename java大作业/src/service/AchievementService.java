package service;

import dao.AchievementDao;
import dao.ItemDao;
import java.util.*;
import model.*;
import model.Item.ItemMode;

/**
 * 成就服务 — 18个成就，基于物品数据检查进度。
 */
public class AchievementService {

    public static List<String> checkAll(String username) {
        Set<String> before = AchievementDao.getCompletedIds(username);

        List<Item> all = ItemDao.findItemsByUser(username);
        int total = all.size();
        double totalValue = all.stream().mapToDouble(Item::getPurchasePrice).sum();
        long cats = all.stream().map(Item::getCategory).filter(Objects::nonNull).distinct().count();

        long longTerm = all.stream().filter(it -> it.getMode() == ItemMode.LONG_TERM).count();
        long ltWithYears = all.stream()
                .filter(it -> it.getMode() == ItemMode.LONG_TERM && it.getExpectedLifespanYears() != null)
                .count();
        long stockpile = all.stream().filter(it -> it.getMode() == ItemMode.STOCKPILE).count();
        long stockOk = all.stream()
                .filter(it -> it.getMode() == ItemMode.STOCKPILE
                        && StockpileService.getCurrentStock(username, it.getId()) >
                           (it.getSafetyStock() != null ? it.getSafetyStock() : 0))
                .count();
        long records = all.stream().filter(it -> it.getMode() == ItemMode.RECORD).count();
        long dailyLow = all.stream()
                .filter(it -> it.getMode() == ItemMode.LONG_TERM
                        && LongTermService.getDailyCost(it) < 1.0)
                .count();

        AchievementDao.updateProgress(username, "A01", total >= 1 ? 1 : 0);
        AchievementDao.updateProgress(username, "A02", total / 10.0);
        AchievementDao.updateProgress(username, "A03", total / 30.0);
        AchievementDao.updateProgress(username, "A04", total / 100.0);

        AchievementDao.updateProgress(username, "A05", longTerm / 3.0);
        AchievementDao.updateProgress(username, "A06", ltWithYears / 5.0);
        AchievementDao.updateProgress(username, "A07", longTerm / 10.0);

        AchievementDao.updateProgress(username, "A08", stockpile / 3.0);
        AchievementDao.updateProgress(username, "A09", stockOk / 3.0);
        AchievementDao.updateProgress(username, "A10", stockpile / 10.0);

        AchievementDao.updateProgress(username, "A11", records / 10.0);
        AchievementDao.updateProgress(username, "A12", records / 30.0);

        AchievementDao.updateProgress(username, "A13", totalValue / 1000.0);
        AchievementDao.updateProgress(username, "A14", totalValue / 10000.0);
        AchievementDao.updateProgress(username, "A15", totalValue / 50000.0);

        AchievementDao.updateProgress(username, "A16", cats / 5.0);
        AchievementDao.updateProgress(username, "A17", cats / 10.0);

        AchievementDao.updateProgress(username, "A18", dailyLow / 3.0);

        AchievementDao.refresh(username);
        Set<String> after = AchievementDao.getCompletedIds(username);

        List<String> newly = new ArrayList<>();
        for (String id : after) {
            if (!before.contains(id)) newly.add(id);
        }
        return newly;
    }

    public static String getUnlockMessage(List<String> ids) {
        if (ids.isEmpty()) return null;
        StringBuilder sb = new StringBuilder("🎉 成就解锁!\n\n");
        for (String id : ids) {
            Achievement a = AchievementDao.getDefinition(id);
            if (a != null) sb.append(a.getIcon()).append(" ").append(a.getName())
                    .append(" — ").append(a.getDescription()).append("\n");
        }
        return sb.toString();
    }

    public static List<AchievementProgress> getOverview(String username) {
        List<Achievement> defs = AchievementDao.getAllDefinitions();
        List<UserAchievement> progs = AchievementDao.getUserAchievements(username);
        List<AchievementProgress> result = new ArrayList<>();
        for (Achievement d : defs) {
            UserAchievement p = progs.stream()
                    .filter(ua -> ua.getAchievementId().equals(d.getId())).findFirst().orElse(null);
            double prog = p != null ? p.getProgress() : 0;
            boolean done = p != null && p.isCompleted();
            result.add(new AchievementProgress(d, prog, done,
                    p != null ? p.getCompletedDate() : null));
        }
        return result;
    }

    public static int[] getStats(String username) {
        List<Achievement> defs = AchievementDao.getAllDefinitions();
        int total = defs.size();
        int done = AchievementDao.getCompletedCount(username);
        return new int[]{done, total};
    }

    public static class AchievementProgress {
        private final Achievement achievement;
        private final double progress;
        private final boolean completed;
        private final String completedDate;

        public AchievementProgress(Achievement a, double p, boolean c, String d) {
            this.achievement = a; this.progress = p; this.completed = c; this.completedDate = d;
        }
        public Achievement getAchievement() { return achievement; }
        public double getProgress() { return progress; }
        public boolean isCompleted() { return completed; }
        public String getCompletedDate() { return completedDate; }
        public int getPercent() { return (int) Math.min(100, progress * 100); }

        public String getHint() {
            if (completed) return "✓ 已完成";
            int pct = getPercent();
            if (pct == 0) return "暂无进度";
            return "进度 " + pct + "%";
        }
    }
}
