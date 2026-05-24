package view;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.Item;
import service.AchievementService;
import service.ItemService;
import service.LongTermService;
import util.Categories;
import util.ColorTheme;
import util.DateUtil;
import view.component.GradientPanel;
import view.component.ModernButton;
import view.component.ModernTextField;

/**
 * 长期主义模式面板。
 */
public class LongTermPanel extends JPanel implements Refreshable {

    private final String username;
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextArea detailArea;
    private ModernTextField tfName, tfPrice, tfDate, tfYears, tfNotes;
    private JComboBox<String> cbCategory;

    public LongTermPanel(String username) {
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
        g.anchor = GridBagConstraints.WEST;

        JLabel title = new JLabel("添加长期物品");
        title.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        title.setForeground(ColorTheme.ACCENT_CYAN);
        g.gridx = 0; g.gridy = 0; g.gridwidth = 4;
        panel.add(title, g);
        g.gridwidth = 1;

        tfName = new ModernTextField(10);
        cbCategory = Categories.createComboBox();
        tfPrice = new ModernTextField(8);
        tfDate = new ModernTextField(10);
        tfDate.setText(DateUtil.getCurrentDate());
        tfYears = new ModernTextField(4);
        tfNotes = new ModernTextField(14);

        g.gridy = 1;
        g.gridx = 0; panel.add(lbl("名称"), g);
        g.gridx = 1; panel.add(tfName, g);
        g.gridx = 2; panel.add(lbl("分类"), g);
        g.gridx = 3; panel.add(cbCategory, g);

        g.gridy = 2;
        g.gridx = 0; panel.add(lbl("价格"), g);
        g.gridx = 1; panel.add(tfPrice, g);
        g.gridx = 2; panel.add(lbl("购买日期"), g);
        g.gridx = 3; panel.add(tfDate, g);

        g.gridy = 3;
        g.gridx = 0; panel.add(lbl("预期年限"), g);
        g.gridx = 1; panel.add(tfYears, g);
        g.gridx = 2; panel.add(lbl("备注"), g);
        g.gridx = 3; panel.add(tfNotes, g);

        ModernButton btnAdd = new ModernButton("添加", ColorTheme.ACCENT_CYAN,
                new Color(0x00, 0xB8, 0xD4), new Color(0x06, 0x0B, 0x1E));
        btnAdd.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        g.gridy = 4; g.gridx = 0; g.gridwidth = 4; g.anchor = GridBagConstraints.CENTER;
        g.insets = new Insets(10, 5, 3, 5);
        panel.add(btnAdd, g);

        btnAdd.addActionListener(e -> addItem());
        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ColorTheme.BG_DARK);

        tableModel = new DefaultTableModel(
                new String[]{"ID", "名称", "分类", "价格", "已持天数", "日均成本", "预期"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel);
        table.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        table.setBackground(ColorTheme.BG_CARD);
        table.setForeground(ColorTheme.TEXT_PRIMARY);
        table.setGridColor(ColorTheme.BORDER);
        table.setSelectionBackground(ColorTheme.BG_HOVER);
        table.setSelectionForeground(ColorTheme.ACCENT_CYAN);
        table.setRowHeight(28);
        table.getTableHeader().setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        table.getTableHeader().setBackground(ColorTheme.BG_SIDEBAR);
        table.getTableHeader().setForeground(ColorTheme.ACCENT_CYAN);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(ColorTheme.BORDER, 1));
        scroll.getViewport().setBackground(ColorTheme.BG_CARD);

        ModernButton btnDelete = new ModernButton("删除选中",
                ColorTheme.ACCENT_RED, new Color(0xD0, 0x30, 0x40), ColorTheme.TEXT_PRIMARY);
        btnDelete.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        btnDelete.addActionListener(e -> deleteSelected());

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(btnDelete, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createDetailPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ColorTheme.BG_DARK);
        panel.setPreferredSize(new Dimension(270, 0));

        JLabel lbl = new JLabel("详情");
        lbl.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        lbl.setForeground(ColorTheme.ACCENT_CYAN);
        lbl.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
        panel.add(lbl, BorderLayout.NORTH);

        detailArea = new JTextArea();
        detailArea.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        detailArea.setForeground(ColorTheme.TEXT_PRIMARY);
        detailArea.setBackground(ColorTheme.BG_CARD);
        detailArea.setEditable(false);
        detailArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JScrollPane sp = new JScrollPane(detailArea);
        sp.setBorder(BorderFactory.createLineBorder(ColorTheme.BORDER, 1));
        panel.add(sp, BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    String id = (String) tableModel.getValueAt(row, 0);
                    Item item = ItemService.getItemById(username, id);
                    if (item != null) detailArea.setText(LongTermService.formatDetail(item));
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
        Integer years = null;
        String yrsStr = tfYears.getText().trim();
        if (!yrsStr.isEmpty()) {
            try { years = Integer.parseInt(yrsStr); }
            catch (NumberFormatException ex) { /* ignore */ }
        }
        String notes = tfNotes.getText().trim();

        LongTermService.addItem(username, name, cat, price, date, years, notes.isEmpty() ? null : notes);
        showAchievementUnlock(AchievementService.checkAll(username));
        refresh();
        tfName.setText(""); cbCategory.setSelectedIndex(0); tfPrice.setText("");
        tfDate.setText(DateUtil.getCurrentDate()); tfYears.setText(""); tfNotes.setText("");
        tfName.requestFocus();
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "请先选择要删除的物品"); return; }
        String id = (String) tableModel.getValueAt(row, 0);
        String name = (String) tableModel.getValueAt(row, 1);
        int r = JOptionPane.showConfirmDialog(this, "确认删除「" + name + "」？", "确认", JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) {
            ItemService.deleteItem(username, id);
            showAchievementUnlock(AchievementService.checkAll(username));
            refresh();
            detailArea.setText("");
        }
    }

    @Override
    public void refresh() {
        tableModel.setRowCount(0);
        List<Item> items = LongTermService.getAllItemsSorted(username);
        for (Item it : items) {
            tableModel.addRow(new Object[]{
                    it.getId(), it.getName(),
                    it.getCategory() != null ? it.getCategory() : "",
                    String.format("%.2f", it.getPurchasePrice()),
                    LongTermService.getDaysHeld(it),
                    String.format("%.2f", LongTermService.getDailyCost(it)),
                    it.getExpectedLifespanYears() != null ? it.getExpectedLifespanYears() + "年" : "-"
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
