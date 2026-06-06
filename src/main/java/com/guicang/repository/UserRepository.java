package com.guicang.repository;

import com.guicang.config.DatabaseConfig;
import com.guicang.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;

/**
 * 用户数据访问
 */
public class UserRepository {

    // ========== 查询 ==========

    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        Connection c = DatabaseConfig.getConnection();
        try (
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            System.err.println("[归藏] 查询用户SQL异常: " + e.getMessage());
            throw new RuntimeException("查询用户失败", e);
        }
        return null;
    }

    public User findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        Connection c = DatabaseConfig.getConnection();
        try (
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询用户失败", e);
        }
        return null;
    }

    public boolean existsByUsername(String username) {
        return findByUsername(username) != null;
    }

    // ========== 写入 ==========

    /** 注册 — 返回带 ID 的用户 */
    public User insert(User user) {
        String sql = "INSERT INTO users (username, password_hash, nickname, email, created_at) VALUES (?, ?, ?, ?, ?)";
        Connection c = DatabaseConfig.getConnection();
        try (
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, BCrypt.hashpw(user.getPasswordHash(), BCrypt.gensalt()));
            ps.setString(3, user.getNickname());
            ps.setString(4, user.getEmail() != null ? user.getEmail() : "");
            ps.setString(5, user.getCreatedAt());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) user.setId(rs.getInt(1));
            }
            return user;
        } catch (SQLException e) {
            System.err.println("[归藏] 注册SQL异常: " + e.getMessage());
            throw new RuntimeException("注册用户失败", e);
        }
    }

    /** 验证密码 */
    public boolean verifyPassword(User user, String rawPassword) {
        return BCrypt.checkpw(rawPassword, user.getPasswordHash());
    }

    /** 修改密码 */
    public void updatePassword(int userId, String newPasswordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        Connection c = DatabaseConfig.getConnection();
        try (
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, BCrypt.hashpw(newPasswordHash, BCrypt.gensalt()));
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("修改密码失败", e);
        }
    }

    /** 更新个人资料 */
    public void updateProfile(int userId, String nickname, String email, Integer avatarIndex, String theme) {
        // 动态构建 UPDATE
        StringBuilder sb = new StringBuilder("UPDATE users SET ");
        boolean first = true;
        if (nickname != null) { sb.append("nickname = ?"); first = false; }
        if (email != null) { if (!first) sb.append(", "); sb.append("email = ?"); first = false; }
        if (avatarIndex != null) { if (!first) sb.append(", "); sb.append("avatar_index = ?"); first = false; }
        if (theme != null) { if (!first) sb.append(", "); sb.append("theme = ?"); }
        sb.append(" WHERE id = ?");

        Connection c = DatabaseConfig.getConnection();
        try (
             PreparedStatement ps = c.prepareStatement(sb.toString())) {
            int idx = 1;
            if (nickname != null) ps.setString(idx++, nickname);
            if (email != null) ps.setString(idx++, email);
            if (avatarIndex != null) ps.setInt(idx++, avatarIndex);
            if (theme != null) ps.setString(idx++, theme);
            ps.setInt(idx, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("更新资料失败", e);
        }
    }

    // ========== 映射 ==========

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setNickname(rs.getString("nickname"));
        u.setEmail(rs.getString("email"));
        u.setAvatarIndex(rs.getInt("avatar_index"));
        u.setTheme(rs.getString("theme"));
        u.setCreatedAt(rs.getString("created_at"));
        return u;
    }
}
