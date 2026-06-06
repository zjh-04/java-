package com.guicang.model;

/**
 * 囤货采购批次实体
 */
public class PurchaseBatch {
    private String id;
    private String assetId;
    private int    quantity;
    private double totalPrice;
    private String batchDate;

    public PurchaseBatch() {
        this.id = java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    public PurchaseBatch(String assetId, int quantity, double totalPrice, String batchDate) {
        this();
        this.assetId = assetId;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.batchDate = batchDate;
    }

    /** 单价 = 总金额 / 数量 */
    public double getUnitPrice() {
        return quantity > 0 ? totalPrice / quantity : 0;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
    public String getBatchDate() { return batchDate; }
    public void setBatchDate(String batchDate) { this.batchDate = batchDate; }
}
