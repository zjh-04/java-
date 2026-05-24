package service;

import dao.ItemDao;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import model.Item;
import model.Item.ItemMode;
import util.DateUtil;

/**
 * 纯记录模式服务 — 简单地记录拥有物品及状态。
 */
public class RecordService {

    public static final String STATUS_ACTIVE   = "在用";
    public static final String STATUS_IDLE     = "闲置";
    public static final String STATUS_SOLD     = "已售";
    public static final String STATUS_LOST     = "已丢";

    public static final String[] ALL_STATUSES = {STATUS_ACTIVE, STATUS_IDLE, STATUS_SOLD, STATUS_LOST};

    /**
     * 添加记录物品
     */
    public static Item addItem(String username, String name, String category,
                               double price, String purchaseDate,
                               String status, String serialNumber,
                               String warrantyExpiry, String notes) {
        Item item = new Item();
        item.setName(name);
        item.setMode(ItemMode.RECORD);
        item.setCategory(category);
        item.setPurchasePrice(price);
        item.setPurchaseDate(purchaseDate);
        item.setStatus(status != null ? status : STATUS_ACTIVE);
        item.setSerialNumber(serialNumber);
        item.setWarrantyExpiry(warrantyExpiry);
        item.setNotes(notes);
        ItemDao.insertItem(username, item);
        return item;
    }

    /**
     * 更新物品状态
     */
    public static void updateStatus(String username, String itemId, String newStatus) {
        Item item = ItemDao.findItemById(username, itemId);
        if (item != null) {
            item.setStatus(newStatus);
            ItemDao.updateItem(username, item);
        }
    }

    /**
     * 按状态筛选
     */
    public static List<Item> listByStatus(String username, String status) {
        return ItemDao.findItemsByMode(username, ItemMode.RECORD).stream()
                .filter(it -> status.equals(it.getStatus()))
                .collect(Collectors.toList());
    }

    /**
     * 按分类筛选
     */
    public static List<Item> listByCategory(String username, String category) {
        return ItemDao.findItemsByMode(username, ItemMode.RECORD).stream()
                .filter(it -> category.equals(it.getCategory()))
                .collect(Collectors.toList());
    }

    /**
     * 获取所有记录物品，按购买日期降序
     */
    public static List<Item> getAllItemsSorted(String username) {
        return ItemDao.findItemsByMode(username, ItemMode.RECORD).stream()
                .sorted(Comparator.comparing(Item::getPurchaseDate).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 格式化输出物品详情（记录视角）
     */
    public static String formatDetail(Item item) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("  物品: %s\n", item.getName()));
        sb.append(String.format("  分类: %s\n", item.getCategory() != null ? item.getCategory() : "未分类"));
        sb.append(String.format("  购买价格: ¥%.2f\n", item.getPurchasePrice()));
        sb.append(String.format("  购买日期: %s\n", item.getPurchaseDate()));
        sb.append(String.format("  状态: %s\n", item.getStatus() != null ? item.getStatus() : STATUS_ACTIVE));

        long days = DateUtil.daysBetween(item.getPurchaseDate(), DateUtil.getCurrentDate());
        if (days > 0) sb.append(String.format("  已拥有: %d 天\n", days));

        if (item.getSerialNumber() != null && !item.getSerialNumber().isEmpty())
            sb.append(String.format("  序列号: %s\n", item.getSerialNumber()));
        if (item.getWarrantyExpiry() != null && !item.getWarrantyExpiry().isEmpty())
            sb.append(String.format("  保修到期: %s\n", item.getWarrantyExpiry()));
        if (item.getNotes() != null && !item.getNotes().isEmpty())
            sb.append(String.format("  备注: %s\n", item.getNotes()));

        return sb.toString();
    }

    /**
     * 统计各状态数量
     */
    public static String getStatusSummary(String username) {
        List<Item> all = ItemDao.findItemsByMode(username, ItemMode.RECORD);
        StringBuilder sb = new StringBuilder("  状态统计:\n");
        for (String status : ALL_STATUSES) {
            long count = all.stream().filter(it -> status.equals(it.getStatus())).count();
            sb.append(String.format("    %s: %d 件\n", status, count));
        }
        sb.append(String.format("    总计: %d 件\n", all.size()));
        return sb.toString();
    }
}
