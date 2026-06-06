package com.guicang.service;

import com.guicang.model.Asset;
import com.guicang.model.UsageLog;
import com.guicang.repository.AssetRepository;
import com.guicang.repository.UsageLogRepository;
import com.guicang.util.DateUtil;

/**
 * 长期主义服务 — 陪伴打卡 + 日均成本计算 + 阈值状态判定
 */
public class LongTermService {

    private final AssetRepository assetRepo = new AssetRepository();
    private final UsageLogRepository logRepo = new UsageLogRepository();

    /** 获取持有天数 */
    public long getDaysHeld(Asset asset) {
        long days = DateUtil.daysBetween(asset.getPurchaseDate(), DateUtil.today());
        return Math.max(days, 1);
    }

    /** 实际日均成本 = purchasePrice / daysHeld */
    public double getDailyCost(Asset asset) {
        long days = getDaysHeld(asset);
        return asset.getPurchasePrice() / days;
    }

    /** 预期日均成本 = purchasePrice / (expectedYears * 365) */
    public double getProjectedDailyCost(Asset asset) {
        if (asset.getExpectedLifespanYears() == null || asset.getExpectedLifespanYears() <= 0) {
            return getDailyCost(asset);
        }
        return asset.getPurchasePrice() / (asset.getExpectedLifespanYears() * 365.0);
    }

    /** 按次打卡 — 递增 usageCount，计算 ratio，返回状态标识 */
    public CheckInResult checkIn(String assetId, int userId) {
        Asset asset = assetRepo.findById(assetId);
        if (asset == null) throw new RuntimeException("资产不存在");

        int usageCount = asset.getUsageCount() != null ? asset.getUsageCount() + 1 : 1;
        asset.setUsageCount(usageCount);
        assetRepo.update(asset);

        // 计算
        long daysHeld = getDaysHeld(asset);
        double costPerUse = usageCount > 0 ? asset.getPurchasePrice() / usageCount : asset.getPurchasePrice();
        double costRatio = asset.getPurchasePrice() > 0 ? costPerUse / asset.getPurchasePrice() : 1;

        // 状态判定
        String tagVariant = "";
        String messageKey = "coat_start";
        if (usageCount == 1) {
            messageKey = "coat_first";
        } else if (costRatio <= 0.10) {
            tagVariant = "success";
            messageKey = "coat_perfect";
        } else if (costRatio <= 0.30) {
            messageKey = "coat_great";
        } else if (costRatio <= 0.50) {
            messageKey = "coat_good";
        } else if (costRatio <= 0.70) {
            messageKey = "coat_warm";
        }

        // 写流水
        logRepo.insert(new UsageLog(assetId, "CHECK_IN", 1, costPerUse,
                "第 " + usageCount + " 次陪伴打卡，单次均摊 ¥" + String.format("%.2f", costPerUse), DateUtil.today()));

        // 返回结果
        asset.setUsageCount(usageCount);
        asset.setDaysHeld(daysHeld);
        asset.setDailyCost(costPerUse);
        asset.setCostRatio(costRatio);
        asset.setStatusTagVariant(tagVariant);
        asset.setStatusMessageKey(messageKey);

        return new CheckInResult(asset, costPerUse, costRatio, daysHeld, tagVariant, messageKey);
    }

    public static class CheckInResult {
        public final Asset asset;
        public final double costPerUse;
        public final double costRatio;
        public final long daysHeld;
        public final String tagVariant;
        public final String messageKey;
        public CheckInResult(Asset a, double cpu, double cr, long dh, String tv, String mk) {
            asset = a; costPerUse = cpu; costRatio = cr; daysHeld = dh; tagVariant = tv; messageKey = mk;
        }
    }
}
