package com.guicang.repository;

import com.guicang.config.DatabaseConfig;
import com.guicang.model.PurchaseBatch;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PurchaseBatchRepository {

    public List<PurchaseBatch> findByAsset(String assetId) {
        String sql = "SELECT * FROM purchase_batches WHERE asset_id = ? ORDER BY batch_date ASC";
        List<PurchaseBatch> list = new ArrayList<>();
        Connection c = DatabaseConfig.getConnection();
        try (
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, assetId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询批次失败", e);
        }
        return list;
    }

    public void insert(PurchaseBatch batch) {
        String sql = "INSERT INTO purchase_batches (id, asset_id, quantity, total_price, batch_date) VALUES (?,?,?,?,?)";
        Connection c = DatabaseConfig.getConnection();
        try (
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, batch.getId());
            ps.setString(2, batch.getAssetId());
            ps.setInt(3, batch.getQuantity());
            ps.setDouble(4, batch.getTotalPrice());
            ps.setString(5, batch.getBatchDate());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("写批次失败", e);
        }
    }

    public void deleteByAsset(String assetId) {
        String sql = "DELETE FROM purchase_batches WHERE asset_id = ?";
        Connection c = DatabaseConfig.getConnection();
        try (
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, assetId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("删除批次失败", e);
        }
    }

    private PurchaseBatch mapRow(ResultSet rs) throws SQLException {
        PurchaseBatch b = new PurchaseBatch();
        b.setId(rs.getString("id"));
        b.setAssetId(rs.getString("asset_id"));
        b.setQuantity(rs.getInt("quantity"));
        b.setTotalPrice(rs.getDouble("total_price"));
        b.setBatchDate(rs.getString("batch_date"));
        return b;
    }
}
