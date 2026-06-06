package com.guicang.service;

import com.guicang.model.Asset;
import com.guicang.repository.AssetRepository;

import java.util.List;

/**
 * 资产通用 CRUD 服务
 */
public class AssetService {

    private final AssetRepository assetRepo = new AssetRepository();

    public List<Asset> getAll(int userId) { return assetRepo.findByUser(userId); }
    public List<Asset> getByType(int userId, String assetType) { return assetRepo.findByUserAndType(userId, assetType); }
    public List<Asset> getArchived(int userId) { return assetRepo.findArchived(userId); }
    public Asset getById(String id) { return assetRepo.findById(id); }

    public Asset create(Asset asset) {
        if (asset.getCreatedAt() == null) asset.setCreatedAt(java.time.LocalDate.now().toString());
        return assetRepo.insert(asset);
    }

    public Asset update(String id, Asset updated) {
        Asset existing = assetRepo.findById(id);
        if (existing == null) throw new RuntimeException("资产不存在");

        // 增量更新：只覆盖前端传来的非空字段（assetType 不可改，不在此列）
        if (updated.getName() != null && !updated.getName().isEmpty()) existing.setName(updated.getName());
        if (updated.getIcon() != null) existing.setIcon(updated.getIcon());
        if (updated.getCategory() != null && !updated.getCategory().isEmpty()) existing.setCategory(updated.getCategory());
        if (updated.getPurchasePrice() >= 0) existing.setPurchasePrice(updated.getPurchasePrice());
        if (updated.getPurchaseDate() != null && !updated.getPurchaseDate().isEmpty()) existing.setPurchaseDate(updated.getPurchaseDate());
        if (updated.getNotes() != null) existing.setNotes(updated.getNotes());
        // 囤货
        if (updated.getCurrentStock() != null) existing.setCurrentStock(updated.getCurrentStock());
        if (updated.getSafetyStock() != null) existing.setSafetyStock(updated.getSafetyStock());
        // 收藏
        if (updated.getCollectStatus() != null) existing.setCollectStatus(updated.getCollectStatus());
        // 订阅
        if (updated.getBillingCycle() != null) existing.setBillingCycle(updated.getBillingCycle());
        if (updated.getMonthlyCost() != null) existing.setMonthlyCost(updated.getMonthlyCost());
        if (updated.getNextBillingDate() != null) existing.setNextBillingDate(updated.getNextBillingDate());
        // 按量计费
        if (updated.getApiBalance() != null) existing.setApiBalance(updated.getApiBalance());
        if (updated.getTotalCharged() != null) existing.setTotalCharged(updated.getTotalCharged());
        // 储值次卡
        if (updated.getRemainingTimes() != null) existing.setRemainingTimes(updated.getRemainingTimes());
        if (updated.getTotalTimes() != null) existing.setTotalTimes(updated.getTotalTimes());
        // 储值通用
        if (updated.getTotalTopup() != null) existing.setTotalTopup(updated.getTotalTopup());
        // 储值量卡
        if (updated.getCardBalance() != null) existing.setCardBalance(updated.getCardBalance());
        if (updated.getTotalSpent() != null) existing.setTotalSpent(updated.getTotalSpent());

        assetRepo.update(existing);

        // 写入更新历程
        new com.guicang.repository.UsageLogRepository().insert(
            new com.guicang.model.UsageLog(id, "UPDATE_META", 0, 0,
                "修改了「" + existing.getName() + "」的元数据", com.guicang.util.DateUtil.today()));

        return existing;
    }

    public void delete(String id) { assetRepo.delete(id); }
    public void archive(String id, boolean archived) { assetRepo.archive(id, archived); }
    public int countByType(int userId, String assetType) {
        return assetRepo.findByUserAndType(userId, assetType).size();
    }
}
