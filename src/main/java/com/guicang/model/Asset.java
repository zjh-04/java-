package com.guicang.model;

/**
 * 统一资产实体 — 11 种 asset_type 共用单表，专属字段可 NULL
 */
public class Asset {

    public enum AssetType {
        LONG_TERM_PER_USE,
        LONG_TERM_PER_DAY,
        STOCKPILE,
        COLLECTIBLE,
        SUBSCRIPTION_MONTHLY,
        SUBSCRIPTION_QUARTERLY,
        SUBSCRIPTION_YEARLY,
        SUBSCRIPTION_METERED,
        SUBSCRIPTION_LIFETIME,
        STORED_TIME_CARD,
        STORED_AMOUNT_CARD
    }

    /* ===== 通用字段 ===== */
    private String  id;
    private int     userId;
    private String  assetType;     // AssetType.name()
    private String  name;
    private String  icon;
    private String  category;
    private double  purchasePrice;
    private String  purchaseDate;
    private String  notes;

    /* ===== 细水长流·按次 ===== */
    private Integer usageCount;

    /* ===== 细水长流·按天 ===== */
    private Integer expectedLifespanYears;

    /* ===== 储备幸福 ===== */
    private Double  currentStock;
    private Double  safetyStock;

    /* ===== 收藏状态 ===== */
    private String  collectStatus;

    /* ===== 周期续费 ===== */
    private String  billingCycle;
    private Double  monthlyCost;
    private String  nextBillingDate;

    /* ===== 按量计费 ===== */
    private Double  apiBalance;
    private Double  totalCharged;

    /* ===== 储值次卡 ===== */
    private Integer remainingTimes;
    private Integer totalTimes;

    /* ===== 储值量卡 + 次卡 ===== */
    private Double  totalTopup;

    /* ===== 次卡专属 ===== */
    private Integer cumulativePurchased;

    /* ===== 储值量卡 ===== */
    private Double  cardBalance;
    private Double  totalSpent;

    /* ===== 通用状态 ===== */
    private int     isArchived;
    private String  createdAt;
    private String  updatedAt;

    // ===== 派生字段 (不存库，Service 计算后设值；不用 transient，否则 Gson 序列化到前端时会被跳过) =====
    private long   daysHeld;
    private double dailyCost;
    private double costRatio;
    private String statusTagVariant;   // success / warning / danger / ""
    private String statusMessageKey;

    public Asset() {}

    // ========== Getter / Setter (省略中间部分，全部生成) ==========

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(double purchasePrice) { this.purchasePrice = purchasePrice; }

    public String getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(String purchaseDate) { this.purchaseDate = purchaseDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Integer getUsageCount() { return usageCount; }
    public void setUsageCount(Integer usageCount) { this.usageCount = usageCount; }

    public Integer getExpectedLifespanYears() { return expectedLifespanYears; }
    public void setExpectedLifespanYears(Integer expectedLifespanYears) { this.expectedLifespanYears = expectedLifespanYears; }

    public Double getCurrentStock() { return currentStock; }
    public void setCurrentStock(Double currentStock) { this.currentStock = currentStock; }

    public Double getSafetyStock() { return safetyStock; }
    public void setSafetyStock(Double safetyStock) { this.safetyStock = safetyStock; }

    public String getCollectStatus() { return collectStatus; }
    public void setCollectStatus(String collectStatus) { this.collectStatus = collectStatus; }

    public String getBillingCycle() { return billingCycle; }
    public void setBillingCycle(String billingCycle) { this.billingCycle = billingCycle; }

    public Double getMonthlyCost() { return monthlyCost; }
    public void setMonthlyCost(Double monthlyCost) { this.monthlyCost = monthlyCost; }

    public String getNextBillingDate() { return nextBillingDate; }
    public void setNextBillingDate(String nextBillingDate) { this.nextBillingDate = nextBillingDate; }

    public Double getApiBalance() { return apiBalance; }
    public void setApiBalance(Double apiBalance) { this.apiBalance = apiBalance; }

    public Double getTotalCharged() { return totalCharged; }
    public void setTotalCharged(Double totalCharged) { this.totalCharged = totalCharged; }

    public Integer getRemainingTimes() { return remainingTimes; }
    public void setRemainingTimes(Integer remainingTimes) { this.remainingTimes = remainingTimes; }

    public Integer getTotalTimes() { return totalTimes; }
    public void setTotalTimes(Integer totalTimes) { this.totalTimes = totalTimes; }

    public Integer getCumulativePurchased() { return cumulativePurchased; }
    public void setCumulativePurchased(Integer cumulativePurchased) { this.cumulativePurchased = cumulativePurchased; }

    public Double getTotalTopup() { return totalTopup; }
    public void setTotalTopup(Double totalTopup) { this.totalTopup = totalTopup; }

    public Double getCardBalance() { return cardBalance; }
    public void setCardBalance(Double cardBalance) { this.cardBalance = cardBalance; }

    public Double getTotalSpent() { return totalSpent; }
    public void setTotalSpent(Double totalSpent) { this.totalSpent = totalSpent; }

    public int getIsArchived() { return isArchived; }
    public void setIsArchived(int isArchived) { this.isArchived = isArchived; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    // ===== 派生字段 =====
    public long getDaysHeld() { return daysHeld; }
    public void setDaysHeld(long daysHeld) { this.daysHeld = daysHeld; }

    public double getDailyCost() { return dailyCost; }
    public void setDailyCost(double dailyCost) { this.dailyCost = dailyCost; }

    public double getCostRatio() { return costRatio; }
    public void setCostRatio(double costRatio) { this.costRatio = costRatio; }

    public String getStatusTagVariant() { return statusTagVariant; }
    public void setStatusTagVariant(String statusTagVariant) { this.statusTagVariant = statusTagVariant; }

    public String getStatusMessageKey() { return statusMessageKey; }
    public void setStatusMessageKey(String statusMessageKey) { this.statusMessageKey = statusMessageKey; }
}
