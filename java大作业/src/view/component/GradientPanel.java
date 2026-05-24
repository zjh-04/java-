package view.component;

import java.awt.*;
import javax.swing.JPanel;

/**
 * 渐变圆角面板 — 科技感卡片。
 */
public class GradientPanel extends JPanel {
    private final Color topColor;
    private final Color bottomColor;

    public GradientPanel(Color top, Color bottom) {
        this.topColor = top;
        this.bottomColor = bottom;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        GradientPaint gp = new GradientPaint(0, 0, topColor, 0, getHeight(), bottomColor);
        g2d.setPaint(gp);
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
    }
}
