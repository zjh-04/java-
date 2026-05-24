package dao;

import java.util.*;
import java.util.stream.Collectors;
import model.Item;
import model.Item.ItemMode;
import model.PurchaseBatch;
import model.Consumption;
import util.FileDataUtil;

/**
 * 物品数据访问层 — 统一管理 Item / PurchaseBatch / Consumption 的持久化。
 *
 * 文件格式：
 *   data/items_{username}.dat         — 物品
 *   data/batches_{username}.dat       — 采购批次
 *   data/consumptions_{username}.dat  — 消耗记录
 */
public class ItemDao {

    private static final String ITEMS_PREFIX  = "items_";
    private static final String BATCHES_PREFIX = "batches_";
    private static final String CONSUME_PREFIX = "consumptions_";
    private static final String SUFFIX = ".dat";

    /* ==================== 缓存 ==================== */
    private static final Map<String, List<Item>>         itemsCache  = new HashMap<>();
    private static final Map<String, List<PurchaseBatch>> batchCache  = new HashMap<>();
    private static final Map<String, List<Consumption>>   consumeCache = new HashMap<>();

    // ======================== Item ========================

    private static void ensureItems(String username) {
        if (!itemsCache.containsKey(username)) loadItems(username);
    }

    private static void loadItems(String username) {
        List<Item> list = new ArrayList<>();
        for (String line : FileDataUtil.readLines(ITEMS_PREFIX + username + SUFFIX)) {
            String[] f = FileDataUtil.parseFields(line);
            if (f.length >= 7) {
                Item it = new Item();
                it.setId(f[0]);
                it.setName(f[1]);
                it.setMode(ItemMode.valueOf(f[2]));
                it.setCategory(f[3]);
                it.setPurchasePrice(Double.parseDouble(f[4]));
                it.setPurchaseDate(f[5]);
                it.setNotes(f[6].isEmpty() ? null : f[6]);
                if (f.length >= 8 && !f[7].isEmpty()) it.setExpectedLifespanYears(Integer.parseInt(f[7]));
                if (f.length >= 9 && !f[8].isEmpty()) it.setCurrentStock(Double.parseDouble(f[8]));
                if (f.length >= 10 && !f[9].isEmpty()) it.setSafetyStock(Double.parseDouble(f[9]));
                if (f.length >= 11) it.setStatus(f[10].isEmpty() ? null : f[10]);
                if (f.length >= 12) it.setSerialNumber(f[11].isEmpty() ? null : f[11]);
                if (f.length >= 13) it.setWarrantyExpiry(f[12].isEmpty() ? null : f[12]);
                list.add(it);
            }
        }
        itemsCache.put(username, list);
    }

    private static void saveItems(String username) {
        List<String> lines = new ArrayList<>();
        for (Item it : itemsCache.getOrDefault(username, Collections.emptyList())) {
            lines.add(FileDataUtil.joinFields(
                    it.getId(),
                    it.getName(),
                    it.getMode().name(),
                    it.getCategory() != null ? it.getCategory() : "",
                    String.valueOf(it.getPurchasePrice()),
                    it.getPurchaseDate(),
                    it.getNotes() != null ? it.getNotes() : "",
                    it.getExpectedLifespanYears() != null ? String.valueOf(it.getExpectedLifespanYears()) : "",
                    it.getCurrentStock() != null ? String.valueOf(it.getCurrentStock()) : "",
                    it.getSafetyStock() != null ? String.valueOf(it.getSafetyStock()) : "",
                    it.getStatus() != null ? it.getStatus() : "",
                    it.getSerialNumber() != null ? it.getSerialNumber() : "",
                    it.getWarrantyExpiry() != null ? it.getWarrantyExpiry() : ""
            ));
        }
        FileDataUtil.writeLines(ITEMS_PREFIX + username + SUFFIX, lines);
    }

    public static List<Item> findItemsByUser(String username) {
        ensureItems(username);
        return new ArrayList<>(itemsCache.get(username));
    }

    public static List<Item> findItemsByMode(String username, ItemMode mode) {
        return findItemsByUser(username).stream()
                .filter(it -> it.getMode() == mode)
                .collect(Collectors.toList());
    }

    public static Item findItemById(String username, String id) {
        ensureItems(username);
        return itemsCache.get(username).stream()
                .filter(it -> it.getId().equals(id)).findFirst().orElse(null);
    }

    public static void insertItem(String username, Item item) {
        ensureItems(username);
        itemsCache.get(username).add(item);
        saveItems(username);
    }

    public static void updateItem(String username, Item updated) {
        ensureItems(username);
        List<Item> list = itemsCache.get(username);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(updated.getId())) {
                list.set(i, updated);
                break;
            }
        }
        saveItems(username);
    }

    public static void deleteItem(String username, String id) {
        ensureItems(username);
        itemsCache.get(username).removeIf(it -> it.getId().equals(id));
        saveItems(username);
        // 级联删除关联的批次和消耗记录
        deleteBatchesByItem(username, id);
        deleteConsumptionsByItem(username, id);
    }

    // ======================== PurchaseBatch ========================

    private static void ensureBatches(String username) {
        if (!batchCache.containsKey(username)) loadBatches(username);
    }

    private static void loadBatches(String username) {
        List<PurchaseBatch> list = new ArrayList<>();
        for (String line : FileDataUtil.readLines(BATCHES_PREFIX + username + SUFFIX)) {
            String[] f = FileDataUtil.parseFields(line);
            if (f.length >= 5) {
                PurchaseBatch pb = new PurchaseBatch();
                pb.setId(f[0]);
                pb.setItemId(f[1]);
                pb.setQuantity(Integer.parseInt(f[2]));
                pb.setTotalPrice(Double.parseDouble(f[3]));
                pb.setPurchaseDate(f[4]);
                list.add(pb);
            }
        }
        batchCache.put(username, list);
    }

    private static void saveBatches(String username) {
        List<String> lines = new ArrayList<>();
        for (PurchaseBatch pb : batchCache.getOrDefault(username, Collections.emptyList())) {
            lines.add(FileDataUtil.joinFields(
                    pb.getId(), pb.getItemId(),
                    String.valueOf(pb.getQuantity()),
                    String.valueOf(pb.getTotalPrice()),
                    pb.getPurchaseDate()
            ));
        }
        FileDataUtil.writeLines(BATCHES_PREFIX + username + SUFFIX, lines);
    }

    public static List<PurchaseBatch> findBatchesByItem(String username, String itemId) {
        ensureBatches(username);
        return batchCache.get(username).stream()
                .filter(pb -> pb.getItemId().equals(itemId))
                .collect(Collectors.toList());
    }

    public static void insertBatch(String username, PurchaseBatch batch) {
        ensureBatches(username);
        batchCache.get(username).add(batch);
        saveBatches(username);
    }

    public static void deleteBatchesByItem(String username, String itemId) {
        ensureBatches(username);
        batchCache.get(username).removeIf(pb -> pb.getItemId().equals(itemId));
        saveBatches(username);
    }

    // ======================== Consumption ========================

    private static void ensureConsumptions(String username) {
        if (!consumeCache.containsKey(username)) loadConsumptions(username);
    }

    private static void loadConsumptions(String username) {
        List<Consumption> list = new ArrayList<>();
        for (String line : FileDataUtil.readLines(CONSUME_PREFIX + username + SUFFIX)) {
            String[] f = FileDataUtil.parseFields(line);
            if (f.length >= 4) {
                Consumption c = new Consumption();
                c.setId(f[0]);
                c.setItemId(f[1]);
                c.setQuantity(Integer.parseInt(f[2]));
                c.setDate(f[3]);
                c.setNotes(f.length >= 5 ? (f[4].isEmpty() ? null : f[4]) : null);
                list.add(c);
            }
        }
        consumeCache.put(username, list);
    }

    private static void saveConsumptions(String username) {
        List<String> lines = new ArrayList<>();
        for (Consumption c : consumeCache.getOrDefault(username, Collections.emptyList())) {
            lines.add(FileDataUtil.joinFields(
                    c.getId(), c.getItemId(),
                    String.valueOf(c.getQuantity()),
                    c.getDate(),
                    c.getNotes() != null ? c.getNotes() : ""
            ));
        }
        FileDataUtil.writeLines(CONSUME_PREFIX + username + SUFFIX, lines);
    }

    public static List<Consumption> findConsumptionsByItem(String username, String itemId) {
        ensureConsumptions(username);
        return consumeCache.get(username).stream()
                .filter(c -> c.getItemId().equals(itemId))
                .collect(Collectors.toList());
    }

    public static void insertConsumption(String username, Consumption consumption) {
        ensureConsumptions(username);
        consumeCache.get(username).add(consumption);
        saveConsumptions(username);
    }

    public static void deleteConsumptionsByItem(String username, String itemId) {
        ensureConsumptions(username);
        consumeCache.get(username).removeIf(c -> c.getItemId().equals(itemId));
        saveConsumptions(username);
    }

    // ======================== 通用 ========================

    public static void refreshItems(String username) {
        itemsCache.remove(username);
        batchCache.remove(username);
        consumeCache.remove(username);
    }
}
