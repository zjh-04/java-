package util;

import java.awt.*;

/**
 * 界面主题 — 深色科技风，高对比度，圆润边框。
 */
public class ColorTheme {

    /* ========== 背景色 ========== */
    public static final Color BG_DARK       = new Color(0x06, 0x0B, 0x1E);
    public static final Color BG_CARD       = new Color(0x0F, 0x14, 0x2D);
    public static final Color BG_SIDEBAR    = new Color(0x09, 0x0F, 0x24);
    public static final Color BG_INPUT      = new Color(0x12, 0x18, 0x36);
    public static final Color BG_HOVER      = new Color(0x18, 0x20, 0x42);
    public static final Color BG_TOPBAR     = new Color(0xF5, 0xF5, 0xFA);
    public static final Color TEXT_DARK     = new Color(0x1A, 0x1A, 0x2E);
    public static final Color TEXT_DARK_SEC = new Color(0x4A, 0x4A, 0x6A);

    /* ========== 强调色 ========== */
    public static final Color ACCENT_CYAN   = new Color(0x00, 0xE5, 0xFF);
    public static final Color ACCENT_BLUE   = new Color(0x44, 0x8A, 0xFF);
    public static final Color ACCENT_PURPLE = new Color(0x9D, 0x7B, 0xFF);
    public static final Color ACCENT_GREEN  = new Color(0x00, 0xE6, 0x76);
    public static final Color ACCENT_RED    = new Color(0xFF, 0x52, 0x65);
    public static final Color ACCENT_ORANGE = new Color(0xFF, 0x91, 0x35);
    public static final Color ACCENT_GOLD   = new Color(0xFF, 0xD6, 0x00);
    public static final Color ACCENT_PINK   = new Color(0xFF, 0x40, 0x80);

    /* ========== 文字色 (提高亮度) ========== */
    public static final Color TEXT_PRIMARY  = new Color(0xF0, 0xF0, 0xF5);
    public static final Color TEXT_SECONDARY= new Color(0xB8, 0xBA, 0xCC);
    public static final Color TEXT_MUTED    = new Color(0x78, 0x7A, 0x8C);

    /* ========== 边框 ========== */
    public static final Color BORDER        = new Color(0x22, 0x28, 0x4A);
    public static final Color BORDER_BRIGHT = new Color(0x35, 0x3D, 0x6E);

    /* ========== 字体 (全部加粗) ========== */
    public static final Font FONT_TITLE    = new Font("Microsoft YaHei", Font.BOLD, 22);
    public static final Font FONT_SUBTITLE = new Font("Microsoft YaHei", Font.BOLD, 16);
    public static final Font FONT_BODY     = new Font("Microsoft YaHei", Font.BOLD, 14);
    public static final Font FONT_SMALL    = new Font("Microsoft YaHei", Font.BOLD, 12);
    public static final Font FONT_NUMBER   = new Font("Consolas", Font.BOLD, 18);

    /* ========== 图标字体 ========== */
    public static final Font FONT_ICON     = new Font("Segoe UI Emoji", Font.PLAIN, 16);

    public static Color alpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    /** 带透明度的强调色，用于发光效果 */
    public static Color glow(Color c, int alpha) {
        return alpha(c, alpha);
    }
}
