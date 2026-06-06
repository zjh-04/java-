package view;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import service.AchievementService;
import service.AchievementService.AchievementProgress;
import util.ColorTheme;

/**
 * 成就面板 — 展示18个成就的完成与未完成状态。
 */
public class AchievementPanel extends JPanel implements Refreshable {

    private final String username;
    private JPanel cardsPanel;
    private JLabel lblSummary;
    private JLabel lblLevelBreakdown;

    public AchievementPanel(String username) {
        this.username = username;
        setLayout(new BorderLayout(10, 10));
        setBackground(ColorTheme.BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel title = new JLabel("成就系统");
        title.setFont(new Font("Microsoft YaHei", Font.BOLD, 22));
        title.setForeground(ColorTheme.ACCENT_GOLD);
        headerPanel.add(title, BorderLayout.WEST);

        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 0));
        rightHeader.setOpaque(false);

        lblLevelBreakdown = new JLabel();
        lblLevelBreakdown.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        lblLevelBreakdown.setForeground(ColorTheme.TEXT_SECONDARY);
        rightHeader.add(lblLevelBreakdown);

        lblSummary = new JLabel();
        lblSummary.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        lblSummary.setForeground(ColorTheme.ACCENT_GOLD);
        rightHeader.add(lblSummary);

        headerPanel.add(rightHeader, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        cardsPanel = new JPanel();
        cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
        cardsPanel.setOpaque(false);
        JScrollPane scroll = new JScrollPane(cardsPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(ColorTheme.BG_DARK);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        cardsPanel.removeAll();
        List<AchievementProgress> list = AchievementService.getOverview(username);

        long completed = list.stream().filter(AchievementProgress::isCompleted).count();
        long gold = list.stream().filter(a -> a.isCompleted() && a.getAchievement().getLevel() == 3).count();
        long silver = list.stream().filter(a -> a.isCompleted() && a.getAchievement().getLevel() == 2).count();
        long bronze = list.stream().filter(a -> a.isCompleted() && a.getAchievement().getLevel() == 1).count();

        lblSummary.setText(String.format("%d / %d 已解锁", completed, list.size()));
        lblLevelBreakdown.setText(String.format("🥇%d  🥈%d  🥉%d", gold, silver, bronze));

        // 已完成排前面，然后按等级
        list.sort((a, b) -> {
            if (a.isCompleted() != b.isCompleted()) return Boolean.compare(!a.isCompleted(), !b.isCompleted());
            return Integer.compare(b.getAchievement().getLevel(), a.getAchievement().getLevel());
        });

        for (AchievementProgress ap : list) {
            cardsPanel.add(createCard(ap));
            cardsPanel.add(Box.createVerticalStrut(6));
        }
        cardsPanel.revalidate();
        cardsPanel.repaint();
    }

    private JPanel createCard(AchievementProgress ap) {
        boolean done = ap.isCompleted();

        Color borderColor = done ? ColorTheme.ACCENT_GREEN : ColorTheme.BORDER;
        Color bgColor = done ? new Color(0x14, 0x28, 0x1C) : ColorTheme.BG_CARD;

        JPanel card = new JPanel(new BorderLayout(14, 0));
        card.setBackground(bgColor);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));

        // 左侧
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JPanel topLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        topLine.setOpaque(false);

        JLabel icon = new JLabel(ap.getAchievement().getIcon());
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        topLine.add(icon);

        JLabel name = new JLabel(ap.getAchievement().getName());
        name.setFont(new Font("Microsoft YaHei", Font.BOLD, 15));
        name.setForeground(done ? ColorTheme.ACCENT_GREEN : ColorTheme.TEXT_PRIMARY);
        topLine.add(name);

        // 等级徽章
        String lv = ap.getAchievement().getLevelName();
        JLabel level = new JLabel(lv.equals("金") ? "🥇" : lv.equals("银") ? "🥈" : "🥉");
        level.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        topLine.add(level);

        // 完成日期
        if (done && ap.getCompletedDate() != null) {
            JLabel doneDate = new JLabel("✓ " + ap.getCompletedDate());
            doneDate.setFont(new Font("Microsoft YaHei", Font.BOLD, 11));
            doneDate.setForeground(ColorTheme.ACCENT_GREEN);
            topLine.add(doneDate);
        }

        left.add(topLine);

        // 描述 + 进度提示
        JPanel descLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        descLine.setOpaque(false);

        JLabel desc = new JLabel(ap.getAchievement().getDescription());
        desc.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        desc.setForeground(ColorTheme.TEXT_MUTED);
        descLine.add(desc);

        if (!done) {
            JLabel hint = new JLabel("— " + ap.getHint());
            hint.setFont(new Font("Microsoft YaHei", Font.BOLD, 11));
            hint.setForeground(done ? ColorTheme.ACCENT_GREEN : ColorTheme.TEXT_MUTED);
            descLine.add(hint);
        }

        left.add(descLine);

        // 右侧：进度条
        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.setPreferredSize(new Dimension(160, 32));

        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(done ? 100 : ap.getPercent());
        bar.setStringPainted(true);
        bar.setString(done ? "完成!" : ap.getPercent() + "%");
        bar.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        bar.setForeground(done ? ColorTheme.ACCENT_GREEN : ColorTheme.ACCENT_GOLD);
        bar.setBackground(ColorTheme.BG_INPUT);
        bar.setBorder(BorderFactory.createEmptyBorder());
        right.add(bar, BorderLayout.CENTER);

        card.add(left, BorderLayout.CENTER);
        card.add(right, BorderLayout.EAST);
        return card;
    }
}
