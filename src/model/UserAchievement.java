package model;

import java.io.Serializable;

/**
 * 用户成就进度。
 */
public class UserAchievement implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;
    private String achievementId;
    private double progress;
    private boolean completed;
    private String completedDate;

    public UserAchievement() {}

    public UserAchievement(String userId, String achievementId) {
        this.userId = userId;
        this.achievementId = achievementId;
        this.progress = 0;
        this.completed = false;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getAchievementId() { return achievementId; }
    public void setAchievementId(String achievementId) { this.achievementId = achievementId; }
    public double getProgress() { return progress; }
    public void setProgress(double progress) { this.progress = progress; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public String getCompletedDate() { return completedDate; }
    public void setCompletedDate(String completedDate) { this.completedDate = completedDate; }
}
