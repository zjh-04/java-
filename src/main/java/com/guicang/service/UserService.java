package com.guicang.service;

import com.guicang.model.User;
import com.guicang.repository.UserRepository;
import com.guicang.util.DateUtil;
import com.guicang.util.Validator;

/**
 * 用户认证与信息管理服务
 */
public class UserService {

    private final UserRepository userRepo = new UserRepository();
    private User currentUser;

    // ========== 认证 ==========

    public LoginResult login(String username, String rawPassword) {
        if (!Validator.isNotEmpty(username)) return LoginResult.fail("请输入用户名");
        if (!Validator.isNotEmpty(rawPassword)) return LoginResult.fail("请输入密码");

        User user = userRepo.findByUsername(username);
        if (user == null) return LoginResult.fail("用户名或密码错误");
        if (!userRepo.verifyPassword(user, rawPassword)) return LoginResult.fail("用户名或密码错误");

        currentUser = user;
        return LoginResult.success(user);
    }

    public RegisterResult register(String username, String password, String nickname) {
        if (!Validator.isValidUsername(username))
            return RegisterResult.fail("用户名格式不正确（3-16位字母、数字、中文、下划线）");
        if (!Validator.isValidPassword(password))
            return RegisterResult.fail("密码需6-20位，且包含字母和数字");
        if (userRepo.existsByUsername(username))
            return RegisterResult.fail("用户名已存在，请更换");

        User user = new User(username, password, nickname, DateUtil.today());
        userRepo.insert(user);
        return RegisterResult.success();
    }

    public void changePassword(String oldPassword, String newPassword) {
        if (currentUser == null) throw new RuntimeException("未登录");
        if (!userRepo.verifyPassword(currentUser, oldPassword)) throw new RuntimeException("旧密码不正确");
        userRepo.updatePassword(currentUser.getId(), newPassword);
    }

    public void updateProfile(String nickname, String email, Integer avatarIndex, String theme) {
        if (currentUser == null) throw new RuntimeException("未登录");
        userRepo.updateProfile(currentUser.getId(), nickname, email, avatarIndex, theme);
        // 刷新本地缓存
        currentUser = userRepo.findById(currentUser.getId());
    }

    public void logout() { currentUser = null; }
    public User getCurrentUser() { return currentUser; }
    public boolean isLoggedIn() { return currentUser != null; }

    // ========== 结果封装 ==========

    public static class LoginResult {
        private final boolean success;
        private final String message;
        private final User user;
        private LoginResult(boolean s, String m, User u) { success = s; message = m; user = u; }
        static LoginResult success(User u) { return new LoginResult(true, "登录成功", u); }
        static LoginResult fail(String m) { return new LoginResult(false, m, null); }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public User getUser() { return user; }
    }

    public static class RegisterResult {
        private final boolean success;
        private final String message;
        private RegisterResult(boolean s, String m) { success = s; message = m; }
        static RegisterResult success() { return new RegisterResult(true, "注册成功"); }
        static RegisterResult fail(String m) { return new RegisterResult(false, m); }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}
