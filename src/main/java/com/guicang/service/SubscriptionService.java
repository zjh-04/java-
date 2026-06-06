package com.guicang.service;

import com.guicang.model.Asset;
import com.guicang.repository.AssetRepository;
import com.guicang.util.DateUtil;

/**
 * 数字订阅服务 — 续费倒计时、暂停
 */
public class SubscriptionService {

    private final AssetRepository assetRepo = new AssetRepository();

    /** 获取剩余天数（距离 nextBillingDate） */
    public long getRemainingDays(Asset asset) {
        if (asset.getNextBillingDate() == null) return 0;
        long days = DateUtil.daysBetween(DateUtil.today(), asset.getNextBillingDate());
        return Math.max(days, 0);
    }

    /** 暂停订阅 → 归档 */
    public void pause(String assetId) {
        assetRepo.archive(assetId, true);
    }

    /** 获取周期天数（用于进度条百分比） */
    public int getBillingDays(Asset asset) {
        if (asset.getBillingCycle() == null) return 30;
        return switch (asset.getBillingCycle()) {
            case "MONTHLY" -> 30;
            case "QUARTERLY" -> 90;
            case "YEARLY" -> 365;
            default -> 30;
        };
    }
}
