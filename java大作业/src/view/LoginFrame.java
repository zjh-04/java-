package view;

import java.awt.*;
import javax.swing.*;
import service.UserService;
import util.ColorTheme;
import view.component.GradientPanel;
import view.component.ModernButton;
import view.component.ModernTextField;

/**
 * 登录界面 — 大气欢迎风格。
 */
public class LoginFrame extends JFrame {

    private ModernTextField tfUsername;
    private JPasswordField tfPassword;
    private JLabel lblMessage;

    public LoginFrame() {
        setTitle("物品视角管理系统");
        setSize(480, 560);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        GradientPanel panel = new GradientPanel(ColorTheme.BG_DARK, ColorTheme.BG_SIDEBAR);
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(40, 50, 40, 50));
        setContentPane(panel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 图标
        JLabel lblIcon = new JLabel("◆", SwingConstants.CENTER);
        lblIcon.setFont(new Font("Segoe UI", Font.PLAIN, 48));
        lblIcon.setForeground(ColorTheme.ACCENT_CYAN);
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 10, 0);
        panel.add(lblIcon, gbc);

        // 欢迎标题
        JLabel lblWelcome = new JLabel("欢迎回来", SwingConstants.CENTER);
        lblWelcome.setFont(new Font("Microsoft YaHei", Font.BOLD, 26));
        lblWelcome.setForeground(ColorTheme.TEXT_PRIMARY);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 4, 0);
        panel.add(lblWelcome, gbc);

        // 副标题
        JLabel lblSub = new JLabel("记录每一件值得留下的物品", SwingConstants.CENTER);
        lblSub.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        lblSub.setForeground(ColorTheme.TEXT_MUTED);
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 28, 0);
        panel.add(lblSub, gbc);

        // 用户名标签
        gbc.insets = new Insets(5, 0, 3, 0);
        gbc.gridy = 3;
        panel.add(createLabel("用户名"), gbc);
        tfUsername = new ModernTextField(20);
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 8, 0);
        panel.add(tfUsername, gbc);

        // 密码标签
        gbc.gridy = 5;
        gbc.insets = new Insets(5, 0, 3, 0);
        panel.add(createLabel("密码"), gbc);
        tfPassword = new JPasswordField(20);
        tfPassword.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        tfPassword.setForeground(ColorTheme.TEXT_PRIMARY);
        tfPassword.setBackground(ColorTheme.BG_INPUT);
        tfPassword.setCaretColor(ColorTheme.ACCENT_CYAN);
        tfPassword.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorTheme.BORDER, 1),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        gbc.gridy = 6;
        gbc.insets = new Insets(0, 0, 4, 0);
        panel.add(tfPassword, gbc);

        // 消息
        lblMessage = new JLabel(" ", SwingConstants.CENTER);
        lblMessage.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        lblMessage.setForeground(ColorTheme.ACCENT_RED);
        gbc.gridy = 7;
        gbc.insets = new Insets(6, 0, 10, 0);
        panel.add(lblMessage, gbc);

        // 登录按钮
        ModernButton btnLogin = new ModernButton("登  录",
                ColorTheme.ACCENT_CYAN, new Color(0x00, 0xB8, 0xD4), new Color(0x06, 0x0B, 0x1E));
        btnLogin.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        gbc.gridy = 8;
        gbc.insets = new Insets(0, 0, 12, 0);
        panel.add(btnLogin, gbc);

        // 注册文字
        JPanel regRow = new JPanel(new FlowLayout(SwingConstants.CENTER, 4, 0));
        regRow.setOpaque(false);
        JLabel lblHint = new JLabel("还没有账号？");
        lblHint.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
        lblHint.setForeground(ColorTheme.TEXT_MUTED);
        regRow.add(lblHint);
        ModernButton btnRegister = new ModernButton("立即注册",
                ColorTheme.ACCENT_PURPLE, new Color(0x7B, 0x5E, 0xE0), ColorTheme.TEXT_PRIMARY);
        btnRegister.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
        regRow.add(btnRegister);
        gbc.gridy = 9;
        gbc.insets = new Insets(8, 0, 0, 0);
        panel.add(regRow, gbc);

        btnLogin.addActionListener(e -> login());
        btnRegister.addActionListener(e -> openRegister());
        tfPassword.addActionListener(e -> login());
        getRootPane().setDefaultButton(btnLogin);
    }

    private JLabel createLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
        l.setForeground(ColorTheme.TEXT_SECONDARY);
        return l;
    }

    private void login() {
        String user = tfUsername.getText().trim();
        String pass = new String(tfPassword.getPassword());
        UserService.LoginResult r = UserService.login(user, pass);
        if (r.isSuccess()) {
            dispose();
            new MainFrame(r.getUser().getUsername()).setVisible(true);
        } else {
            lblMessage.setText(r.getMessage());
        }
    }

    private void openRegister() {
        new RegisterFrame(this).setVisible(true);
    }
}
