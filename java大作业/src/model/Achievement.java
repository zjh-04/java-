package model;

import java.io.Serializable;

/**
 * 成就定义 — 物品管理相关的成就。
 */
public class Achievement implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String description;
    private String icon;
    private int level;  // 1=铜 2=银 3=金

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
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public String getLevelName() {
        return switch (level) {
            case 1 -> "铜";
            case 2 -> "银";
            case 3 -> "金";
            default -> "";
        };
    }
}
