package view;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.Item;
import service.ItemService;
import service.LongTermService;
import service.StockpileService;
import util.ColorTheme;

/**
 * 全部物品视图 — 按模式分组，表格展示。
 */
public class AllItemsPanel extends JPanel implements Refreshable {

    private final String username;
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel lblCount;

    public AllItemsPanel(String username) {
        this.username = username;
        setLayout(new BorderLayout(10, 10));
        setBackground(ColorTheme.BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("全部物品总览");
        title.setFont(ColorTheme.FONT_TITLE);
        title.setForeground(ColorTheme.ACCENT_BLUE);
        title.setBorder(BorderFactory.createEmptyBorder(4, 4, 10, 4));
        add(title, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new String[]{"ID", "名称", "模式", "分类", "价格", "关键指标", "日期"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel);
        table.setFont(ColorTheme.FONT_SMALL);
        table.setBackground(ColorTheme.BG_CARD);
        table.setForeground(ColorTheme.TEXT_PRIMARY);
        table.setGridColor(ColorTheme.BORDER);
        table.setSelectionBackground(ColorTheme.BG_HOVER);
        table.setSelectionForeground(ColorTheme.ACCENT_BLUE);
        table.setRowHeight(28);
        table.getTableHeader().setFont(ColorTheme.FONT_SMALL);
        table.getTableHeader().setBackground(ColorTheme.BG_SIDEBAR);
        table.getTableHeader().setForeground(ColorTheme.ACCENT_BLUE);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(ColorTheme.BORDER));
        scroll.getViewport().setBackground(ColorTheme.BG_CARD);
        add(scroll, BorderLayout.CENTER);

        // 底部统计
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 8));
        footer.setBackground(ColorTheme.BG_DARK);
        lblCount = new JLabel();
        lblCount.setFont(ColorTheme.FONT_BODY);
        lblCount.setForeground(ColorTheme.TEXT_SECONDARY);
        footer.add(lblCount);
        add(footer, BorderLayout.SOUTH);

        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent e) { refresh(); }
        });
    }

    @Override
    public void refresh() {
        tableModel.setRowCount(0);
        List<Item> all = ItemService.getAllItems(username);

        // 按模式分组排序: LONG_TERM first, then STOCKPILE, then RECORD
        all.sort((a, b) -> {
            int modeCmp = Integer.compare(a.getMode().ordinal(), b.getMode().ordinal());
            if (modeCmp != 0) return modeCmp;
            return a.getPurchaseDate().compareTo(b.getPurchaseDate());
        });

        for (Item it : all) {
            String keyMetric = switch (it.getMode()) {
                case LONG_TERM -> String.format("日均¥%.2f / %d天",
                        LongTermService.getDailyCost(it), LongTermService.getDaysHeld(it));
                case STOCKPILE -> String.format("库存%.0f / 均价¥%.2f",
                        StockpileService.getCurrentStock(username, it.getId()),
                        StockpileService.getAveragePrice(username, it.getId()));
                case RECORD -> it.getStatus() != null ? it.getStatus() : "在用";
            };

            tableModel.addRow(new Object[]{
                    it.getId(), it.getName(),
                    it.getMode().getDisplayName(),
                    it.getCategory() != null ? it.getCategory() : "",
                    String.format("%.2f", it.getPurchasePrice()),
                    keyMetric,
                    it.getPurchaseDate()
            });
        }

        long ltCount = all.stream().filter(i -> i.getMode() == Item.ItemMode.LONG_TERM).count();
        long spCount = all.stream().filter(i -> i.getMode() == Item.ItemMode.STOCKPILE).count();
        long rcCount = all.stream().filter(i -> i.getMode() == Item.ItemMode.RECORD).count();
        lblCount.setText(String.format("共 %d 件物品 | 长期主义 %d | 囤货模式 %d | 纯记录 %d",
                all.size(), ltCount, spCount, rcCount));
    }
}
