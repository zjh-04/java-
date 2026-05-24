package model;

import java.io.Serializable;
import java.util.UUID;

/**
 * 囤货消耗记录 — 记录每次使用的数量，用于扣减库存。
 */
public class Consumption implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String itemId;   // 关联物品 ID
    private int    quantity;  // 消耗数量
    private String date;      // yyyy-MM-dd
    private String notes;     // 备注

    public Consumption() {
        this.id = UUID.randomUUID().toString().substring(0, 8);
    }

    public Consumption(String itemId, int quantity, String date, String notes) {
        this();
        this.itemId = itemId;
        this.quantity = quantity;
        this.date = date;
        this.notes = notes;
    }

    /* ==================== Getter / Setter ==================== */

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @Override
    public String toString() {
        return String.format("消耗 %d件 | %s %s", quantity, date,
                notes != null && !notes.isEmpty() ? "(" + notes + ")" : "");
    }
}
