package view;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import service.AdviceService;
import service.AdviceService.Insight;
import util.ColorTheme;
import view.component.ModernButton;

/**
 * 洞察面板 — 基于物品数据生成智能建议。
 */
public class AdvicePanel extends JPanel implements Refreshable {

    private final String username;
    private JPanel cardsPanel;

    public AdvicePanel(String username) {
        this.username = username;
        setLayout(new BorderLayout(10, 10));
        setBackground(ColorTheme.BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        JLabel title = new JLabel("智能洞察");
        title.setFont(new Font("Microsoft YaHei", Font.BOLD, 20));
        title.setForeground(ColorTheme.ACCENT_BLUE);
        topBar.add(title, BorderLayout.WEST);

        ModernButton btnRefresh = new ModernButton("刷新分析");
        btnRefresh.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        btnRefresh.addActionListener(e -> refresh());
        topBar.add(btnRefresh, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        cardsPanel = new JPanel();
        cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
        cardsPanel.setOpaque(false);
        JScrollPane scroll = new JScrollPane(cardsPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(ColorTheme.BG_DARK);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        cardsPanel.removeAll();
        List<Insight> insights = AdviceService.generate(username);
        for (Insight in : insights) {
            cardsPanel.add(createCard(in));
            cardsPanel.add(Box.createVerticalStrut(6));
        }
        cardsPanel.revalidate();
        cardsPanel.repaint();
    }

    private JPanel createCard(Insight in) {
        JPanel card = new JPanel(new BorderLayout(12, 6));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        Color borderColor = switch (in.getType()) {
            case POSITIVE -> ColorTheme.ACCENT_GREEN;
            case WARNING -> ColorTheme.ACCENT_RED;
            default -> ColorTheme.BORDER_BRIGHT;
        };
        Color bgColor = switch (in.getType()) {
            case POSITIVE -> new Color(0x10, 0x28, 0x18);
            case WARNING -> new Color(0x28, 0x10, 0x14);
            default -> ColorTheme.BG_CARD;
        };

        card.setBackground(bgColor);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, 1),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));

        JLabel icon = new JLabel(in.getIcon());
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));

        JLabel title = new JLabel(in.getTitle());
        title.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        title.setForeground(ColorTheme.TEXT_PRIMARY);

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        header.setOpaque(false);
        header.add(icon);
        header.add(title);

        JLabel content = new JLabel("<html><div style='width:600px'>" + in.getContent() + "</div></html>");
        content.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        content.setForeground(ColorTheme.TEXT_SECONDARY);

        card.add(header, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }
}
