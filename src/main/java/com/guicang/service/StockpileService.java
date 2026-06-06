package com.guicang.service;

import com.guicang.config.DatabaseConfig;
import com.guicang.model.Asset;
import com.guicang.model.PurchaseBatch;
import com.guicang.model.UsageLog;
import com.guicang.repository.AssetRepository;
import com.guicang.repository.PurchaseBatchRepository;
import com.guicang.repository.UsageLogRepository;
import com.guicang.util.DateUtil;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 囤货模式服务 — 库存管理 + 多批次均价 + 消耗记录 + 比价 + 预警
 * 所有库存变动包裹 SQLite 事务
 */
public class StockpileService {

    private final AssetRepository assetRepo = new AssetRepository();
    private final PurchaseBatchRepository batchRepo = new PurchaseBatchRepository();
    private final UsageLogRepository logRepo = new UsageLogRepository();

    /** 创建囤货物品 + 首批入库（事务保护） */
    public Asset createItem(int userId, String name, String category, double totalPrice,
                            int initialQty, double safetyStock, String date, String notes) {
        Connection conn = DatabaseConfig.getConnection();
        try {
            conn.setAutoCommit(false);

            Asset item = new Asset();
            item.setUserId(userId);
            item.setAssetType("STOCKPILE");
            item.setName(name);
            item.setCategory(category);
            item.setPurchasePrice(totalPrice);
            item.setPurchaseDate(date);
            item.setCurrentStock((double) initialQty);
            item.setSafetyStock(safetyStock);
            item.setNotes(notes);
            item.setCreatedAt(date);
            assetRepo.insert(item);

            PurchaseBatch batch = new PurchaseBatch(item.getId(), initialQty, totalPrice, date);
            batchRepo.insert(batch);

            conn.commit();
            return item;
        } catch (Exception e) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("创建囤货物品失败", e);
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    /** 追加入库（事务保护） */
    public PurchaseBatch restock(String assetId, int qty, double totalPrice) {
        Connection conn = DatabaseConfig.getConnection();
        try {
            conn.setAutoCommit(false);

            String today = DateUtil.today();
            PurchaseBatch batch = new PurchaseBatch(assetId, qty, totalPrice, today);
            batchRepo.insert(batch);
            recalcStock(assetId);
            logRepo.insert(new UsageLog(assetId, "RESTOCK", qty, totalPrice / qty,
                    "快捷补货入库 " + qty + " 件", today));

            conn.commit();
            return batch;
        } catch (Exception e) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("补货入库失败", e);
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    /** 记录消耗（事务保护） */
    public UsageLog consume(String assetId, int qty, String notes) {
        Connection conn = DatabaseConfig.getConnection();
        try {
            conn.setAutoCommit(false);

            Asset item = assetRepo.findById(assetId);
            double currentStock = item.getCurrentStock() != null ? item.getCurrentStock() : 0;
            if (currentStock < qty) throw new RuntimeException("库存不足");

            String today = DateUtil.today();
            UsageLog log = new UsageLog(assetId, "CONSUME", qty, getAveragePrice(assetId), notes, today);
            logRepo.insert(log);
            recalcStock(assetId);

            conn.commit();
            return log;
        } catch (Exception e) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("消耗记录失败", e);
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    /** 重新计算库存 */
    void recalcStock(String assetId) {
        Asset item = assetRepo.findById(assetId);
        if (item == null) return;

        var batches = batchRepo.findByAsset(assetId);
        // 如果没有采购批次（旧数据/新创建但批次未写入），跳过重算，保留当前库存
        if (batches.isEmpty()) return;

        int totalBought = batches.stream().mapToInt(PurchaseBatch::getQuantity).sum();
        int totalConsumed = logRepo.findByAsset(assetId).stream()
                .filter(l -> "CONSUME".equals(l.getActionType()))
                .mapToInt(l -> (int) l.getQuantity()).sum();

        item.setCurrentStock((double) (totalBought - totalConsumed));
        assetRepo.update(item);
    }

    /** 综合均价 = 所有批次总金额 / 所有批次总数量 */
    public double getAveragePrice(String assetId) {
        var batches = batchRepo.findByAsset(assetId);
        int totalQty = batches.stream().mapToInt(PurchaseBatch::getQuantity).sum();
        double totalPrice = batches.stream().mapToDouble(PurchaseBatch::getTotalPrice).sum();
        return totalQty > 0 ? totalPrice / totalQty : 0;
    }

    /** 将本次补货单价与历史价格区间（史低/史高）对比，文案对齐 HTML 示例卡片的语气 */
    public CompareResult comparePrice(String assetId, double externalUnitPrice) {
        var batches = batchRepo.findByAsset(assetId);
        if (batches.isEmpty())
            return new CompareResult("neutral", "暂无历史购买记录，无法比较");

        double minUnit = batches.stream().mapToDouble(b -> b.getTotalPrice() / b.getQuantity()).min().orElse(0);
        double maxUnit = batches.stream().mapToDouble(b -> b.getTotalPrice() / b.getQuantity()).max().orElse(0);

        // 只有一个价格点：本次与唯一历史价比较
        if (Math.abs(maxUnit - minUnit) < 0.001) {
            if (externalUnitPrice < minUnit) {
                return new CompareResult("good",
                        String.format("<i class=\"fa-solid fa-tags\"></i> <b>比价反馈：</b>本次单价 ¥%.2f，比历史唯一价格 ¥%.2f 还低，发现新史低！",
                                externalUnitPrice, minUnit));
            } else if (externalUnitPrice > minUnit) {
                return new CompareResult("bad",
                        String.format("<i class=\"fa-solid fa-lightbulb\"></i> <b>比价反馈：</b>本次单价 ¥%.2f，比历史价格 ¥%.2f 高，刷新了史高。虽然多花了一点点，但它带来的快乐和便利是无法用金钱衡量的，早买早享受呀。",
                                externalUnitPrice, minUnit));
            } else {
                return new CompareResult("neutral",
                        String.format("<i class=\"fa-solid fa-tags\"></i> <b>比价反馈：</b>本次单价 ¥%.2f，与历史价格持平，稳稳的幸福。", externalUnitPrice));
            }
        }

        // 多批次：与史低/史高区间比较
        if (externalUnitPrice <= minUnit) {
            return new CompareResult("good",
                    String.format("<i class=\"fa-solid fa-tags\"></i> <b>比价反馈：</b>很幸运，这次是在它最温柔的阶段（史低 ¥%.2f）遇到了它，省下的钱可以多喝一杯拿铁啦。", minUnit));
        } else if (externalUnitPrice >= maxUnit) {
            return new CompareResult("bad",
                    String.format("<i class=\"fa-solid fa-lightbulb\"></i> <b>比价反馈：</b>本次单价 ¥%.2f 触及史高 ¥%.2f。虽然这次多花了一点点，但它带来的快乐和便利是无法用金钱衡量的，早买早享受呀。", externalUnitPrice, maxUnit));
        } else {
            double pct = (externalUnitPrice - minUnit) / (maxUnit - minUnit) * 100;
            return new CompareResult("neutral",
                    String.format("<i class=\"fa-solid fa-lightbulb\"></i> <b>比价反馈：</b>本次单价 ¥%.2f，处于历史价格区间 [¥%.2f – ¥%.2f] 的 %.0f%% 位置，属于平稳波动，补货完毕。", externalUnitPrice, minUnit, maxUnit, pct));
        }
    }

    public record CompareResult(String verdict, String message) {}
}
