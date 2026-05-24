package model;

import java.io.Serializable;

/**
 * 用户实体 — 精简版，仅保留认证相关字段。
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private String username;
    private String password;
    private String email;
    private String registerDate;

    public User() {}

    public User(String username, String password, String email, String registerDate) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.registerDate = registerDate;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRegisterDate() { return registerDate; }
    public void setRegisterDate(String registerDate) { this.registerDate = registerDate; }

    @Override
    public String toString() {
        return username;
    }
}
