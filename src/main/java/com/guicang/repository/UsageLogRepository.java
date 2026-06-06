package com.guicang.repository;

import com.guicang.config.DatabaseConfig;
import com.guicang.model.UsageLog;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsageLogRepository {

    public List<UsageLog> findByAsset(String assetId) {
        String sql = "SELECT * FROM usage_logs WHERE asset_id = ? ORDER BY created_at DESC";
        List<UsageLog> list = new ArrayList<>();
        Connection c = DatabaseConfig.getConnection();
        try (
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, assetId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询流水失败", e);
        }
        return list;
    }

    public void insert(UsageLog log) {
        String sql = "INSERT INTO usage_logs (asset_id, action_type, quantity, unit_price, notes, created_at) VALUES (?,?,?,?,?,?)";
        Connection c = DatabaseConfig.getConnection();
        try (
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, log.getAssetId());
            ps.setString(2, log.getActionType());
            ps.setDouble(3, log.getQuantity());
            ps.setDouble(4, log.getUnitPrice());
            ps.setString(5, log.getNotes());
            ps.setString(6, log.getCreatedAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("写流水失败", e);
        }
    }

    private UsageLog mapRow(ResultSet rs) throws SQLException {
        UsageLog l = new UsageLog();
        l.setId(rs.getInt("id"));
        l.setAssetId(rs.getString("asset_id"));
        l.setActionType(rs.getString("action_type"));
        l.setQuantity(rs.getDouble("quantity"));
        l.setUnitPrice(rs.getDouble("unit_price"));
        l.setNotes(rs.getString("notes"));
        l.setCreatedAt(rs.getString("created_at"));
        return l;
    }
}
