package model;

import java.io.Serializable;
import java.util.UUID;

/**
 * 囤货采购批次 — 记录每次补货的数量和金额，用于计算综合均价。
 */
public class PurchaseBatch implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String itemId;       // 关联物品 ID
    private int    quantity;     // 本次购买数量
    private double totalPrice;   // 本次总金额
    private String purchaseDate; // yyyy-MM-dd

    public PurchaseBatch() {
        this.id = UUID.randomUUID().toString().substring(0, 8);
    }

    public PurchaseBatch(String itemId, int quantity, double totalPrice, String purchaseDate) {
        this();
        this.itemId = itemId;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.purchaseDate = purchaseDate;
    }

    /* ==================== Getter / Setter ==================== */

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public String getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(String purchaseDate) { this.purchaseDate = purchaseDate; }

    /** 单价 */
    public double getUnitPrice() {
        return quantity > 0 ? totalPrice / quantity : 0;
    }

    @Override
    public String toString() {
        return String.format("批次[%s] %d件 ¥%.2f (单价¥%.2f) %s",
                id, quantity, totalPrice, getUnitPrice(), purchaseDate);
    }
}
