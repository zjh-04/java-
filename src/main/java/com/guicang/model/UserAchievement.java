package com.guicang.model;

/**
 * 用户成就进度实体
 */
public class UserAchievement {
    private int     userId;
    private String  achievementId;
    private double  progress;
    private boolean completed;
    private String  completedDate;
    private boolean notified;

    public UserAchievement() {}

    public UserAchievement(int userId, String achievementId) {
        this.userId = userId;
        this.achievementId = achievementId;
        this.progress = 0;
        this.completed = false;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getAchievementId() { return achievementId; }
    public void setAchievementId(String achievementId) { this.achievementId = achievementId; }
    public double getProgress() { return progress; }
    public void setProgress(double progress) { this.progress = progress; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public String getCompletedDate() { return completedDate; }
    public void setCompletedDate(String completedDate) { this.completedDate = completedDate; }
    public boolean isNotified() { return notified; }
    public void setNotified(boolean notified) { this.notified = notified; }
}
