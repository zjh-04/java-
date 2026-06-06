package com.guicang.repository;

import com.guicang.config.DatabaseConfig;
import com.guicang.model.Asset;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 资产数据访问 — 统一处理 11 种 asset_type
 */
public class AssetRepository {

    // ========== 查询 ==========

    public List<Asset> findByUser(int userId) {
        return findByUser(userId, null, false);
    }

    public List<Asset> findByUserAndType(int userId, String assetType) {
        return findByUser(userId, assetType, false);
    }

    public List<Asset> findArchived(int userId) {
        String sql = "SELECT * FROM assets WHERE user_id = ? AND is_archived = 1 ORDER BY updated_at DESC";
        List<Asset> list = new ArrayList<>();
        Connection c = DatabaseConfig.getConnection();
        try (
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询归档资产失败", e);
        }
        return list;
    }

    private List<Asset> findByUser(int userId, String assetType, boolean archived) {
        StringBuilder sql = new StringBuilder("SELECT * FROM assets WHERE user_id = ? AND is_archived = 0");
        if (assetType != null) sql.append(" AND asset_type = ?");
        sql.append(" ORDER BY created_at DESC");

        List<Asset> list = new ArrayList<>();
        Connection c = DatabaseConfig.getConnection();
        try (
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            ps.setInt(1, userId);
            if (assetType != null) ps.setString(2, assetType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询资产失败", e);
        }
        return list;
    }

    public Asset findById(String id) {
        String sql = "SELECT * FROM assets WHERE id = ?";
        Connection c = DatabaseConfig.getConnection();
        try (
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询资产失败", e);
        }
        return null;
    }

    // ========== 写入 ==========

    public Asset insert(Asset asset) {
        if (asset.getId() == null) asset.setId(UUID.randomUUID().toString().substring(0, 8));
        String sql = buildInsertSQL();
        Connection c = DatabaseConfig.getConnection();
        try (
             PreparedStatement ps = c.prepareStatement(sql)) {
            bindInsertParams(ps, asset);
            ps.executeUpdate();
            return asset;
        } catch (SQLException e) {
            throw new RuntimeException("创建资产失败", e);
        }
    }

    public void update(Asset asset) {
        String sql = buildUpdateSQL();
        Connection c = DatabaseConfig.getConnection();
        try (
             PreparedStatement ps = c.prepareStatement(sql)) {
            bindUpdateParams(ps, asset);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("更新资产失败", e);
        }
    }

    public void delete(String id) {
        Connection c = DatabaseConfig.getConnection();
        try {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM usage_logs WHERE asset_id = ?")) {
                ps.setString(1, id); ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM purchase_batches WHERE asset_id = ?")) {
                ps.setString(1, id); ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM assets WHERE id = ?")) {
                ps.setString(1, id); ps.executeUpdate();
            }
            c.commit();
        } catch (SQLException e) {
            try { c.rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("删除资产失败", e);
        } finally {
            try { c.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    public void archive(String id, boolean archived) {
        String sql = "UPDATE assets SET is_archived = ?, updated_at = datetime('now') WHERE id = ?";
        Connection c = DatabaseConfig.getConnection();
        try (
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, archived ? 1 : 0);
            ps.setString(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("归档操作失败", e);
        }
    }

    // ========== SQL 构建 ==========

    private String buildInsertSQL() {
        return "INSERT INTO assets (id, user_id, asset_type, name, icon, category, purchase_price, purchase_date, notes, " +
               "usage_count, expected_lifespan_years, current_stock, safety_stock, collect_status, " +
               "billing_cycle, monthly_cost, next_billing_date, api_balance, total_charged, " +
               "remaining_times, total_times, total_topup, card_balance, total_spent, created_at) " +
               "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    }

    private void bindInsertParams(PreparedStatement ps, Asset a) throws SQLException {
        int i = 1;
        ps.setString(i++, a.getId()); ps.setInt(i++, a.getUserId());
        ps.setString(i++, a.getAssetType()); ps.setString(i++, a.getName());
        ps.setString(i++, a.getIcon()); ps.setString(i++, a.getCategory());
        ps.setDouble(i++, a.getPurchasePrice()); ps.setString(i++, a.getPurchaseDate());
        ps.setString(i++, a.getNotes());
        setIntOrNull(ps, i++, a.getUsageCount()); setIntOrNull(ps, i++, a.getExpectedLifespanYears());
        setDoubleOrNull(ps, i++, a.getCurrentStock()); setDoubleOrNull(ps, i++, a.getSafetyStock());
        ps.setString(i++, a.getCollectStatus());
        ps.setString(i++, a.getBillingCycle()); setDoubleOrNull(ps, i++, a.getMonthlyCost());
        ps.setString(i++, a.getNextBillingDate()); setDoubleOrNull(ps, i++, a.getApiBalance());
        setDoubleOrNull(ps, i++, a.getTotalCharged()); setIntOrNull(ps, i++, a.getRemainingTimes());
        setIntOrNull(ps, i++, a.getTotalTimes()); setDoubleOrNull(ps, i++, a.getTotalTopup());
        setDoubleOrNull(ps, i++, a.getCardBalance()); setDoubleOrNull(ps, i++, a.getTotalSpent());
        ps.setString(i, a.getCreatedAt());
    }

    private String buildUpdateSQL() {
        return "UPDATE assets SET name=?, icon=?, category=?, purchase_price=?, purchase_date=?, notes=?, " +
               "usage_count=?, expected_lifespan_years=?, current_stock=?, safety_stock=?, collect_status=?, " +
               "billing_cycle=?, monthly_cost=?, next_billing_date=?, api_balance=?, total_charged=?, " +
               "remaining_times=?, total_times=?, total_topup=?, card_balance=?, total_spent=?, " +
               "is_archived=?, updated_at=datetime('now') WHERE id=?";
    }

    private void bindUpdateParams(PreparedStatement ps, Asset a) throws SQLException {
        int i = 1;
        ps.setString(i++, a.getName()); ps.setString(i++, a.getIcon());
        ps.setString(i++, a.getCategory()); ps.setDouble(i++, a.getPurchasePrice());
        ps.setString(i++, a.getPurchaseDate()); ps.setString(i++, a.getNotes());
        setIntOrNull(ps, i++, a.getUsageCount()); setIntOrNull(ps, i++, a.getExpectedLifespanYears());
        setDoubleOrNull(ps, i++, a.getCurrentStock()); setDoubleOrNull(ps, i++, a.getSafetyStock());
        ps.setString(i++, a.getCollectStatus());
        ps.setString(i++, a.getBillingCycle()); setDoubleOrNull(ps, i++, a.getMonthlyCost());
        ps.setString(i++, a.getNextBillingDate()); setDoubleOrNull(ps, i++, a.getApiBalance());
        setDoubleOrNull(ps, i++, a.getTotalCharged()); setIntOrNull(ps, i++, a.getRemainingTimes());
        setIntOrNull(ps, i++, a.getTotalTimes()); setDoubleOrNull(ps, i++, a.getTotalTopup());
        setDoubleOrNull(ps, i++, a.getCardBalance()); setDoubleOrNull(ps, i++, a.getTotalSpent());
        ps.setInt(i++, a.getIsArchived());
        ps.setString(i, a.getId());
    }

    // ========== 辅助 ==========

    private Asset mapRow(ResultSet rs) throws SQLException {
        Asset a = new Asset();
        a.setId(rs.getString("id")); a.setUserId(rs.getInt("user_id"));
        a.setAssetType(rs.getString("asset_type")); a.setName(rs.getString("name"));
        a.setIcon(rs.getString("icon")); a.setCategory(rs.getString("category"));
        a.setPurchasePrice(rs.getDouble("purchase_price")); a.setPurchaseDate(rs.getString("purchase_date"));
        a.setNotes(rs.getString("notes"));

        a.setUsageCount(getIntOrNull(rs, "usage_count"));
        a.setExpectedLifespanYears(getIntOrNull(rs, "expected_lifespan_years"));
        a.setCurrentStock(getDoubleOrNull(rs, "current_stock"));
        a.setSafetyStock(getDoubleOrNull(rs, "safety_stock"));
        a.setCollectStatus(rs.getString("collect_status"));
        a.setBillingCycle(rs.getString("billing_cycle"));
        a.setMonthlyCost(getDoubleOrNull(rs, "monthly_cost"));
        a.setNextBillingDate(rs.getString("next_billing_date"));
        a.setApiBalance(getDoubleOrNull(rs, "api_balance"));
        a.setTotalCharged(getDoubleOrNull(rs, "total_charged"));
        a.setRemainingTimes(getIntOrNull(rs, "remaining_times"));
        a.setTotalTimes(getIntOrNull(rs, "total_times"));
        a.setTotalTopup(getDoubleOrNull(rs, "total_topup"));
        a.setCardBalance(getDoubleOrNull(rs, "card_balance"));
        a.setTotalSpent(getDoubleOrNull(rs, "total_spent"));

        a.setIsArchived(rs.getInt("is_archived"));
        a.setCreatedAt(rs.getString("created_at"));
        a.setUpdatedAt(rs.getString("updated_at"));
        return a;
    }

    private void setIntOrNull(PreparedStatement ps, int idx, Integer val) throws SQLException {
        if (val != null) ps.setInt(idx, val); else ps.setNull(idx, Types.INTEGER);
    }
    private void setDoubleOrNull(PreparedStatement ps, int idx, Double val) throws SQLException {
        if (val != null) ps.setDouble(idx, val); else ps.setNull(idx, Types.REAL);
    }
    private Integer getIntOrNull(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col); return rs.wasNull() ? null : v;
    }
    private Double getDoubleOrNull(ResultSet rs, String col) throws SQLException {
        double v = rs.getDouble(col); return rs.wasNull() ? null : v;
    }
}
