package com.guicang.model;

/**
 * 操作流水实体 — 只追加不修改
 */
public class UsageLog {
    private int    id;
    private String assetId;
    private String actionType;   // CHECK_IN|CONSUME|PUNCH|TOPUP|SPEND|RESTOCK|STATUS_CHANGE
    private double quantity;
    private double unitPrice;
    private String notes;
    private String createdAt;

    public UsageLog() {}

    public UsageLog(String assetId, String actionType, double quantity, double unitPrice, String notes, String createdAt) {
        this.assetId = assetId;
        this.actionType = actionType;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
