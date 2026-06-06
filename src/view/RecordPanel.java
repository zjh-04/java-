package view;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.Item;
import service.AchievementService;
import service.ItemService;
import service.RecordService;
import util.Categories;
import util.ColorTheme;
import util.DateUtil;
import view.component.GradientPanel;
import view.component.ModernButton;
import view.component.ModernTextField;

/**
 * 纯记录模式面板。
 */
public class RecordPanel extends JPanel implements Refreshable {

    private final String username;
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextArea detailArea;
    private JComboBox<String> cbFilterStatus;

    private ModernTextField tfName, tfPrice, tfDate;
    private JComboBox<String> cbCategory;
    private ModernTextField tfSerial, tfWarranty, tfNotes;
    private JComboBox<String> cbStatus;

    public RecordPanel(String username) {
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

        JLabel title = new JLabel("添加记录物品");
        title.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        title.setForeground(ColorTheme.ACCENT_PURPLE);
        g.gridx = 0; g.gridy = 0; g.gridwidth = 4;
        panel.add(title, g);
        g.gridwidth = 1;

        tfName = new ModernTextField(10);
        cbCategory = Categories.createComboBox();
        tfPrice = new ModernTextField(8);
        tfDate = new ModernTextField(10);
        tfDate.setText(DateUtil.getCurrentDate());
        tfSerial = new ModernTextField(10);
        tfWarranty = new ModernTextField(10);
        tfNotes = new ModernTextField(14);
        cbStatus = new JComboBox<>(RecordService.ALL_STATUSES);
        cbStatus.setBackground(ColorTheme.BG_INPUT);
        cbStatus.setForeground(ColorTheme.TEXT_PRIMARY);
        cbStatus.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        cbStatus.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                c.setBackground(isSelected ? ColorTheme.BG_HOVER : ColorTheme.BG_INPUT);
                c.setForeground(isSelected ? ColorTheme.ACCENT_PURPLE : ColorTheme.TEXT_PRIMARY);
                return c;
            }
        });

        g.gridy = 1;
        g.gridx = 0; panel.add(lbl("名称"), g);
        g.gridx = 1; panel.add(tfName, g);
        g.gridx = 2; panel.add(lbl("分类"), g);
        g.gridx = 3; panel.add(cbCategory, g);
        g.gridy = 2;
        g.gridx = 0; panel.add(lbl("价格"), g);
        g.gridx = 1; panel.add(tfPrice, g);
        g.gridx = 2; panel.add(lbl("日期"), g);
        g.gridx = 3; panel.add(tfDate, g);
        g.gridy = 3;
        g.gridx = 0; panel.add(lbl("状态"), g);
        g.gridx = 1; panel.add(cbStatus, g);
        g.gridx = 2; panel.add(lbl("序列号"), g);
        g.gridx = 3; panel.add(tfSerial, g);
        g.gridy = 4;
        g.gridx = 0; panel.add(lbl("保修到期"), g);
        g.gridx = 1; panel.add(tfWarranty, g);
        g.gridx = 2; panel.add(lbl("备注"), g);
        g.gridx = 3; panel.add(tfNotes, g);

        ModernButton btnAdd = new ModernButton("添加", ColorTheme.ACCENT_PURPLE,
                new Color(0x7B, 0x5E, 0xE0), new Color(0x06, 0x0B, 0x1E));
        btnAdd.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        g.gridy = 5; g.gridx = 0; g.gridwidth = 4; g.anchor = GridBagConstraints.CENTER;
        g.insets = new Insets(10, 5, 3, 5);
        panel.add(btnAdd, g);

        btnAdd.addActionListener(e -> addItem());
        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ColorTheme.BG_DARK);

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filterBar.setBackground(ColorTheme.BG_DARK);
        filterBar.add(lbl("筛选状态:"));
        cbFilterStatus = new JComboBox<>(new String[]{"全部", "在用", "闲置", "已售", "已丢"});
        cbFilterStatus.setBackground(ColorTheme.BG_INPUT);
        cbFilterStatus.setForeground(ColorTheme.TEXT_PRIMARY);
        cbFilterStatus.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        cbFilterStatus.addActionListener(e -> refresh());
        filterBar.add(cbFilterStatus);
        panel.add(filterBar, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new String[]{"ID", "名称", "分类", "价格", "状态", "日期", "序列号"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel);
        table.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        table.setBackground(ColorTheme.BG_CARD);
        table.setForeground(ColorTheme.TEXT_PRIMARY);
        table.setGridColor(ColorTheme.BORDER);
        table.setSelectionBackground(ColorTheme.BG_HOVER);
        table.setSelectionForeground(ColorTheme.ACCENT_PURPLE);
        table.setRowHeight(28);
        table.getTableHeader().setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        table.getTableHeader().setBackground(ColorTheme.BG_SIDEBAR);
        table.getTableHeader().setForeground(ColorTheme.ACCENT_PURPLE);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(ColorTheme.BORDER, 1));
        scroll.getViewport().setBackground(ColorTheme.BG_CARD);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        btnRow.setBackground(ColorTheme.BG_DARK);

        ModernButton btnUpdate = new ModernButton("更改状态", ColorTheme.ACCENT_GREEN,
                new Color(0x00, 0xB0, 0x50), ColorTheme.BG_DARK);
        btnUpdate.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        ModernButton btnSummary = new ModernButton("状态统计", ColorTheme.ACCENT_GOLD,
                new Color(0xC0, 0xA0, 0x00), ColorTheme.BG_DARK);
        btnSummary.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        ModernButton btnDelete = new ModernButton("删除", ColorTheme.ACCENT_RED,
                new Color(0xD0, 0x30, 0x40), ColorTheme.TEXT_PRIMARY);
        btnDelete.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));

        btnUpdate.addActionListener(e -> updateStatus());
        btnSummary.addActionListener(e -> showSummary());
        btnDelete.addActionListener(e -> deleteSelected());

        btnRow.add(btnUpdate); btnRow.add(btnSummary); btnRow.add(btnDelete);
        panel.add(btnRow, BorderLayout.SOUTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createDetailPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ColorTheme.BG_DARK);
        panel.setPreferredSize(new Dimension(270, 0));

        JLabel lbl = new JLabel("详情");
        lbl.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        lbl.setForeground(ColorTheme.ACCENT_PURPLE);
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
                    if (item != null) detailArea.setText(RecordService.formatDetail(item));
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

    private void addItem() {
        String name = tfName.getText().trim();
        if (name.isEmpty()) { JOptionPane.showMessageDialog(this, "请输入名称"); return; }
        String cat = (String) cbCategory.getSelectedItem();
        double price;
        try { price = Double.parseDouble(tfPrice.getText().trim()); }
        catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "价格格式错误"); return; }
        String date = tfDate.getText().trim();
        if (date.isEmpty()) date = DateUtil.getCurrentDate();
        String status = (String) cbStatus.getSelectedItem();
        String serial = tfSerial.getText().trim();
        String warranty = tfWarranty.getText().trim();
        String notes = tfNotes.getText().trim();

        RecordService.addItem(username, name, cat, price, date, status,
                serial.isEmpty() ? null : serial,
                warranty.isEmpty() ? null : warranty,
                notes.isEmpty() ? null : notes);
        showAchievementUnlock(AchievementService.checkAll(username));
        refresh();
        tfName.setText(""); cbCategory.setSelectedIndex(0); tfPrice.setText("");
        tfDate.setText(DateUtil.getCurrentDate());
        tfSerial.setText(""); tfWarranty.setText(""); tfNotes.setText("");
    }

    private void updateStatus() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "请先选择物品"); return; }
        String id = (String) tableModel.getValueAt(row, 0);
        String name = (String) tableModel.getValueAt(row, 1);
        String cur = (String) tableModel.getValueAt(row, 4);
        JComboBox<String> cb = new JComboBox<>(RecordService.ALL_STATUSES);
        cb.setSelectedItem(cur);
        cb.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        int r = JOptionPane.showConfirmDialog(this, new Object[]{"新状态:", cb},
                "更新状态 - " + name, JOptionPane.OK_CANCEL_OPTION);
        if (r == JOptionPane.OK_OPTION) {
            RecordService.updateStatus(username, id, (String) cb.getSelectedItem());
            showAchievementUnlock(AchievementService.checkAll(username));
            refresh();
        }
    }

    private void showSummary() {
        JOptionPane.showMessageDialog(this, RecordService.getStatusSummary(username));
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "请先选择物品"); return; }
        String id = (String) tableModel.getValueAt(row, 0);
        String name = (String) tableModel.getValueAt(row, 1);
        int r = JOptionPane.showConfirmDialog(this, "确认删除「" + name + "」？", "确认", JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) {
            ItemService.deleteItem(username, id);
            showAchievementUnlock(AchievementService.checkAll(username));
            refresh(); detailArea.setText("");
        }
    }

    @Override
    public void refresh() {
        tableModel.setRowCount(0);
        String filter = (String) cbFilterStatus.getSelectedItem();
        List<Item> items = (filter == null || "全部".equals(filter))
                ? RecordService.getAllItemsSorted(username)
                : RecordService.listByStatus(username, filter);
        for (Item it : items) {
            tableModel.addRow(new Object[]{
                    it.getId(), it.getName(),
                    it.getCategory() != null ? it.getCategory() : "",
                    String.format("%.2f", it.getPurchasePrice()),
                    it.getStatus() != null ? it.getStatus() : "在用",
                    it.getPurchaseDate(),
                    it.getSerialNumber() != null ? it.getSerialNumber() : ""
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