package com.guicang.service;

import com.guicang.config.DatabaseConfig;
import com.guicang.model.Asset;
import com.guicang.model.UsageLog;
import com.guicang.repository.AssetRepository;
import com.guicang.repository.UsageLogRepository;
import com.guicang.util.DateUtil;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 次卡 / 储值卡服务 — 核销、充值、消费
 * 全部包裹 SQLite 事务
 */
public class StoredCardService {

    private final AssetRepository assetRepo = new AssetRepository();
    private final UsageLogRepository logRepo = new UsageLogRepository();

    /** 次卡核销（事务保护） */
    public PunchResult punch(String assetId) {
        Connection conn = DatabaseConfig.getConnection();
        try {
            conn.setAutoCommit(false);

            Asset item = assetRepo.findById(assetId);
            int remaining = item.getRemainingTimes() != null ? item.getRemainingTimes() : 0;
            if (remaining <= 0) return new PunchResult(false, "次数已用完", 0);

            remaining--;
            item.setRemainingTimes(remaining);
            assetRepo.update(item);
            logRepo.insert(new UsageLog(assetId, "PUNCH", 1, 0,
                    "核销 1 次，剩余 " + remaining + " 次", DateUtil.today()));

            conn.commit();
            return new PunchResult(true, remaining == 0 ? "卡券完全履约！" : "", remaining);
        } catch (Exception e) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("次卡核销失败", e);
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    /** 次卡充值（事务保护） */
    public Asset topupTimes(String assetId, double amount, int times) {
        Connection conn = DatabaseConfig.getConnection();
        try {
            conn.setAutoCommit(false);

            Asset item = assetRepo.findById(assetId);
            item.setTotalTimes((item.getTotalTimes() != null ? item.getTotalTimes() : 0) + times);
            item.setRemainingTimes((item.getRemainingTimes() != null ? item.getRemainingTimes() : 0) + times);
            item.setTotalTopup((item.getTotalTopup() != null ? item.getTotalTopup() : 0) + amount);
            assetRepo.update(item);
            logRepo.insert(new UsageLog(assetId, "TOPUP", amount, amount / times,
                    "充值 ¥" + amount + " 新增 " + times + " 次", DateUtil.today()));

            conn.commit();
            return item;
        } catch (Exception e) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("次卡充值失败", e);
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    /** 储值卡消费（事务保护） */
    public SpendResult spend(String assetId, double amount) {
        Connection conn = DatabaseConfig.getConnection();
        try {
            conn.setAutoCommit(false);

            Asset item = assetRepo.findById(assetId);
            double balance = item.getCardBalance() != null ? item.getCardBalance() : 0;
            if (balance < amount) return new SpendResult(false, "余额不足", balance);

            balance -= amount;
            double totalSpent = (item.getTotalSpent() != null ? item.getTotalSpent() : 0) + amount;
            item.setCardBalance(balance);
            item.setTotalSpent(totalSpent);
            assetRepo.update(item);
            logRepo.insert(new UsageLog(assetId, "SPEND", amount, 0,
                    "消费 ¥" + amount + "，余额 ¥" + balance, DateUtil.today()));

            conn.commit();
            return new SpendResult(true, balance == 0 ? "余额用尽" : "", balance);
        } catch (Exception e) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("储值卡消费失败", e);
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    /** 储值卡充值（事务保护）— 充值后 totalTopup 重设为当前余额，进度条补满 */
    public Asset topupAmount(String assetId, double amount) {
        Connection conn = DatabaseConfig.getConnection();
        try {
            conn.setAutoCommit(false);

            Asset item = assetRepo.findById(assetId);
            double newBalance = (item.getCardBalance() != null ? item.getCardBalance() : 0) + amount;
            item.setCardBalance(newBalance);
            item.setTotalTopup(newBalance);  // 重置进度条上限 = 当前余额
            assetRepo.update(item);
            logRepo.insert(new UsageLog(assetId, "TOPUP", amount, 0,
                    "充值 ¥" + amount, DateUtil.today()));

            conn.commit();
            return item;
        } catch (Exception e) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("储值卡充值失败", e);
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    /** 按量计费充值（事务保护）— 充值后 totalCharged 重设为当前余额，进度条补满 */
    public Asset topupApi(String assetId, double amount) {
        Connection conn = DatabaseConfig.getConnection();
        try {
            conn.setAutoCommit(false);

            Asset item = assetRepo.findById(assetId);
            double newBalance = (item.getApiBalance() != null ? item.getApiBalance() : 0) + amount;
            item.setApiBalance(newBalance);
            item.setTotalCharged(newBalance);  // 重置进度条上限 = 当前余额
            assetRepo.update(item);
            logRepo.insert(new UsageLog(assetId, "TOPUP", amount, 0,
                    "API 充值 ¥" + amount, DateUtil.today()));

            conn.commit();
            return item;
        } catch (Exception e) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("按量计费充值失败", e);
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    /** 按量计费消耗（事务保护） */
    public SpendResult spendApi(String assetId, double amount) {
        Connection conn = DatabaseConfig.getConnection();
        try {
            conn.setAutoCommit(false);

            Asset item = assetRepo.findById(assetId);
            double balance = item.getApiBalance() != null ? item.getApiBalance() : 0;
            if (balance < amount) return new SpendResult(false, "API 余额不足", balance);

            balance -= amount;
            item.setApiBalance(balance);
            assetRepo.update(item);
            logRepo.insert(new UsageLog(assetId, "SPEND", amount, 0,
                    "API 调用消耗 ¥" + amount, DateUtil.today()));

            conn.commit();
            return new SpendResult(true, balance == 0 ? "余额用尽，请及时充值" : "", balance);
        } catch (Exception e) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("按量计费消耗失败", e);
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    public record PunchResult(boolean success, String message, int remaining) {}
    public record SpendResult(boolean success, String message, double remainingBalance) {}
}
