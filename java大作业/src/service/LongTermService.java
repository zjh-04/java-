package service;

import dao.ItemDao;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import model.Item;
import model.Item.ItemMode;
import util.DateUtil;

/**
 * 长期主义模式服务 — 日均成本计算、预期年限推算。
 */
public class LongTermService {

    /**
     * 添加长期物品
     */
    public static Item addItem(String username, String name, String category,
                               double price, String purchaseDate,
                               Integer expectedYears, String notes) {
        Item item = new Item();
        item.setName(name);
        item.setMode(ItemMode.LONG_TERM);
        item.setCategory(category);
        item.setPurchasePrice(price);
        item.setPurchaseDate(purchaseDate);
        item.setExpectedLifespanYears(expectedYears);
        item.setNotes(notes);
        ItemDao.insertItem(username, item);
        return item;
    }

    /**
     * 计算实际日均成本 = 购买价格 / 已持有天数
     */
    public static double getDailyCost(Item item) {
        long days = DateUtil.daysBetween(item.getPurchaseDate(), DateUtil.getCurrentDate());
        if (days <= 0) days = 1;
        return item.getPurchasePrice() / days;
    }

    /**
     * 按预期年限推算日均成本 = 购买价格 / (预期年限 * 365)
     */
    public static double getProjectedDailyCost(Item item) {
        if (item.getExpectedLifespanYears() == null || item.getExpectedLifespanYears() <= 0) {
            return getDailyCost(item);
        }
        return item.getPurchasePrice() / (item.getExpectedLifespanYears() * 365.0);
    }

    /**
     * 获取持有天数
     */
    public static long getDaysHeld(Item item) {
        long days = DateUtil.daysBetween(item.getPurchaseDate(), DateUtil.getCurrentDate());
        return Math.max(days, 1);
    }

    /**
     * 获取所有长期物品，按日均成本降序
     */
    public static List<Item> getAllItemsSorted(String username) {
        return ItemDao.findItemsByMode(username, ItemMode.LONG_TERM).stream()
                .sorted(Comparator.comparingDouble(LongTermService::getDailyCost).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 格式化输出详情
     */
    public static String formatDetail(Item item) {
        long days = getDaysHeld(item);
        double dailyCost = getDailyCost(item);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("  物品: %s\n", item.getName()));
        sb.append(String.format("  分类: %s\n", item.getCategory() != null ? item.getCategory() : "未分类"));
        sb.append(String.format("  购买价格: ¥%.2f\n", item.getPurchasePrice()));
        sb.append(String.format("  购买日期: %s\n", item.getPurchaseDate()));
        sb.append(String.format("  已持有: %d 天\n", days));
        sb.append(String.format("  实际日均成本: ¥%.2f / 天\n", dailyCost));
        if (item.getExpectedLifespanYears() != null && item.getExpectedLifespanYears() > 0) {
            sb.append(String.format("  预期使用年限: %d 年\n", item.getExpectedLifespanYears()));
            sb.append(String.format("  预期日均成本: ¥%.2f / 天\n", getProjectedDailyCost(item)));
        }
        if (item.getNotes() != null && !item.getNotes().isEmpty()) {
            sb.append(String.format("  备注: %s\n", item.getNotes()));
        }
        return sb.toString();
    }
}
