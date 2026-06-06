package view.component;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import util.ColorTheme;

/**
 * 科技风输入框 — 圆角深色背景。
 */
public class ModernTextField extends JTextField {

    public ModernTextField(int columns) {
        super(columns);
        setFont(new java.awt.Font("Microsoft YaHei", java.awt.Font.BOLD, 14));
        setForeground(ColorTheme.TEXT_PRIMARY);
        setBackground(ColorTheme.BG_INPUT);
        setCaretColor(ColorTheme.ACCENT_CYAN);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorTheme.BORDER, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
    }
}
