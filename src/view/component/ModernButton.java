package view.component;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import util.ColorTheme;

/**
 * 科技风圆角按钮 — 带悬停发光效果。
 */
public class ModernButton extends JButton {

    private Color normalBg;
    private Color hoverBg;
    private Color textColor;
    private boolean hovered;

    public ModernButton(String text) {
        super(text);
        this.normalBg = ColorTheme.BG_CARD;
        this.hoverBg = ColorTheme.BG_HOVER;
        this.textColor = ColorTheme.TEXT_PRIMARY;
        init();
    }

    public ModernButton(String text, Color bg, Color hover, Color fg) {
        super(text);
        this.normalBg = bg;
        this.hoverBg = hover;
        this.textColor = fg;
        init();
    }

    private void init() {
        setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        setForeground(textColor);
        setBackground(normalBg);
        setBorderPainted(false);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));

        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
            public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        int arc = 10;

        // 发光效果
        if (hovered) {
            g2d.setColor(ColorTheme.glow(normalBg == ColorTheme.BG_CARD
                    ? ColorTheme.ACCENT_CYAN : normalBg, 60));
            g2d.fillRoundRect(-2, -2, w + 4, h + 4, arc + 2, arc + 2);
        }

        g2d.setColor(hovered ? hoverBg : normalBg);
        g2d.fillRoundRect(0, 0, w, h, arc, arc);

        FontMetrics fm = g2d.getFontMetrics();
        int x = (w - fm.stringWidth(getText())) / 2;
        int y = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2d.setColor(textColor);
        g2d.drawString(getText(), x, y);
    }
}
