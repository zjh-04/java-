package view;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.Item;
import model.Item.ItemMode;
import service.AchievementService;
import service.ItemService;
import service.StockpileService;
import util.Categories;
import util.ColorTheme;
import util.DateUtil;
import view.component.GradientPanel;
import view.component.ModernButton;
import view.component.ModernTextField;

/**
 * 囤货模式面板。
 */
public class StockpilePanel extends JPanel implements Refreshable {

    private final String username;
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextArea detailArea;

    private ModernTextField tfName, tfPrice, tfQty, tfDate, tfSafety, tfNotes;
    private JComboBox<String> cbCategory;

    public StockpilePanel(String username) {
        this.username = username;
        setLayout(new BorderLayout(10, 10));
        setBackground(ColorTheme.BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(createFormPanel(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        add(createDetailPanel(), BorderLayout.EAST);
    }

    private JPanel createFormPanel() {
        GradientPanel panel = new GradientPanel(ColorTheme.BG_SIDEBAR, ColorTheme.BG_CARD);
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 5, 3, 5);
        g.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("新建囤货物品");
        title.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        title.setForeground(ColorTheme.ACCENT_ORANGE);
        g.gridx = 0; g.gridy = 0; g.gridwidth = 4;
        panel.add(title, g);
        g.gridwidth = 1;

        tfName = new ModernTextField(10);
        cbCategory = Categories.createComboBox();
        tfQty = new ModernTextField(5);
        tfPrice = new ModernTextField(8);
        tfDate = new ModernTextField(10);
        tfDate.setText(DateUtil.getCurrentDate());
        tfSafety = new ModernTextField(5);
        tfNotes = new ModernTextField(14);

        g.gridy = 1;
        g.gridx = 0; panel.add(lbl("名称"), g);
        g.gridx = 1; panel.add(tfName, g);
        g.gridx = 2; panel.add(lbl("分类"), g);
        g.gridx = 3; panel.add(cbCategory, g);
        g.gridy = 2;
        g.gridx = 0; panel.add(lbl("数量"), g);
        g.gridx = 1; panel.add(tfQty, g);
        g.gridx = 2; panel.add(lbl("总金额"), g);
        g.gridx = 3; panel.add(tfPrice, g);
        g.gridy = 3;
        g.gridx = 0; panel.add(lbl("日期"), g);
        g.gridx = 1; panel.add(tfDate, g);
        g.gridx = 2; panel.add(lbl("安全库存"), g);
        g.gridx = 3; panel.add(tfSafety, g);
        g.gridy = 4;
        g.gridx = 0; panel.add(lbl("备注"), g);
        g.gridx = 1; panel.add(tfNotes, g);

        ModernButton btnCreate = new ModernButton("创建", ColorTheme.ACCENT_ORANGE,
                new Color(0xD0, 0x70, 0x00), new Color(0x06, 0x0B, 0x1E));
        btnCreate.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        g.gridy = 5; g.gridx = 0; g.gridwidth = 4; g.anchor = GridBagConstraints.CENTER;
        g.insets = new Insets(10, 5, 3, 5);
        panel.add(btnCreate, g);

        btnCreate.addActionListener(e -> createItem());
        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ColorTheme.BG_DARK);

        tableModel = new DefaultTableModel(
                new String[]{"ID", "名称", "库存", "安全线", "均价", "状态"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel);
        table.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        table.setBackground(ColorTheme.BG_CARD);
        table.setForeground(ColorTheme.TEXT_PRIMARY);
        table.setGridColor(ColorTheme.BORDER);
        table.setSelectionBackground(ColorTheme.BG_HOVER);
        table.setSelectionForeground(ColorTheme.ACCENT_ORANGE);
        table.setRowHeight(28);
        table.getTableHeader().setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        table.getTableHeader().setBackground(ColorTheme.BG_SIDEBAR);
        table.getTableHeader().setForeground(ColorTheme.ACCENT_ORANGE);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(ColorTheme.BORDER, 1));
        scroll.getViewport().setBackground(ColorTheme.BG_CARD);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        btnRow.setBackground(ColorTheme.BG_DARK);

        ModernButton btnBatch = new ModernButton("追加入库", ColorTheme.ACCENT_GREEN,
                new Color(0x00, 0xB0, 0x50), ColorTheme.BG_DARK);
        btnBatch.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        ModernButton btnConsume = new ModernButton("记录消耗", ColorTheme.ACCENT_PURPLE,
                new Color(0x70, 0x50, 0xD0), ColorTheme.BG_DARK);
        btnConsume.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        ModernButton btnWarn = new ModernButton("库存预警", ColorTheme.ACCENT_RED,
                new Color(0xD0, 0x30, 0x40), ColorTheme.TEXT_PRIMARY);
        btnWarn.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        ModernButton btnCompare = new ModernButton("比价", ColorTheme.ACCENT_GOLD,
                new Color(0xC0, 0xA0, 0x00), ColorTheme.BG_DARK);
        btnCompare.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        ModernButton btnDelete = new ModernButton("删除", ColorTheme.ACCENT_RED,
                new Color(0xD0, 0x30, 0x40), ColorTheme.TEXT_PRIMARY);
        btnDelete.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));

        btnBatch.addActionListener(e -> addBatch());
        btnConsume.addActionListener(e -> consume());
        btnWarn.addActionListener(e -> checkWarnings());
        btnCompare.addActionListener(e -> comparePrice());
        btnDelete.addActionListener(e -> deleteSelected());

        btnRow.add(btnBatch); btnRow.add(btnConsume);
        btnRow.add(btnWarn); btnRow.add(btnCompare); btnRow.add(btnDelete);

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(btnRow, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createDetailPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ColorTheme.BG_DARK);
        panel.setPreferredSize(new Dimension(290, 0));

        JLabel lbl = new JLabel("详情");
        lbl.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        lbl.setForeground(ColorTheme.ACCENT_ORANGE);
        lbl.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
        panel.add(lbl, BorderLayout.NORTH);

        detailArea = new JTextArea();
        detailArea.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        detailArea.setForeground(ColorTheme.TEXT_PRIMARY);
        detailArea.setBackground(ColorTheme.BG_CARD);
        detailArea.setEditable(false);
        JScrollPane sp = new JScrollPane(detailArea);
        sp.setBorder(BorderFactory.createLineBorder(ColorTheme.BORDER, 1));
        panel.add(sp, BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    String id = (String) tableModel.getValueAt(row, 0);
                    Item item = ItemService.getItemById(username, id);
                    if (item != null) detailArea.setText(StockpileService.formatDetail(username, item));
                }
            }
        });
        return panel;
    }

    private JLabel lbl(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        l.setForeground(ColorTheme.TEXT_SECONDARY);
        return l;
    }

    private Item getSelectedItem() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "请先选择物品"); return null; }
        String id = (String) tableModel.getValueAt(row, 0);
        return ItemService.getItemById(username, id);
    }

    private void createItem() {
        String name = tfName.getText().trim();
        if (name.isEmpty()) { JOptionPane.showMessageDialog(this, "请输入名称"); return; }
        int qty;
        try { qty = Integer.parseInt(tfQty.getText().trim()); }
        catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "数量格式错误"); return; }
        double price;
        try { price = Double.parseDouble(tfPrice.getText().trim()); }
        catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "金额格式错误"); return; }
        String date = tfDate.getText().trim();
        if (date.isEmpty()) date = DateUtil.getCurrentDate();
        double safety;
        try { safety = Double.parseDouble(tfSafety.getText().trim()); }
        catch (NumberFormatException ex) { safety = 0; }
        String cat = (String) cbCategory.getSelectedItem();
        String notes = tfNotes.getText().trim();

        StockpileService.createItem(username, name, cat, qty, price, date, safety,
                notes.isEmpty() ? null : notes);
        showAchievementUnlock(AchievementService.checkAll(username));
        refresh();
        tfName.setText(""); cbCategory.setSelectedIndex(0); tfQty.setText(""); tfPrice.setText("");
        tfDate.setText(DateUtil.getCurrentDate()); tfSafety.setText(""); tfNotes.setText("");
    }

    private void addBatch() {
        Item item = getSelectedItem();
        if (item == null) return;
        JTextField qtyF = new JTextField(5);
        qtyF.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        JTextField priceF = new JTextField(8);
        priceF.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        JTextField dateF = new JTextField(DateUtil.getCurrentDate(), 10);
        dateF.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        Object[] fields = {"数量:", qtyF, "总金额:", priceF, "日期:", dateF};
        int r = JOptionPane.showConfirmDialog(this, fields, "追加入库 - " + item.getName(),
                JOptionPane.OK_CANCEL_OPTION);
        if (r != JOptionPane.OK_OPTION) return;
        try {
            int qty = Integer.parseInt(qtyF.getText().trim());
            double price = Double.parseDouble(priceF.getText().trim());
            String date = dateF.getText().trim();
            if (date.isEmpty()) date = DateUtil.getCurrentDate();
            StockpileService.addBatch(username, item.getId(), qty, price, date);
            showAchievementUnlock(AchievementService.checkAll(username));
            JOptionPane.showMessageDialog(this,
                    String.format("入库成功！%d件 ¥%.2f\n新均价: ¥%.2f/单位", qty, price,
                            StockpileService.getAveragePrice(username, item.getId())));
            refresh();
        } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "输入格式错误"); }
    }

    private void consume() {
        Item item = getSelectedItem();
        if (item == null) return;
        double stock = StockpileService.getCurrentStock(username, item.getId());
        JTextField qtyF = new JTextField(5);
        qtyF.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        JTextField dateF = new JTextField(DateUtil.getCurrentDate(), 10);
        dateF.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        JTextField noteF = new JTextField(10);
        noteF.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        Object[] fields = {"消耗数量:", qtyF, "日期:", dateF, "备注:", noteF,
                " ", "当前库存: " + String.format("%.0f", stock)};
        int r = JOptionPane.showConfirmDialog(this, fields, "记录消耗 - " + item.getName(),
                JOptionPane.OK_CANCEL_OPTION);
        if (r != JOptionPane.OK_OPTION) return;
        try {
            int qty = Integer.parseInt(qtyF.getText().trim());
            String date = dateF.getText().trim();
            if (date.isEmpty()) date = DateUtil.getCurrentDate();
            String notes = noteF.getText().trim();
            StockpileService.consume(username, item.getId(), qty, date, notes.isEmpty() ? null : notes);
            showAchievementUnlock(AchievementService.checkAll(username));
            JOptionPane.showMessageDialog(this, "已记录消耗。剩余库存: "
                    + String.format("%.0f", StockpileService.getCurrentStock(username, item.getId())));
            refresh();
        } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "输入格式错误"); }
    }

    private void checkWarnings() {
        List<String> warnings = StockpileService.checkLowStock(username);
        JOptionPane.showMessageDialog(this,
                warnings.isEmpty() ? "所有囤货物品库存充足" : "═══ 库存预警 ═══\n\n" + String.join("\n", warnings),
                "库存预警", warnings.isEmpty() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
    }

    private void comparePrice() {
        Item item = getSelectedItem();
        if (item == null) return;
        JTextField priceF = new JTextField(8);
        priceF.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        Object[] fields = {"外部单价:", priceF, " ",
                "当前囤货均价: ¥" + String.format("%.2f",
                        StockpileService.getAveragePrice(username, item.getId()))};
        int r = JOptionPane.showConfirmDialog(this, fields, "比价 - " + item.getName(),
                JOptionPane.OK_CANCEL_OPTION);
        if (r != JOptionPane.OK_OPTION) return;
        try {
            JOptionPane.showMessageDialog(this,
                    StockpileService.comparePrice(username, item.getId(),
                            Double.parseDouble(priceF.getText().trim())));
        } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "输入格式错误"); }
    }

    private void deleteSelected() {
        Item item = getSelectedItem();
        if (item == null) return;
        int r = JOptionPane.showConfirmDialog(this,
                "确认删除「" + item.getName() + "」及其所有批次和消耗记录？", "确认", JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) {
            ItemService.deleteItem(username, item.getId());
            showAchievementUnlock(AchievementService.checkAll(username));
            refresh(); detailArea.setText("");
        }
    }

    @Override
    public void refresh() {
        tableModel.setRowCount(0);
        for (Item it : ItemService.getItemsByMode(username, ItemMode.STOCKPILE)) {
            double stock = StockpileService.getCurrentStock(username, it.getId());
            double avg = StockpileService.getAveragePrice(username, it.getId());
            double safety = it.getSafetyStock() != null ? it.getSafetyStock() : 0;
            String flag = stock <= 0 ? "已用尽" : stock <= safety ? "不足!" : "正常";
            tableModel.addRow(new Object[]{
                    it.getId(), it.getName(), String.format("%.0f", stock),
                    String.format("%.0f", safety), String.format("%.2f", avg), flag
            });
        }
    }

    private void showAchievementUnlock(java.util.List<String> ids) {
        String msg = AchievementService.getUnlockMessage(ids);
        if (msg != null) {
            JOptionPane.showMessageDialog(this, msg, "成就解锁", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
