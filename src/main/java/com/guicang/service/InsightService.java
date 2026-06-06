package com.guicang.service;

import com.guicang.model.Asset;
import com.guicang.repository.AssetRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能洞察服务 — 后端计算，返回结构化结果
 */
public class InsightService {

    private final AssetRepository assetRepo = new AssetRepository();
    private final LongTermService ltService = new LongTermService();
    private final SubscriptionService subService = new SubscriptionService();

    public List<Map<String, Object>> generate(int userId) {
        List<Map<String, Object>> list = new ArrayList<>();
        List<Asset> all = assetRepo.findByUser(userId);

        if (all.isEmpty()) {
            list.add(insight("info", "开始记录吧", "还没有任何物品记录，开始添加第一个物品吧！"));
            return list;
        }

        // 1. 高日均成本预警（按天+按次，日均 > 50）
        all.stream()
                .filter(a -> "LONG_TERM_PER_USE".equals(a.getAssetType()) || "LONG_TERM_PER_DAY".equals(a.getAssetType()))
                .filter(a -> {
                    long days = ltService.getDaysHeld(a);
                    return a.getPurchasePrice() / Math.max(days, 1) > 50;
                })
                .forEach(a -> {
                    long days = ltService.getDaysHeld(a);
                    double daily = a.getPurchasePrice() / Math.max(days, 1);
                    list.add(insight("warning", "让它多陪陪你",
                            String.format("「%s」日均成本 ¥%.0f / 天，才陪了 %d 天。多拉它出来遛遛，成本自然就薄了。", a.getName(), daily, days)));
                });

        // 2. 库存预警
        all.stream()
                .filter(a -> "STOCKPILE".equals(a.getAssetType()))
                .filter(a -> {
                    double s = a.getCurrentStock() != null ? a.getCurrentStock() : 0;
                    double sf = a.getSafetyStock() != null ? a.getSafetyStock() : 0;
                    return s <= sf;
                })
                .forEach(a -> list.add(insight("warning", "囤货见底了",
                        String.format("「%s」只剩 %.0f 件（安全线 %.0f），趁还没用完，记得补一批。",
                                a.getName(), a.getCurrentStock(), a.getSafetyStock()))));

        // 3. 最近续费提醒：找剩余天数最少的那笔订阅，给温暖提醒
        all.stream()
                .filter(a -> a.getNextBillingDate() != null)
                .filter(a -> a.getAssetType() != null && a.getAssetType().startsWith("SUBSCRIPTION_")
                        && !"SUBSCRIPTION_METERED".equals(a.getAssetType())
                        && !"SUBSCRIPTION_LIFETIME".equals(a.getAssetType()))
                .min(Comparator.comparingLong(a -> subService.getRemainingDays(a)))
                .ifPresent(a -> {
                    long remaining = subService.getRemainingDays(a);
                    if (remaining <= 7) {
                        String cycle = a.getBillingCycle() != null && a.getBillingCycle().equals("YEARLY") ? "年"
                                : a.getBillingCycle() != null && a.getBillingCycle().equals("QUARTERLY") ? "季" : "月";
                        double cost = a.getMonthlyCost() != null ? a.getMonthlyCost() : 0;
                        list.add(insight("warning", "续费倒计时",
                                String.format("「%s」%d 天后就要续费 ¥%.0f / %s了。如果最近打开得少了，不妨先暂停一阵子，钱和注意力都省下来。",
                                        a.getName(), remaining, cost, cycle)));
                    }
                });

        // 4. 资产概况（根据规模给出不同评价）
        int count = all.size();
        double totalValue = all.stream().mapToDouble(a -> {
            double v = a.getPurchasePrice();
            if (a.getTotalTopup() != null) v += a.getTotalTopup();
            if (a.getTotalCharged() != null) v += a.getTotalCharged();
            return v;
        }).sum();
        long cats = all.stream().map(Asset::getCategory).filter(Objects::nonNull).distinct().count();

        String assessment;
        if (count <= 3) {
            assessment = "归藏之路刚刚起步，每一件物品都值得被认真对待。";
        } else if (count <= 10) {
            assessment = "已经有了 " + count + " 件宝贝，生活正在被你温柔地整理着。";
        } else if (count <= 20) {
            assessment = count + " 件物品，总价值 ¥" + String.format("%.0f", totalValue) + "。你对自己的所有物了然于胸，这本身就是一种笃定。";
        } else {
            assessment = count + " 件物品，横跨 " + cats + " 个分类，总值 ¥" + String.format("%.0f", totalValue) + "。你不是在囤积，你是在经营一座小型生活博物馆。";
        }
        if (cats >= 5 && count > 3) {
            assessment += " " + cats + " 种分类打理得井井有条，眼光确实够广。";
        }
        list.add(insight("info", "生活资产小结", assessment));

        return list;
    }

    private Map<String, Object> insight(String type, String title, String content) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        m.put("title", title);
        m.put("content", content);
        return m;
    }
}
