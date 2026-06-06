package com.guicang.model;

/**
 * 成就定义实体 — 36 条预置数据
 */
public class Achievement {
    private String id;
    private String name;
    private String description;
    private String icon;
    private String category;
    private String goalType;
    private double goalValue;
    private int    level;          // 1=铜 2=银 3=金

    public Achievement() {}

    public Achievement(String id, String name, String description, String icon, int level) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.level = level;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getGoalType() { return goalType; }
    public void setGoalType(String goalType) { this.goalType = goalType; }
    public double getGoalValue() { return goalValue; }
    public void setGoalValue(double goalValue) { this.goalValue = goalValue; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
}
