package model;

import java.io.Serializable;
import java.util.UUID;

/**
 * 物品实体 — 核心数据模型。
 * 单一类 + 模式枚举：模式专属字段在不适用的模式下为 null。
 */
public class Item implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum ItemMode {
        LONG_TERM("长期主义"),
        STOCKPILE("囤货模式"),
        RECORD("纯记录");

        private final String displayName;
        ItemMode(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    /* ---------- 通用字段 ---------- */
    private String  id;
    private String  name;
    private ItemMode mode;
    private String  category;
    private double  purchasePrice;
    private String  purchaseDate;      // yyyy-MM-dd
    private String  notes;

    /* ---------- 长期主义专属 ---------- */
    private Integer expectedLifespanYears;  // 预期使用年限

    /* ---------- 囤货模式专属 ---------- */
    private Double  currentStock;   // 当前库存（由批次-消耗计算，此处缓存）
    private Double  safetyStock;    // 安全库存阈值

    /* ---------- 纯记录专属 ---------- */
    private String  status;         // 在用 / 闲置 / 已售 / 已丢
    private String  serialNumber;   // 序列号
    private String  warrantyExpiry; // 保修到期日

    public Item() {
        this.id = UUID.randomUUID().toString().substring(0, 8);
    }

    /* ==================== Getter / Setter ==================== */

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ItemMode getMode() { return mode; }
    public void setMode(ItemMode mode) { this.mode = mode; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(double purchasePrice) { this.purchasePrice = purchasePrice; }

    public String getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(String purchaseDate) { this.purchaseDate = purchaseDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Integer getExpectedLifespanYears() { return expectedLifespanYears; }
    public void setExpectedLifespanYears(Integer expectedLifespanYears) { this.expectedLifespanYears = expectedLifespanYears; }

    public Double getCurrentStock() { return currentStock; }
    public void setCurrentStock(Double currentStock) { this.currentStock = currentStock; }

    public Double getSafetyStock() { return safetyStock; }
    public void setSafetyStock(Double safetyStock) { this.safetyStock = safetyStock; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getWarrantyExpiry() { return warrantyExpiry; }
    public void setWarrantyExpiry(String warrantyExpiry) { this.warrantyExpiry = warrantyExpiry; }

    @Override
    public String toString() {
        return String.format("[%s] %s | ¥%.2f | %s", mode.getDisplayName(), name, purchasePrice, purchaseDate);
    }
}
