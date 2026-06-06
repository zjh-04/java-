package com.guicang.service;

import com.guicang.model.Asset;
import com.guicang.model.UsageLog;
import com.guicang.repository.AssetRepository;
import com.guicang.repository.UsageLogRepository;
import com.guicang.util.DateUtil;

/**
 * 收藏状态服务 — 三态循环 + 陪伴追踪（不计均摊）
 */
public class CollectibleService {

    private final AssetRepository assetRepo = new AssetRepository();
    private final UsageLogRepository logRepo = new UsageLogRepository();

    public static final String[] STATUSES = {"日常使用中", "完美珍藏中", "计划转手中"};

    /** 切换收藏状态（三态循环） */
    public String cycleStatus(String assetId) {
        Asset item = assetRepo.findById(assetId);
        String current = item.getCollectStatus();
        String next;
        if (current == null || current.isEmpty() || current.equals(STATUSES[2])) {
            next = STATUSES[0];
        } else if (current.equals(STATUSES[0])) {
            next = STATUSES[1];
        } else {
            next = STATUSES[2];
        }
        item.setCollectStatus(next);
        assetRepo.update(item);

        logRepo.insert(new UsageLog(assetId, "STATUS_CHANGE", 0, 0,
                "收藏状态变更为：" + next, DateUtil.today()));
        return next;
    }

    /** 获取陪伴天数 */
    public long getDaysHeld(Asset asset) {
        long days = DateUtil.daysBetween(asset.getPurchaseDate(), DateUtil.today());
        return Math.max(days, 1);
    }
}
