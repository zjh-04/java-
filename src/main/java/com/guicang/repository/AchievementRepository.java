package com.guicang.repository;

import com.guicang.config.DatabaseConfig;
import com.guicang.model.Achievement;
import com.guicang.model.UserAchievement;

import java.sql.*;
import java.util.*;

public class AchievementRepository {

    public List<Achievement> getAllDefinitions() {
        String sql = "SELECT * FROM achievement_defs ORDER BY id";
        List<Achievement> list = new ArrayList<>();
        Connection c = DatabaseConfig.getConnection();
        try (
             Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Achievement a = new Achievement();
                a.setId(rs.getString("id"));
                a.setName(rs.getString("name"));
                a.setDescription(rs.getString("description"));
                a.setIcon(rs.getString("icon"));
                a.setCategory(rs.getString("category"));
                a.setGoalType(rs.getString("goal_type"));
                a.setGoalValue(rs.getDouble("goal_value"));
                a.setLevel(rs.getInt("level"));
                list.add(a);
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询成就定义失败", e);
        }
        return list;
    }

    public List<UserAchievement> getUserAchievements(int userId) {
        String sql = "SELECT * FROM user_achievements WHERE user_id = ?";
        List<UserAchievement> list = new ArrayList<>();
        Connection c = DatabaseConfig.getConnection();
        try (
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UserAchievement ua = new UserAchievement();
                    ua.setUserId(rs.getInt("user_id"));
                    ua.setAchievementId(rs.getString("achievement_id"));
                    ua.setProgress(rs.getDouble("progress"));
                    ua.setCompleted(rs.getInt("is_completed") == 1);
                    ua.setCompletedDate(rs.getString("completed_date"));
                    ua.setNotified(rs.getInt("is_notified") == 1);
                    list.add(ua);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询用户成就失败", e);
        }
        return list;
    }

    public void updateProgress(int userId, String achievementId, double progress) {
        String sql = "INSERT INTO user_achievements (user_id, achievement_id, progress, is_completed, completed_date, is_notified) " +
                     "VALUES (?, ?, ?, 0, ?, 0) " +
                     "ON CONFLICT(user_id, achievement_id) DO UPDATE SET " +
                     "progress = excluded.progress, " +
                     "is_completed = CASE WHEN excluded.progress >= 1 OR user_achievements.is_completed = 1 THEN 1 ELSE 0 END, " +
                     "completed_date = CASE WHEN excluded.progress >= 1 AND user_achievements.completed_date IS NULL " +
                     "  THEN excluded.completed_date ELSE user_achievements.completed_date END, " +
                     "is_notified = CASE WHEN excluded.progress >= 1 AND user_achievements.is_completed = 0 THEN 0 " +
                     "                   ELSE COALESCE(user_achievements.is_notified, 0) END";
        Connection c = DatabaseConfig.getConnection();
        try (
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, achievementId);
            ps.setDouble(3, progress);
            ps.setString(4, progress >= 1 ? java.time.LocalDate.now().toString() : null);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("更新成就进度失败", e);
        }
    }

    public Set<String> getCompletedIds(int userId) {
        Set<String> ids = new HashSet<>();
        for (UserAchievement ua : getUserAchievements(userId)) {
            if (ua.isCompleted()) ids.add(ua.getAchievementId());
        }
        return ids;
    }

    public int getCompletedCount(int userId) {
        return getUserAchievements(userId).stream()
                .filter(UserAchievement::isCompleted).mapToInt(x -> 1).sum();
    }

    /** 获取已完成但未播报的成就 */
    public List<UserAchievement> getUnnotifiedCompleted(int userId) {
        return getUserAchievements(userId).stream()
                .filter(ua -> ua.isCompleted() && !ua.isNotified())
                .collect(java.util.stream.Collectors.toList());
    }

    /** 标记成就为已播报 */
    public void markNotified(int userId, String achievementId) {
        String sql = "UPDATE user_achievements SET is_notified = 1 WHERE user_id = ? AND achievement_id = ?";
        Connection c = DatabaseConfig.getConnection();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, achievementId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("标记成就已播报失败", e);
        }
    }
}
