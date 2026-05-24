package service;

import dao.UserDao;
import model.User;
import util.DateUtil;
import util.Validator;

/**
 * 用户服务层 — 精简版，仅负责注册与登录。
 */
public class UserService {

    public static LoginResult login(String username, String password) {
        if (!Validator.isNotEmpty(username)) return LoginResult.fail("请输入用户名");
        if (!Validator.isNotEmpty(password)) return LoginResult.fail("请输入密码");

        User user = UserDao.login(username, password);
        if (user == null) return LoginResult.fail("用户名或密码错误");
        return LoginResult.success(user);
    }

    public static RegisterResult register(String username, String password, String email) {
        if (!Validator.isValidUsername(username))
            return RegisterResult.fail("用户名格式不正确（3-16位字母、数字、中文、下划线）");
        if (!Validator.isValidPassword(password))
            return RegisterResult.fail("密码需6-20位，且包含字母和数字");
        if (!Validator.isValidEmail(email))
            return RegisterResult.fail("邮箱格式不正确");
        if (UserDao.existsByUsername(username))
            return RegisterResult.fail("用户名已存在，请更换");

        User user = new User(username, password, email, DateUtil.getCurrentDate());
        boolean ok = UserDao.register(user);
        return ok ? RegisterResult.success() : RegisterResult.fail("注册失败，请重试");
    }

    /* ========== 结果封装 ========== */

    public static class LoginResult {
        private final boolean success;
        private final String message;
        private final User user;

        private LoginResult(boolean s, String m, User u) { this.success = s; this.message = m; this.user = u; }
        public static LoginResult success(User u) { return new LoginResult(true, "登录成功", u); }
        public static LoginResult fail(String m) { return new LoginResult(false, m, null); }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public User getUser() { return user; }
    }

    public static class RegisterResult {
        private final boolean success;
        private final String message;

        private RegisterResult(boolean s, String m) { this.success = s; this.message = m; }
        public static RegisterResult success() { return new RegisterResult(true, "注册成功！请登录"); }
        public static RegisterResult fail(String m) { return new RegisterResult(false, m); }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}
