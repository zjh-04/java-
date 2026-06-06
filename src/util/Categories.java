package util;

import java.awt.*;
import javax.swing.*;

/**
 * 物品分类选项 — 统一的分类下拉列表。
 */
public class Categories {

    public static final String[] OPTIONS = {
        "电子数码", "家居生活", "服饰鞋包", "食品饮料",
        "个护美妆", "图书文具", "运动户外", "医疗保健",
        "宠物用品", "汽车用品", "办公设备", "其他"
    };

    public static JComboBox<String> createComboBox() {
        JComboBox<String> cb = new JComboBox<>(OPTIONS);
        cb.setBackground(ColorTheme.BG_INPUT);
        cb.setForeground(ColorTheme.TEXT_PRIMARY);
        cb.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                JLabel c = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                c.setBackground(isSelected ? ColorTheme.BG_HOVER : ColorTheme.BG_INPUT);
                c.setForeground(isSelected ? ColorTheme.ACCENT_CYAN : ColorTheme.TEXT_PRIMARY);
                return c;
            }
        });
        return cb;
    }
}
