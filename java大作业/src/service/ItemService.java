package service;

import dao.ItemDao;
import java.util.List;
import model.Item;
import model.Item.ItemMode;

/**
 * 物品通用服务 — 跨模式的 CRUD 和查询。
 */
public class ItemService {

    public static List<Item> getAllItems(String username) {
        return ItemDao.findItemsByUser(username);
    }

    public static List<Item> getItemsByMode(String username, ItemMode mode) {
        return ItemDao.findItemsByMode(username, mode);
    }

    public static Item getItemById(String username, String id) {
        return ItemDao.findItemById(username, id);
    }

    public static void deleteItem(String username, String id) {
        ItemDao.deleteItem(username, id);
    }

    /** 列出某个模式下的物品简要信息，返回格式化字符串列表 */
    public static List<String> listItemSummaries(String username, ItemMode mode) {
        List<Item> items = ItemDao.findItemsByMode(username, mode);
        List<String> result = new java.util.ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            Item it = items.get(i);
            result.add(String.format("  %d. [%s] %s | ¥%.2f | %s",
                    i + 1, it.getId(), it.getName(), it.getPurchasePrice(), it.getPurchaseDate()));
        }
        return result;
    }

    /** 获取某模式下的物品数量 */
    public static int countByMode(String username, ItemMode mode) {
        return ItemDao.findItemsByMode(username, mode).size();
    }
}
