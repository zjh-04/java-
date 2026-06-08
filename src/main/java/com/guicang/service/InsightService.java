package com.guicang.service;

import com.guicang.model.Asset;
import com.guicang.repository.AssetRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能洞察服务 — 后端计算，返回结构化结果
 * 关键字段用 &lt;span class=&quot;hl&quot;&gt; 包裹，前端 v-html 渲染高亮
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

        // 1a. 按次物品 — 用了多少次？
        all.stream()
                .filter(a -> "LONG_TERM_PER_USE".equals(a.getAssetType()))
                .forEach(a -> {
                    long days = ltService.getDaysHeld(a);
                    int uc = a.getUsageCount() != null ? a.getUsageCount() : 0;
                    double cpu = uc > 0 ? a.getPurchasePrice() / uc : a.getPurchasePrice();
                    double ratio = a.getPurchasePrice() > 0 ? cpu / a.getPurchasePrice() : 1;

                    if (uc == 0 && days > 30) {
                        list.add(insight("info", "第一次还没发生",
                                String.format("「%s」来家里 %s 天了，还没派上过用场。不着急，等一个对的日子。",
                                        hl(a.getName()), hl(String.valueOf(days)))));
                    } else if (uc == 1) {
                        list.add(insight("success", "第一次相遇",
                                String.format("「%s」完成了第一次陪伴，现在单次成本 %s。有了开始，故事就慢慢展开了。",
                                        hl(a.getName()), hl("¥" + String.format("%.0f", cpu)))));
                    } else if (ratio <= 0.10 && uc >= 5) {
                        list.add(insight("success", "早已值回票价",
                                String.format("「%s」陪了你 %s 次，单次只需 %s。当初的价格早就被一次次相遇稀释成了一段段值得的回忆。",
                                        hl(a.getName()), hl(String.valueOf(uc)), hl("¥" + String.format("%.0f", cpu)))));
                    } else if (ratio <= 0.30 && uc >= 3) {
                        list.add(insight("success", "越用越轻盈",
                                String.format("「%s」单次成本已降至 %s，只有原价的 %d%%。它在用每一次陪伴回应你的选择。",
                                        hl(a.getName()), hl("¥" + String.format("%.0f", cpu)), hl(String.valueOf((int) (ratio * 100))))));
                    }
                });

        // 1b. 按天物品 — 陪了多少天？
        all.stream()
                .filter(a -> "LONG_TERM_PER_DAY".equals(a.getAssetType()))
                .forEach(a -> {
                    long days = ltService.getDaysHeld(a);
                    double daily = a.getPurchasePrice() / Math.max(days, 1);

                    if (days < 30) {
                        list.add(insight("info", "初见不久",
                                String.format("「%s」才来了 %s 天，还在慢慢融入你的日常。好的陪伴都是细水长流的。",
                                        hl(a.getName()), hl(String.valueOf(days)))));
                    } else if (days >= 30 && days < 100 && daily > 20) {
                        list.add(insight("info", "慢慢熟悉中",
                                String.format("「%s」陪你走过了 %s 天，日均 %s。还早呢，让时间把它酿成生活里的一小杯温热。",
                                        hl(a.getName()), hl(String.valueOf(days)), hl("¥" + String.format("%.0f", daily)))));
                    } else if (days >= 365) {
                        list.add(insight("success", "一年如一日",
                                String.format("「%s」已经陪你 %s 天了。那些日复一日的存在，就是生活里最安静也最笃定的部分。",
                                        hl(a.getName()), hl(String.valueOf(days)))));
                    }
                });

        // 2. 库存预警
        all.stream()
                .filter(a -> "STOCKPILE".equals(a.getAssetType()))
                .filter(a -> {
                    double s = a.getCurrentStock() != null ? a.getCurrentStock() : 0;
                    double sf = a.getSafetyStock() != null ? a.getSafetyStock() : 0;
                    return s <= sf;
                })
                .forEach(a -> {
                    double s = a.getCurrentStock() != null ? a.getCurrentStock() : 0;
                    double sf = a.getSafetyStock() != null ? a.getSafetyStock() : 0;
                    if (s <= 0) {
                        list.add(insight("danger", "库存已空",
                                String.format("「%s」已经用完，该补一批了。", hl(a.getName()))));
                    } else {
                        list.add(insight("warning", "囤货见底了",
                                String.format("「%s」只剩 %s 件（安全线 %s），趁还没用完，记得补一批。",
                                        hl(a.getName()), hl(String.valueOf((int) s)), hl(String.valueOf((int) sf)))));
                    }
                });

        // 3. 续费提醒：自动续费 + 今日扣费 + 近期到期

        List<String> renewedToday = new ArrayList<>();
        List<Asset> subsForRenew = all.stream()
                .filter(a -> a.getNextBillingDate() != null)
                .filter(a -> a.getAssetType() != null && a.getAssetType().startsWith("SUBSCRIPTION_")
                        && !"SUBSCRIPTION_METERED".equals(a.getAssetType())
                        && !"SUBSCRIPTION_LIFETIME".equals(a.getAssetType()))
                .collect(Collectors.toList());

        for (Asset a : subsForRenew) {
            int renewed = subService.autoRenewIfDue(a);
            if (renewed > 0) {
                renewedToday.add(a.getName());
            }
        }

        // 今天刚续费
        for (String name : renewedToday) {
            list.add(insight("info", "今日扣费提醒",
                    "今天「" + hl(name) + "」到期扣费，已自动续费至下个周期。"));
        }

        // 最近到期（非今天）
        subsForRenew.stream()
                .filter(a -> subService.getRemainingDays(a) > 0)
                .min(Comparator.comparingLong(a -> subService.getRemainingDays(a)))
                .ifPresent(a -> {
                    long remaining = subService.getRemainingDays(a);
                    if (remaining <= 7) {
                        String cycle = a.getBillingCycle() != null && a.getBillingCycle().equals("YEARLY") ? "年"
                                : a.getBillingCycle() != null && a.getBillingCycle().equals("QUARTERLY") ? "季" : "月";
                        double cost = a.getMonthlyCost() != null ? a.getMonthlyCost() : 0;
                        list.add(insight("warning", "续费倒计时",
                                String.format("「%s」%s 天后就要续费 %s / %s了。如果最近打开得少了，不妨先暂停一阵子。",
                                        hl(a.getName()), hl(String.valueOf(remaining)),
                                        hl("¥" + (int) cost), cycle)));
                    }
                });

        // 4a. 次卡剩余次数
        all.stream()
                .filter(a -> "STORED_TIME_CARD".equals(a.getAssetType()))
                .filter(a -> {
                    int remaining = a.getRemainingTimes() != null ? a.getRemainingTimes() : 0;
                    return remaining <= 2;
                })
                .forEach(a -> {
                    int remaining = a.getRemainingTimes() != null ? a.getRemainingTimes() : 0;
                    if (remaining <= 0) {
                        list.add(insight("danger", "余额耗尽",
                                String.format("「%s」的次数刚刚走完。每一笔使用都是一段小记。", hl(a.getName()))));
                    } else {
                        list.add(insight("warning", "余量告急",
                                String.format("「%s」还剩 %s 次。不多，但也还够用一阵。",
                                        hl(a.getName()), hl(String.valueOf(remaining)))));
                    }
                });

        // 4b. 量卡余额
        all.stream()
                .filter(a -> "STORED_AMOUNT_CARD".equals(a.getAssetType()))
                .filter(a -> {
                    double balance = a.getCardBalance() != null ? a.getCardBalance() : 0;
                    double topup = a.getTotalTopup() != null ? a.getTotalTopup() : 1;
                    return balance / Math.max(topup, 1) < 0.15;
                })
                .forEach(a -> {
                    double balance = a.getCardBalance() != null ? a.getCardBalance() : 0;
                    if (balance <= 0) {
                        list.add(insight("danger", "余额耗尽",
                                String.format("「%s」余额归零了。它陪你走过了一段路，每一笔花销都算数。", hl(a.getName()))));
                    } else {
                        list.add(insight("warning", "余量告急",
                                String.format("「%s」余额 %s，已走过 %s。帐上不多了，但还在就好。",
                                        hl(a.getName()), hl("¥" + String.format("%.2f", balance)),
                                        hl("¥" + (int) (a.getTotalSpent() != null ? a.getTotalSpent() : 0)))));
                    }
                });

        // 4c. 按量计费余额
        all.stream()
                .filter(a -> "SUBSCRIPTION_METERED".equals(a.getAssetType()))
                .filter(a -> {
                    double balance = a.getApiBalance() != null ? a.getApiBalance() : 0;
                    double charged = a.getTotalCharged() != null ? a.getTotalCharged() : 1;
                    return balance / Math.max(charged, 1) < 0.15;
                })
                .forEach(a -> {
                    double balance = a.getApiBalance() != null ? a.getApiBalance() : 0;
                    if (balance <= 0) {
                        list.add(insight("danger", "余额耗尽",
                                String.format("「%s」余额刚好用完。记下这一笔，它曾为你工作过。", hl(a.getName()))));
                    } else {
                        list.add(insight("warning", "余量告急",
                                String.format("「%s」余额 %s，快见底了。回头看看这一路用了多少，心里有数就好。",
                                        hl(a.getName()), hl("¥" + String.format("%.2f", balance)))));
                    }
                });

        // 5. 资产概况
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
            assessment = "已经有了 " + hl(String.valueOf(count)) + " 件宝贝，生活正在被你温柔地整理着。";
        } else if (count <= 20) {
            assessment = hl(String.valueOf(count)) + " 件物品，总价值 " + hl("¥" + String.format("%.0f", totalValue)) + "。你对自己的所有物了然于胸，这本身就是一种笃定。";
        } else {
            assessment = hl(String.valueOf(count)) + " 件物品，横跨 " + hl(String.valueOf(cats)) + " 个分类，总值 " + hl("¥" + String.format("%.0f", totalValue)) + "。你不是在囤积，你是在经营一座小型生活博物馆。";
        }
        if (cats >= 5 && count > 3) {
            assessment += " " + hl(String.valueOf(cats)) + " 种分类打理得井井有条，眼光确实够广。";
        }
        list.add(insight("info", "生活资产小结", assessment));

        return list;
    }

    /** 高亮包裹 */
    private static String hl(String text) {
        return "<span style=\"color:var(--primary);font-weight:600;\">" + text + "</span>";
    }

    private Map<String, Object> insight(String type, String title, String content) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        m.put("title", title);
        m.put("content", content);
        return m;
    }
}
