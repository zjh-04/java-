package com.guicang.service;

import com.guicang.model.*;
import com.guicang.repository.AchievementRepository;
import com.guicang.repository.AssetRepository;

import java.util.*;

/**
 * 成就系统服务 — 36 成就检查
 */
public class AchievementService {

    private final AchievementRepository achRepo = new AchievementRepository();
    private final AssetRepository assetRepo = new AssetRepository();
    private final LongTermService ltService = new LongTermService();
    private final StockpileService spService = new StockpileService();

    /** 检查全部成就，返回新解锁清单 */
    public List<Achievement> checkAll(int userId) {
        Set<String> before = achRepo.getCompletedIds(userId);
        List<Asset> all = assetRepo.findByUser(userId);

        int total = all.size();
        double totalValue = all.stream().mapToDouble(Asset::getPurchasePrice).sum();
        long cats = all.stream().map(Asset::getCategory).filter(Objects::nonNull).distinct().count();

        long longTermPerUse = countType(all, "LONG_TERM_PER_USE");
        long longTermPerDay = countType(all, "LONG_TERM_PER_DAY");
        long stockpile = countType(all, "STOCKPILE");
        long collectible = countType(all, "COLLECTIBLE");
        long subscriptions = countType(all, "SUBSCRIPTION_MONTHLY") + countType(all, "SUBSCRIPTION_QUARTERLY")
                + countType(all, "SUBSCRIPTION_YEARLY") + countType(all, "SUBSCRIPTION_METERED")
                + countType(all, "SUBSCRIPTION_LIFETIME");
        long storedCards = countType(all, "STORED_TIME_CARD") + countType(all, "STORED_AMOUNT_CARD");
        long archived = all.stream().filter(a -> a.getIsArchived() == 1).count();

        // 打卡相关
        int totalCheckIns = all.stream()
                .filter(a -> a.getUsageCount() != null)
                .mapToInt(Asset::getUsageCount).sum();

        // 物品收集
        update(userId, "a01", total >= 1 ? 1 : 0);
        update(userId, "a02", total / 5.0);
        update(userId, "a03", total / 10.0);
        update(userId, "a04", total / 20.0);

        // 细水长流
        update(userId, "a05", totalCheckIns >= 1 ? 1 : 0);
        update(userId, "a06", totalCheckIns / 10.0);
        update(userId, "a07", totalCheckIns / 30.0);
        update(userId, "a08", totalCheckIns / 100.0);

        // 物尽其用: ratio ≤ 0.10
        boolean hasUltra = all.stream()
                .filter(a -> a.getUsageCount() != null && a.getUsageCount() > 0)
                .anyMatch(a -> (a.getPurchasePrice() / a.getUsageCount()) / a.getPurchasePrice() <= 0.10);
        update(userId, "a09", hasUltra ? 1 : 0);

        // 日积月累: 有物品持有超过365天
        boolean has365 = all.stream().anyMatch(a -> ltService.getDaysHeld(a) >= 365);
        update(userId, "a10", has365 ? 1 : 0);

        // 储备幸福
        update(userId, "a11", stockpile >= 1 ? 1 : 0);
        update(userId, "a12", hasRestock(userId) ? 1 : 0);
        update(userId, "a13", hasLowStock(all) ? 1 : 0);
        update(userId, "a14", hasEmptyStock(all) ? 1 : 0);
        update(userId, "a15", stockpile / 3.0);
        update(userId, "a16", 1); // 简化：假设触发过
        update(userId, "a17", 3); // 简化

        // 收藏纪念
        update(userId, "a18", collectible >= 1 ? 1 : 0);
        long maxCollectDays = all.stream().filter(a -> "COLLECTIBLE".equals(a.getAssetType()))
                .mapToLong(ltService::getDaysHeld).max().orElse(0);
        update(userId, "a19", maxCollectDays / 30.0);
        update(userId, "a20", maxCollectDays / 365.0);
        update(userId, "a21", collectible / 3.0);

        // 数字订阅
        update(userId, "a22", subscriptions >= 1 ? 1 : 0);
        update(userId, "a23", subscriptions / 3.0);
        update(userId, "a24", archived > 0 ? 1 : 0);
        update(userId, "a25", countType(all, "SUBSCRIPTION_LIFETIME") >= 1 ? 1 : 0);
        update(userId, "a26", storedCards / 2.0);
        long typeCount = (subscriptions > 0 ? 1 : 0) + (storedCards > 0 ? 1 : 0) +
                (countType(all, "SUBSCRIPTION_LIFETIME") > 0 ? 1 : 0) +
                (countType(all, "SUBSCRIPTION_METERED") > 0 ? 1 : 0);
        update(userId, "a27", typeCount / 4.0);

        // 资产总览
        update(userId, "a28", totalValue / 10000.0);
        update(userId, "a29", totalValue / 50000.0);
        update(userId, "a30", (longTermPerUse + longTermPerDay > 0 && subscriptions > 0 && collectible > 0 ? 3 : 0) / 3.0);
        update(userId, "a31", archived > 0 ? 1 : 0);
        update(userId, "a32", cats / 5.0);

        // 里程碑
        boolean costUnder1 = all.stream()
                .filter(a -> a.getUsageCount() != null && a.getUsageCount() > 0)
                .anyMatch(a -> a.getPurchasePrice() / a.getUsageCount() < 1);
        boolean costUnder10 = all.stream()
                .filter(a -> a.getUsageCount() != null && a.getUsageCount() > 0)
                .filter(a -> a.getPurchasePrice() / a.getUsageCount() < 10)
                .count() >= 3;
        update(userId, "a33", costUnder1 ? 1 : 0);
        update(userId, "a34", costUnder10 ? 1 : 0);
        update(userId, "a35", 1); // 简化
        int unlockedCount = achRepo.getCompletedCount(userId);
        update(userId, "a36", unlockedCount / 25.0);

        // 检测新解锁
        Set<String> after = achRepo.getCompletedIds(userId);
        List<Achievement> newly = new ArrayList<>();
        List<Achievement> defs = achRepo.getAllDefinitions();
        for (String id : after) {
            if (!before.contains(id)) {
                defs.stream().filter(d -> d.getId().equals(id)).findFirst().ifPresent(newly::add);
            }
        }
        return newly;
    }

    private void update(int userId, String id, double progress) {
        achRepo.updateProgress(userId, id, Math.min(progress, 1.0));
    }

    private long countType(List<Asset> all, String type) {
        return all.stream().filter(a -> type.equals(a.getAssetType())).count();
    }

    private boolean hasRestock(int userId) {
        return true; // 简化
    }

    private boolean hasLowStock(List<Asset> all) {
        return all.stream().filter(a -> "STOCKPILE".equals(a.getAssetType()))
                .anyMatch(a -> {
                    double s = a.getCurrentStock() != null ? a.getCurrentStock() : 0;
                    double safe = a.getSafetyStock() != null ? a.getSafetyStock() : 0;
                    return s <= safe && s > 0;
                });
    }

    private boolean hasEmptyStock(List<Asset> all) {
        return all.stream().filter(a -> "STOCKPILE".equals(a.getAssetType()))
                .anyMatch(a -> {
                    double s = a.getCurrentStock() != null ? a.getCurrentStock() : 0;
                    return s <= 0;
                });
    }

    /** 带进度的成就概览 */
    public List<Map<String, Object>> getOverview(int userId) {
        List<Achievement> defs = achRepo.getAllDefinitions();
        List<UserAchievement> progs = achRepo.getUserAchievements(userId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Achievement d : defs) {
            UserAchievement p = progs.stream()
                    .filter(ua -> ua.getAchievementId().equals(d.getId())).findFirst().orElse(null);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.getId());
            m.put("name", d.getName());
            m.put("description", d.getDescription());
            m.put("icon", d.getIcon());
            m.put("category", d.getCategory());
            m.put("goal", d.getGoalValue());
            m.put("level", d.getLevel());
            m.put("current", p != null ? p.getProgress() : 0);
            m.put("completed", p != null && p.isCompleted());
            m.put("completedDate", p != null ? p.getCompletedDate() : null);
            result.add(m);
        }
        return result;
    }
}
