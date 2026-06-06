package com.guicang.model;

/**
 * 用户实体
 */
public class User {
    private int    id;
    private String username;       // 登录账号 (不可修改)
    private String passwordHash;   // BCrypt 哈希
    private String nickname;       // 显示昵称 (可修改)
    private String email;
    private int    avatarIndex;
    private String theme;          // 'dark' | 'light'
    private String createdAt;

    public User() {}

    public User(String username, String passwordHash, String nickname, String createdAt) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.createdAt = createdAt;
    }

    // ========== Getter / Setter ==========
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getAvatarIndex() { return avatarIndex; }
    public void setAvatarIndex(int avatarIndex) { this.avatarIndex = avatarIndex; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
