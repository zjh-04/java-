package service;

import dao.ItemDao;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import model.Consumption;
import model.Item;
import model.Item.ItemMode;
import model.PurchaseBatch;

/**
 * 囤货模式服务 — 多批次入库、均价计算、库存管理、消耗记录、预警。
 */
public class StockpileService {

    /**
     * 创建囤货物品（首次入库同时创建物品）
     */
    public static Item createItem(String username, String name, String category,
                                  int initialQty, double totalPrice, String date,
                                  double safetyStock, String notes) {
        Item item = new Item();
        item.setName(name);
        item.setMode(ItemMode.STOCKPILE);
        item.setCategory(category);
        item.setPurchasePrice(totalPrice);          // 初始总价，后续批次会稀释
        item.setPurchaseDate(date);
        item.setCurrentStock((double) initialQty);
        item.setSafetyStock(safetyStock);
        item.setNotes(notes);
        ItemDao.insertItem(username, item);

        // 记录首批入库
        PurchaseBatch batch = new PurchaseBatch(item.getId(), initialQty, totalPrice, date);
        ItemDao.insertBatch(username, batch);

        return item;
    }

    /**
     * 追加入库 — 新增一个采购批次
     */
    public static PurchaseBatch addBatch(String username, String itemId,
                                         int quantity, double totalPrice, String date) {
        PurchaseBatch batch = new PurchaseBatch(itemId, quantity, totalPrice, date);
        ItemDao.insertBatch(username, batch);
        recalcStock(username, itemId);
        return batch;
    }

    /**
     * 记录消耗
     */
    public static Consumption consume(String username, String itemId,
                                       int quantity, String date, String notes) {
        Consumption c = new Consumption(itemId, quantity, date, notes);
        ItemDao.insertConsumption(username, c);
        recalcStock(username, itemId);
        return c;
    }

    /**
     * 重新计算某物品当前库存，并更新 Item 中的缓存值
     */
    private static void recalcStock(String username, String itemId) {
        Item item = ItemDao.findItemById(username, itemId);
        if (item == null) return;

        int totalBought = ItemDao.findBatchesByItem(username, itemId).stream()
                .mapToInt(PurchaseBatch::getQuantity).sum();
        int totalConsumed = ItemDao.findConsumptionsByItem(username, itemId).stream()
                .mapToInt(Consumption::getQuantity).sum();

        item.setCurrentStock((double) (totalBought - totalConsumed));
        ItemDao.updateItem(username, item);
    }

    /**
     * 获取当前库存
     */
    public static double getCurrentStock(String username, String itemId) {
        Item item = ItemDao.findItemById(username, itemId);
        return item != null && item.getCurrentStock() != null ? item.getCurrentStock() : 0;
    }

    /**
     * 计算综合均价 = 所有批次总金额 / 所有批次总数量
     */
    public static double getAveragePrice(String username, String itemId) {
        List<PurchaseBatch> batches = ItemDao.findBatchesByItem(username, itemId);
        int totalQty = batches.stream().mapToInt(PurchaseBatch::getQuantity).sum();
        double totalPrice = batches.stream().mapToDouble(PurchaseBatch::getTotalPrice).sum();
        return totalQty > 0 ? totalPrice / totalQty : 0;
    }

    /**
     * 将外部单价与历史价格区间（史低/史高）对比，文案对齐 HTML 示例卡片的语气
     */
    public static String comparePrice(String username, String itemId, double externalUnitPrice) {
        List<PurchaseBatch> batches = ItemDao.findBatchesByItem(username, itemId);
        if (batches.isEmpty()) return "暂无历史购买记录，无法比较";

        double minUnit = batches.stream().mapToDouble(b -> b.getTotalPrice() / b.getQuantity()).min().orElse(0);
        double maxUnit = batches.stream().mapToDouble(b -> b.getTotalPrice() / b.getQuantity()).max().orElse(0);

        if (Math.abs(maxUnit - minUnit) < 0.001) {
            if (externalUnitPrice < minUnit) {
                return String.format("✅ 发现新史低！本次单价 ¥%.2f，比历史唯一价格 ¥%.2f 还低。", externalUnitPrice, minUnit);
            } else if (externalUnitPrice > minUnit) {
                return String.format("⚠ 刷新史高！本次单价 ¥%.2f，比历史价格 ¥%.2f 高。虽然多花了一点点，但早买早享受呀。", externalUnitPrice, minUnit);
            } else {
                return String.format("⚖️ 本次单价 ¥%.2f，与历史价格持平，稳稳的幸福。", externalUnitPrice);
            }
        }

        if (externalUnitPrice <= minUnit) {
            return String.format("✅ 很幸运，这次是在它最温柔的阶段（史低 ¥%.2f）遇到了它，省下的钱可以多喝一杯拿铁啦。", minUnit);
        } else if (externalUnitPrice >= maxUnit) {
            return String.format("⚠ 本次单价 ¥%.2f 触及史高 ¥%.2f。虽然多花了一点点，但它带来的快乐和便利是无法用金钱衡量的。", externalUnitPrice, maxUnit);
        } else {
            double pct = (externalUnitPrice - minUnit) / (maxUnit - minUnit) * 100;
            return String.format("📊 本次单价 ¥%.2f，处于历史价格区间 [¥%.2f – ¥%.2f] 的 %.0f%% 位置，属于平稳波动。", externalUnitPrice, minUnit, maxUnit, pct);
        }
    }

    /**
     * 检查所有囤货物品的库存预警
     * @return 库存不足的物品列表，每个元素是 [物品名, 当前库存, 安全阈值]
     */
    public static List<String> checkLowStock(String username) {
        List<String> warnings = new ArrayList<>();
        for (Item item : ItemDao.findItemsByMode(username, ItemMode.STOCKPILE)) {
            double stock = item.getCurrentStock() != null ? item.getCurrentStock() : 0;
            double safety = item.getSafetyStock() != null ? item.getSafetyStock() : 0;
            if (stock <= safety) {
                warnings.add(String.format("  ⚠ %s [%s] | 库存: %.0f | 安全线: %.0f | 建议补货",
                        item.getName(), item.getId(), stock, safety));
            }
        }
        return warnings;
    }

    /**
     * 获取某物品的所有采购批次，按日期降序
     */
    public static List<PurchaseBatch> getBatches(String username, String itemId) {
        List<PurchaseBatch> list = ItemDao.findBatchesByItem(username, itemId);
        list.sort(Comparator.comparing(PurchaseBatch::getPurchaseDate).reversed());
        return list;
    }

    /**
     * 获取某物品的所有消耗记录，按日期降序
     */
    public static List<Consumption> getConsumptions(String username, String itemId) {
        List<Consumption> list = ItemDao.findConsumptionsByItem(username, itemId);
        list.sort(Comparator.comparing(Consumption::getDate).reversed());
        return list;
    }

    /**
     * 格式化输出物品详情（囤货视角）
     */
    public static String formatDetail(String username, Item item) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("  物品: %s\n", item.getName()));
        sb.append(String.format("  分类: %s\n", item.getCategory() != null ? item.getCategory() : "未分类"));
        sb.append(String.format("  首次购买: %s\n", item.getPurchaseDate()));

        double stock = getCurrentStock(username, item.getId());
        double avgPrice = getAveragePrice(username, item.getId());
        double safety = item.getSafetyStock() != null ? item.getSafetyStock() : 0;

        sb.append(String.format("  当前库存: %.0f\n", stock));
        sb.append(String.format("  安全库存线: %.0f\n", safety));
        sb.append(String.format("  综合均价: ¥%.2f / 单位\n", avgPrice));

        if (stock <= safety && stock > 0) {
            sb.append("  ⚠ 库存不足，建议补货！\n");
        } else if (stock == 0) {
            sb.append("  ‼ 已用尽，请尽快补货！\n");
        }

        // 采购批次
        List<PurchaseBatch> batches = getBatches(username, item.getId());
        if (!batches.isEmpty()) {
            sb.append("  采购记录:\n");
            for (PurchaseBatch pb : batches) {
                sb.append(String.format("    - %s: %d件 ¥%.2f (单价¥%.2f)\n",
                        pb.getPurchaseDate(), pb.getQuantity(), pb.getTotalPrice(), pb.getUnitPrice()));
            }
        }

        // 最近消耗
        List<Consumption> consumes = getConsumptions(username, item.getId());
        if (!consumes.isEmpty()) {
            sb.append("  最近消耗:\n");
            for (int i = 0; i < Math.min(5, consumes.size()); i++) {
                Consumption c = consumes.get(i);
                sb.append(String.format("    - %s: %d件 %s\n",
                        c.getDate(), c.getQuantity(),
                        c.getNotes() != null ? "(" + c.getNotes() + ")" : ""));
            }
        }

        if (item.getNotes() != null && !item.getNotes().isEmpty()) {
            sb.append(String.format("  备注: %s\n", item.getNotes()));
        }
        return sb.toString();
    }
}
