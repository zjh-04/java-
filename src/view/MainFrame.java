package view;

import java.awt.*;
import javax.swing.*;
import util.ColorTheme;
import view.component.ModernButton;

/**
 * 主窗口 — 标签页布局，四个面板分别对应三种模式 + 全部物品。
 */
public class MainFrame extends JFrame {

    private static final Color[] TAB_COLORS = {
        new Color(0x00, 0xE5, 0xFF), // 长期主义 — cyan
        new Color(0xFF, 0x91, 0x35), // 囤货模式 — orange
        new Color(0x9D, 0x7B, 0xFF), // 纯记录 — purple
        new Color(0x44, 0x8A, 0xFF), // 全部物品 — blue
        new Color(0xFF, 0xD6, 0x00), // 成就 — gold
        new Color(0x00, 0xE6, 0x76), // 洞察 — green
    };

    private final String username;
    private JTabbedPane tabbedPane;
    private LongTermPanel longTermPanel;
    private StockpilePanel stockpilePanel;
    private RecordPanel recordPanel;
    private AllItemsPanel allItemsPanel;
    private AchievementPanel achievementPanel;
    private AdvicePanel advicePanel;

    public MainFrame(String username) {
        this.username = username;
        setTitle("物品管理系统 - " + username);
        setSize(960, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(800, 600));

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(ColorTheme.BG_DARK);
        setContentPane(root);

        // 顶部栏
        root.add(createTopBar(), BorderLayout.NORTH);

        // 标签页
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(ColorTheme.FONT_BODY);
        tabbedPane.setBackground(ColorTheme.BG_DARK);
        tabbedPane.setForeground(ColorTheme.TEXT_PRIMARY);
        tabbedPane.setBorder(BorderFactory.createEmptyBorder());

        longTermPanel = new LongTermPanel(username);
        stockpilePanel = new StockpilePanel(username);
        recordPanel = new RecordPanel(username);
        allItemsPanel = new AllItemsPanel(username);
        achievementPanel = new AchievementPanel(username);
        advicePanel = new AdvicePanel(username);

        addStyledTab("长期主义", longTermPanel, 0);
        addStyledTab("囤货模式", stockpilePanel, 1);
        addStyledTab("纯记录", recordPanel, 2);
        addStyledTab("全部物品", allItemsPanel, 3);
        addStyledTab("成就", achievementPanel, 4);
        addStyledTab("洞察", advicePanel, 5);

        root.add(tabbedPane, BorderLayout.CENTER);

        // 切换标签页时刷新
        tabbedPane.addChangeListener(e -> {
            Component c = tabbedPane.getSelectedComponent();
            if (c instanceof Refreshable) ((Refreshable) c).refresh();
        });
    }

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(ColorTheme.BG_TOPBAR);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, ColorTheme.BORDER),
                BorderFactory.createEmptyBorder(14, 24, 14, 24)));

        JLabel lblTitle = new JLabel("物品视角管理系统");
        lblTitle.setFont(new Font("Microsoft YaHei", Font.BOLD, 22));
        lblTitle.setForeground(ColorTheme.TEXT_DARK);
        bar.add(lblTitle, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        right.setOpaque(false);

        JLabel lblUser = new JLabel("当前用户: " + username);
        lblUser.setFont(new Font("Microsoft YaHei", Font.BOLD, 15));
        lblUser.setForeground(ColorTheme.TEXT_DARK_SEC);
        right.add(lblUser);

        ModernButton btnLogout = new ModernButton("切换账号",
                new Color(0xE8, 0xE8, 0xF0), new Color(0xD0, 0xD0, 0xDC), ColorTheme.TEXT_DARK);
        btnLogout.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
        btnLogout.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });
        right.add(btnLogout);

        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void addStyledTab(String title, JComponent panel, int idx) {
        tabbedPane.addTab(null, panel);
        JLabel tabLabel = new JLabel(title);
        tabLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        tabLabel.setForeground(TAB_COLORS[idx]);
        tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, tabLabel);
    }
}

/** 面板刷新接口 */
interface Refreshable {
    void refresh();
}
