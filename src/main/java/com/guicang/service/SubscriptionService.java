package com.guicang.service;

import com.guicang.model.Asset;
import com.guicang.model.UsageLog;
import com.guicang.repository.AssetRepository;
import com.guicang.repository.UsageLogRepository;
import com.guicang.util.DateUtil;

/**
 * 数字订阅服务 — 续费倒计时、暂停、自动续费
 */
public class SubscriptionService {

    private final AssetRepository assetRepo = new AssetRepository();
    private final UsageLogRepository logRepo = new UsageLogRepository();

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

    /**
     * 自动续费：若今天已到/超过 nextBillingDate，则从原扣费日推算新日期，写库+流水。
     * synchronized + DB 重读防止并发请求重复续费。
     * @return 本次续费天数，0 表示无需续费
     */
    public synchronized int autoRenewIfDue(Asset asset) {
        if (asset.getNextBillingDate() == null) return 0;
        if (asset.getBillingCycle() == null) return 0;
        if ("SUBSCRIPTION_METERED".equals(asset.getAssetType())) return 0;
        if ("SUBSCRIPTION_LIFETIME".equals(asset.getAssetType())) return 0;

        // 重读 DB 最新状态，防止并发时拿到过期对象
        Asset fresh = assetRepo.findById(asset.getId());
        if (fresh == null) return 0;

        long remaining = getRemainingDays(fresh);
        if (remaining > 0) {
            // DB 已被其他线程续费过 → 同步回传入对象
            asset.setNextBillingDate(fresh.getNextBillingDate());
            return 0;
        }

        int cycleDays = getBillingDays(fresh);
        String oldDate = fresh.getNextBillingDate();

        // 从原扣费日往后推，跳过已错过的周期，直到落在未来
        String newDate = DateUtil.addDays(oldDate, cycleDays);
        while (DateUtil.daysBetween(DateUtil.today(), newDate) <= 0) {
            newDate = DateUtil.addDays(newDate, cycleDays);
        }

        asset.setNextBillingDate(newDate);
        assetRepo.update(asset);

        logRepo.insert(new UsageLog(asset.getId(), "RENEW", 0, 0,
                "自动续费「" + asset.getName() + "」→ " + newDate + "（+" + cycleDays + "天，原扣费日 " + oldDate + "）",
                DateUtil.today()));

        System.out.println("[归藏] 自动续费: " + asset.getName() + " " + oldDate + " → " + newDate);
        return cycleDays;
    }
}
