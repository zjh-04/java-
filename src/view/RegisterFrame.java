package view;

import java.awt.*;
import javax.swing.*;
import service.UserService;
import util.ColorTheme;
import view.component.GradientPanel;
import view.component.ModernButton;
import view.component.ModernTextField;

/**
 * 注册界面 — 与登录风格统一。
 */
public class RegisterFrame extends JFrame {

    private ModernTextField tfUsername;
    private JPasswordField tfPassword, tfConfirm;
    private ModernTextField tfEmail;
    private JLabel lblMessage;

    public RegisterFrame(JFrame parent) {
        setTitle("创建账号 - 物品视角管理系统");
        setSize(480, 590);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(parent);
        setResizable(false);

        GradientPanel panel = new GradientPanel(ColorTheme.BG_DARK, ColorTheme.BG_SIDEBAR);
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(36, 50, 36, 50));
        setContentPane(panel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 标题
        JLabel lblTitle = new JLabel("创建新账号", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Microsoft YaHei", Font.BOLD, 24));
        lblTitle.setForeground(ColorTheme.ACCENT_PURPLE);
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 6, 0);
        panel.add(lblTitle, gbc);

        JLabel lblSub = new JLabel("开始管理你的物品", SwingConstants.CENTER);
        lblSub.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        lblSub.setForeground(ColorTheme.TEXT_MUTED);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 22, 0);
        panel.add(lblSub, gbc);

        // 表单
        gbc.insets = new Insets(4, 0, 2, 0);
        gbc.gridy = 2;
        panel.add(createLabel("用户名 (3-16位)"), gbc);
        tfUsername = new ModernTextField(20);
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 6, 0);
        panel.add(tfUsername, gbc);

        gbc.gridy = 4;
        gbc.insets = new Insets(4, 0, 2, 0);
        panel.add(createLabel("密码 (6-20位)"), gbc);
        tfPassword = createPwdField();
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 6, 0);
        panel.add(tfPassword, gbc);

        gbc.gridy = 6;
        gbc.insets = new Insets(4, 0, 2, 0);
        panel.add(createLabel("确认密码"), gbc);
        tfConfirm = createPwdField();
        gbc.gridy = 7;
        gbc.insets = new Insets(0, 0, 6, 0);
        panel.add(tfConfirm, gbc);

        gbc.gridy = 8;
        gbc.insets = new Insets(4, 0, 2, 0);
        panel.add(createLabel("邮箱 (可选)"), gbc);
        tfEmail = new ModernTextField(20);
        gbc.gridy = 9;
        gbc.insets = new Insets(0, 0, 4, 0);
        panel.add(tfEmail, gbc);

        // 消息
        lblMessage = new JLabel(" ", SwingConstants.CENTER);
        lblMessage.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        lblMessage.setForeground(ColorTheme.ACCENT_RED);
        gbc.gridy = 10;
        gbc.insets = new Insets(6, 0, 10, 0);
        panel.add(lblMessage, gbc);

        // 注册按钮
        ModernButton btnRegister = new ModernButton("注  册",
                ColorTheme.ACCENT_PURPLE, new Color(0x7B, 0x5E, 0xE0), new Color(0x06, 0x0B, 0x1E));
        btnRegister.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        gbc.gridy = 11;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(btnRegister, gbc);

        btnRegister.addActionListener(e -> register());

        // 返回登录
        JPanel backRow = new JPanel(new FlowLayout(SwingConstants.CENTER, 4, 0));
        backRow.setOpaque(false);
        JLabel lblHint = new JLabel("已有账号？");
        lblHint.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
        lblHint.setForeground(ColorTheme.TEXT_MUTED);
        backRow.add(lblHint);
        ModernButton btnBack = new ModernButton("返回登录",
                ColorTheme.ACCENT_CYAN, new Color(0x00, 0xB8, 0xD4), ColorTheme.TEXT_PRIMARY);
        btnBack.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
        btnBack.addActionListener(e -> dispose());
        backRow.add(btnBack);
        gbc.gridy = 12;
        gbc.insets = new Insets(12, 0, 0, 0);
        panel.add(backRow, gbc);
    }

    private JPasswordField createPwdField() {
        JPasswordField pf = new JPasswordField(20);
        pf.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        pf.setForeground(ColorTheme.TEXT_PRIMARY);
        pf.setBackground(ColorTheme.BG_INPUT);
        pf.setCaretColor(ColorTheme.ACCENT_CYAN);
        pf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorTheme.BORDER, 1),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        return pf;
    }

    private JLabel createLabel(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        l.setForeground(ColorTheme.TEXT_SECONDARY);
        return l;
    }

    private void register() {
        String user = tfUsername.getText().trim();
        String pass = new String(tfPassword.getPassword());
        String confirm = new String(tfConfirm.getPassword());
        String email = tfEmail.getText().trim();

        if (!pass.equals(confirm)) {
            lblMessage.setText("两次密码不一致");
            return;
        }
        UserService.RegisterResult r = UserService.register(user, pass, email);
        if (r.isSuccess()) {
            JOptionPane.showMessageDialog(this, r.getMessage(), "注册成功", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            lblMessage.setText(r.getMessage());
        }
    }
}
