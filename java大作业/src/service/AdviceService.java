package service;

import dao.ItemDao;
import java.util.*;
import java.util.stream.Collectors;
import model.Item;
import model.Item.ItemMode;
import util.DateUtil;

/**
 * 物品洞察服务 — 基于物品数据生成智能建议。
 */
public class AdviceService {

    public enum Type { POSITIVE, INFO, WARNING }

    public static class Insight {
        private final String title;
        private final String content;
        private final Type type;
        public Insight(String t, String c, Type tp) { this.title = t; this.content = c; this.type = tp; }
        public String getTitle() { return title; }
        public String getContent() { return content; }
        public Type getType() { return type; }
        public String getIcon() {
            return switch (type) {
                case POSITIVE -> "✅";
                case INFO -> "ℹ️";
                case WARNING -> "⚠️";
            };
        }
    }

    public static List<Insight> generate(String username) {
        List<Insight> list = new ArrayList<>();
        List<Item> all = ItemDao.findItemsByUser(username);
        if (all.isEmpty()) {
            list.add(new Insight("开始记录吧", "还没有任何物品记录，开始添加第一个物品吧！", Type.INFO));
            return list;
        }

        checkHighDailyCost(username, list);
        checkIdleItems(username, list);
        checkStockWarnings(username, list);
        checkWarrantyExpiring(username, list);
        checkCategoryConcentration(username, all, list);
        checkOverallSummary(username, all, list);

        if (list.isEmpty()) {
            list.add(new Insight("管理有序", "您的物品管理井井有条，继续保持！", Type.POSITIVE));
        }
        return list;
    }

    private static void checkHighDailyCost(String username, List<Insight> list) {
        List<Item> lt = ItemDao.findItemsByMode(username, ItemMode.LONG_TERM);
        for (Item it : lt) {
            long days = LongTermService.getDaysHeld(it);
            double daily = LongTermService.getDailyCost(it);
            if (daily > 50) {
                list.add(new Insight("高日均成本",
                        String.format("「%s」日均成本 ¥%.2f/天（已持有%d天），考虑物尽其用。",
                                it.getName(), daily, days), Type.WARNING));
            }
        }
        if (!lt.isEmpty()) {
            Item maxItem = lt.stream().max(Comparator.comparingDouble(LongTermService::getDailyCost)).get();
            double maxDaily = LongTermService.getDailyCost(maxItem);
            if (maxDaily < 20 && maxDaily > 0) {
                list.add(new Insight("长期物品使用良好",
                        String.format("长期物品日均成本均低于 ¥20/天，最贵的「%s」仅 ¥%.2f/天。",
                                maxItem.getName(), maxDaily), Type.POSITIVE));
            }
        }
    }

    private static void checkIdleItems(String username, List<Insight> list) {
        List<Item> idle = ItemDao.findItemsByMode(username, ItemMode.RECORD).stream()
                .filter(it -> "闲置".equals(it.getStatus()))
                .collect(Collectors.toList());
        if (!idle.isEmpty()) {
            List<String> names = idle.stream().map(Item::getName).collect(Collectors.toList());
            list.add(new Insight("闲置物品提醒",
                    "有 " + idle.size() + " 件物品处于闲置状态: " + String.join("、", names)
                            + "。考虑出售或转赠以减少浪费。", Type.INFO));
        }
    }

    private static void checkStockWarnings(String username, List<Insight> list) {
        List<String> warnings = StockpileService.checkLowStock(username);
        if (!warnings.isEmpty()) {
            list.add(new Insight("库存预警",
                    "以下物品库存不足，建议尽快补货: " + String.join("; ", warnings), Type.WARNING));
        }
    }

    private static void checkWarrantyExpiring(String username, List<Insight> list) {
        String today = DateUtil.getCurrentDate();
        for (Item it : ItemDao.findItemsByMode(username, ItemMode.RECORD)) {
            String exp = it.getWarrantyExpiry();
            if (exp != null && !exp.isEmpty()) {
                try {
                    long daysLeft = DateUtil.daysBetween(today, exp);
                    if (daysLeft >= 0 && daysLeft <= 30) {
                        list.add(new Insight("保修即将到期",
                                String.format("「%s」保修还剩 %d 天（到期日 %s），请留意。",
                                        it.getName(), daysLeft, exp), Type.WARNING));
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private static void checkCategoryConcentration(String username, List<Item> all, List<Insight> list) {
        Set<String> cats = all.stream().map(Item::getCategory)
                .filter(Objects::nonNull).filter(c -> !c.isEmpty())
                .collect(Collectors.toSet());
        if (cats.size() >= 8) {
            list.add(new Insight("分类丰富",
                    "您已经使用 " + cats.size() + " 种分类，物品管理很有条理！", Type.POSITIVE));
        }
    }

    private static void checkOverallSummary(String username, List<Item> all, List<Insight> list) {
        long total = all.size();
        double totalValue = all.stream().mapToDouble(Item::getPurchasePrice).sum();

        Map<ItemMode, Long> modeCounts = all.stream()
                .collect(Collectors.groupingBy(Item::getMode, Collectors.counting()));

        String summary = String.format("共 %d 件物品，总价值 ¥%.0f。", total, totalValue);
        if (modeCounts.containsKey(ItemMode.LONG_TERM))
            summary += String.format(" 长期物品 %d 件。", modeCounts.get(ItemMode.LONG_TERM));
        if (modeCounts.containsKey(ItemMode.STOCKPILE))
            summary += String.format(" 囤货物料 %d 件。", modeCounts.get(ItemMode.STOCKPILE));
        if (modeCounts.containsKey(ItemMode.RECORD))
            summary += String.format(" 记录物品 %d 件。", modeCounts.get(ItemMode.RECORD));

        list.add(new Insight("资产概况", summary, Type.INFO));
    }
}
