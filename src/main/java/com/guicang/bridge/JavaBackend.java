package com.guicang.bridge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.guicang.model.*;
import com.guicang.service.*;
import com.guicang.util.DateUtil;

import java.util.*;

/**
 * HTTP API 路由分发 — 前端 fetch('/api/xxx') → 返回 JSON
 */
public class JavaBackend {

    private final Gson gson = new GsonBuilder().serializeNulls().create();

    private final UserService userService = new UserService();
    private final AssetService assetService = new AssetService();
    private final LongTermService longTermService = new LongTermService();
    private final StockpileService stockpileService = new StockpileService();
    private final CollectibleService collectibleService = new CollectibleService();
    private final SubscriptionService subscriptionService = new SubscriptionService();
    private final StoredCardService storedCardService = new StoredCardService();
    private final AchievementService achievementService = new AchievementService();
    private final InsightService insightService = new InsightService();

    // ===== 认证 =====

    public String login(String body) {
        Map<?,?> req = gson.fromJson(body, Map.class);
        UserService.LoginResult r = userService.login(
                (String) req.get("username"), (String) req.get("password"));
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", r.isSuccess());
        res.put("message", r.getMessage());
        if (r.isSuccess()) {
            Map<String, Object> u = new LinkedHashMap<>();
            u.put("id", r.getUser().getId());
            u.put("username", r.getUser().getUsername());
            u.put("nickname", r.getUser().getNickname());
            u.put("avatarIndex", r.getUser().getAvatarIndex());
            u.put("theme", r.getUser().getTheme());
            res.put("user", u);
        }
        return gson.toJson(res);
    }

    public String register(String body) {
        Map<?,?> req = gson.fromJson(body, Map.class);
        UserService.RegisterResult r = userService.register(
                (String) req.get("username"),
                (String) req.get("password"),
                req.get("nickname") != null ? (String) req.get("nickname") : (String) req.get("username"));
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", r.isSuccess());
        res.put("message", r.getMessage());
        return gson.toJson(res);
    }

    public String getSessionUser() {
        Map<String, Object> res = new LinkedHashMap<>();
        User u = userService.getCurrentUser();
        res.put("loggedIn", u != null);
        if (u != null) {
            Map<String, Object> um = new LinkedHashMap<>();
            um.put("id", u.getId()); um.put("username", u.getUsername());
            um.put("nickname", u.getNickname()); um.put("avatarIndex", u.getAvatarIndex());
            um.put("theme", u.getTheme());
            res.put("user", um);
        }
        return gson.toJson(res);
    }

    // ===== 资产 CRUD =====

    public String getAllAssets() {
        User u = requireLogin();
        List<Asset> list = assetService.getAll(u.getId());
        autoRenewSubscriptions(list);
        enrichAssets(list);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("data", list);
        return gson.toJson(res);
    }

    public String getArchivedAssets() {
        User u = requireLogin();
        List<Asset> list = assetService.getArchived(u.getId());
        enrichAssets(list);
        return gson.toJson(Map.of("success", true, "data", list));
    }

    public String getAssetById(String id) {
        requireLogin();
        Asset a = assetService.getById(id);
        if (a == null) return gson.toJson(Map.of("success", false, "message", "资产不存在"));
        enrichAssets(List.of(a));
        return gson.toJson(Map.of("success", true, "data", a));
    }

    public String createAsset(String body) {
        try {
            User u = requireLogin();
            Asset asset = gson.fromJson(body, Asset.class);
            asset.setUserId(u.getId());
            // 次卡：create 前初始化累计购入次数
            if ("STORED_TIME_CARD".equals(asset.getAssetType())) {
                int initTotal = asset.getTotalTimes() != null ? asset.getTotalTimes() : 0;
                asset.setCumulativePurchased(initTotal);
            }
            Asset created = assetService.create(asset);
            // 囤货模式：创建初始采购批次
            // purchasePrice 是购入总价，批次总价直接用它，单价 = 总价 / 数量
            if ("STOCKPILE".equals(created.getAssetType())) {
                try {
                    int initQty = created.getCurrentStock() != null ? created.getCurrentStock().intValue() : 0;
                    if (initQty > 0) {
                        double totalPrice = created.getPurchasePrice();
                        new com.guicang.repository.PurchaseBatchRepository().insert(
                            new com.guicang.model.PurchaseBatch(created.getId(), initQty, totalPrice, created.getCreatedAt()));
                    }
                } catch (Exception e) { System.err.println("[归藏] 创建囤货初始批次失败: " + e.getMessage()); }
            }
            // 写入创建历程（含类型专属详情）
            try {
                String detail = buildCreateNote(created);
                new com.guicang.repository.UsageLogRepository().insert(
                    new com.guicang.model.UsageLog(created.getId(), "CREATE", 0, 0,
                        detail, created.getCreatedAt()));
            } catch (Exception e) { System.err.println("[归藏] 写创建历程失败: " + e.getMessage()); }
            // 成就检查
            try { achievementService.checkAll(u.getId()); } catch (Exception e) {
                System.err.println("[归藏] 成就检查异常: " + e.getMessage());
                e.printStackTrace();
            }
            // 填充派生字段（均价/史低/史高等），否则前端创建后首次渲染依赖 fallback 假数据
            enrichAssets(List.of(created));
            return gson.toJson(Map.of("success", true, "data", created));
        } catch (Exception e) {
            System.err.println("[归藏] 创建资产异常: " + e.getMessage());
            e.printStackTrace();
            return gson.toJson(Map.of("success", false, "message", e.getMessage()));
        }
    }

    public String updateAsset(String id, String body) {
        requireLogin();
        Asset updated = gson.fromJson(body, Asset.class);
        Asset result = assetService.update(id, updated);
        return gson.toJson(Map.of("success", true, "data", result));
    }

    public String deleteAsset(String id) {
        requireLogin();
        assetService.delete(id);
        return gson.toJson(Map.of("success", true));
    }

    public String archiveAsset(String id) {
        User u = requireLogin();
        assetService.archive(id, true);
        try { achievementService.checkAll(u.getId()); } catch (Exception e) {}
        return gson.toJson(Map.of("success", true));
    }

    public String restoreAsset(String id) {
        User u = requireLogin();
        assetService.archive(id, false);
        try { achievementService.checkAll(u.getId()); } catch (Exception e) {}
        return gson.toJson(Map.of("success", true));
    }

    // ===== 资产操作 =====

    public String checkIn(String id) {
        User u = requireLogin();
        LongTermService.CheckInResult r = longTermService.checkIn(id, u.getId());
        List<Achievement> newAch = achievementService.checkAll(u.getId());

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("asset", r.asset);
        res.put("new_achievements", newAch);
        return gson.toJson(res);
    }

    public String consumeStock(String body) {
        User u = requireLogin();
        Map<?,?> req = gson.fromJson(body, Map.class);
        String assetId = (String) req.get("assetId");
        int qty = ((Number) req.get("quantity")).intValue();
        String notes = req.get("notes") != null ? (String) req.get("notes") : "";
        UsageLog log = stockpileService.consume(assetId, qty, notes);
        try { achievementService.checkAll(u.getId()); } catch (Exception e) {}
        return gson.toJson(Map.of("success", true, "data", log));
    }

    public String restock(String body) {
        User u = requireLogin();
        Map<?,?> req = gson.fromJson(body, Map.class);
        String assetId = (String) req.get("assetId");
        int qty = ((Number) req.get("quantity")).intValue();
        double totalPrice = ((Number) req.get("totalPrice")).doubleValue();
        PurchaseBatch batch = stockpileService.restock(assetId, qty, totalPrice);
        StockpileService.CompareResult cmp = stockpileService.comparePrice(assetId, batch.getUnitPrice());
        try { achievementService.checkAll(u.getId()); } catch (Exception e) {}
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("batch", batch);
        res.put("compare", cmp);
        return gson.toJson(res);
    }

    public String punchCard(String id) {
        User u = requireLogin();
        StoredCardService.PunchResult r = storedCardService.punch(id);
        try { achievementService.checkAll(u.getId()); } catch (Exception e) {}
        return gson.toJson(Map.of("success", r.success(), "message", r.message(), "remaining", r.remaining()));
    }

    public String topup(String body) {
        User u = requireLogin();
        Map<?,?> req = gson.fromJson(body, Map.class);
        String assetId = (String) req.get("assetId");
        double amount = ((Number) req.get("amount")).doubleValue();
        int times = req.containsKey("times") ? ((Number) req.get("times")).intValue() : 0;

        Asset item = assetService.getById(assetId);
        Asset result;
        if ("SUBSCRIPTION_METERED".equals(item.getAssetType())) {
            result = storedCardService.topupApi(assetId, amount);
        } else if (times > 0) {
            result = storedCardService.topupTimes(assetId, amount, times);
        } else {
            result = storedCardService.topupAmount(assetId, amount);
        }
        try { achievementService.checkAll(u.getId()); } catch (Exception e) {}
        return gson.toJson(Map.of("success", true, "data", result));
    }

    public String spend(String body) {
        User u = requireLogin();
        Map<?,?> req = gson.fromJson(body, Map.class);
        String assetId = (String) req.get("assetId");
        double amount = ((Number) req.get("amount")).doubleValue();

        Asset item = assetService.getById(assetId);
        if ("SUBSCRIPTION_METERED".equals(item.getAssetType())) {
            StoredCardService.SpendResult r = storedCardService.spendApi(assetId, amount);
            try { achievementService.checkAll(u.getId()); } catch (Exception e) {}
            return gson.toJson(Map.of("success", r.success(), "message", r.message(), "remainingBalance", r.remainingBalance()));
        }
        StoredCardService.SpendResult r = storedCardService.spend(assetId, amount);
        try { achievementService.checkAll(u.getId()); } catch (Exception e) {}
        return gson.toJson(Map.of("success", r.success(), "message", r.message(), "remainingBalance", r.remainingBalance()));
    }

    public String updateCollectStatus(String id) {
        requireLogin();
        String newStatus = collectibleService.cycleStatus(id);
        return gson.toJson(Map.of("success", true, "status", newStatus));
    }

    public String comparePrice(String body) {
        requireLogin();
        Map<?,?> req = gson.fromJson(body, Map.class);
        String assetId = (String) req.get("assetId");
        double externalPrice = ((Number) req.get("externalPrice")).doubleValue();
        StockpileService.CompareResult r = stockpileService.comparePrice(assetId, externalPrice);
        return gson.toJson(Map.of("success", true, "compare", r));
    }

    public String getHistory(String assetId) {
        requireLogin();
        var logs = new com.guicang.repository.UsageLogRepository().findByAsset(assetId);
        return gson.toJson(Map.of("success", true, "data", logs));
    }

    // ===== 仪表盘 / 成就 / 洞察 =====

    public String getDashboard() {
        User u = requireLogin();
        List<Asset> all = assetService.getAll(u.getId());
        autoRenewSubscriptions(all);
        Map<String, Object> d = new LinkedHashMap<>();

        // 1. 固定流速：所有周期续费统一折算为月均
        double monthlyFlow = all.stream()
                .filter(a -> a.getMonthlyCost() != null && a.getMonthlyCost() > 0)
                .filter(a -> a.getAssetType() != null && a.getAssetType().startsWith("SUBSCRIPTION_"))
                .filter(a -> a.getBillingCycle() != null)
                .mapToDouble(a -> {
                    double cost = a.getMonthlyCost();
                    String cycle = a.getBillingCycle();
                    if ("QUARTERLY".equals(cycle)) return cost / 3.0;
                    if ("YEARLY".equals(cycle)) return cost / 12.0;
                    return cost; // MONTHLY
                }).sum();
        d.put("monthlyFlow", monthlyFlow);

        // 2. 细水长流均摊：按天+按次一起算（购入价 / 持有天数）
        double dailyAvg = all.stream()
                .filter(a -> "LONG_TERM_PER_USE".equals(a.getAssetType()) || "LONG_TERM_PER_DAY".equals(a.getAssetType()))
                .mapToDouble(a -> {
                    long days = longTermService.getDaysHeld(a);
                    return a.getPurchasePrice() / Math.max(days, 1);
                }).average().orElse(0);
        d.put("dailyAvg", dailyAvg);

        // 3. 按量计费累计注资
        double quantPool = all.stream()
                .filter(a -> "SUBSCRIPTION_METERED".equals(a.getAssetType()))
                .mapToDouble(a -> a.getTotalCharged() != null ? a.getTotalCharged() : 0).sum();
        d.put("quantPool", quantPool);

        // 4. 资产总价值：购入价 + 储值卡后续充值 + 按量计费后续充值
        double totalValue = all.stream().mapToDouble(a -> {
            double v = a.getPurchasePrice();
            if (a.getTotalTopup() != null) v += a.getTotalTopup();
            if (a.getTotalCharged() != null) v += a.getTotalCharged();
            return v;
        }).sum();
        d.put("totalItems", all.size());
        d.put("totalValue", totalValue);
        d.put("categoryCount", all.stream().map(Asset::getCategory).filter(Objects::nonNull).distinct().count());

        // 5. 最近一笔续费剩余天数（用于物语提醒）
        double nearestRenewalDays = all.stream()
                .filter(a -> a.getNextBillingDate() != null)
                .filter(a -> a.getAssetType() != null && a.getAssetType().startsWith("SUBSCRIPTION_")
                        && !"SUBSCRIPTION_METERED".equals(a.getAssetType())
                        && !"SUBSCRIPTION_LIFETIME".equals(a.getAssetType()))
                .mapToDouble(a -> (double) subscriptionService.getRemainingDays(a))
                .filter(days -> days >= 0)
                .min().orElse(-1);
        d.put("nearestRenewalDays", nearestRenewalDays);

        // 6. 成就
        d.put("achievementStats", achievementService.getOverview(u.getId()).stream()
                .filter(m -> Boolean.TRUE.equals(m.get("completed"))).count() + "/36");

        return gson.toJson(Map.of("success", true, "data", d));
    }

    public String getAchievements() {
        User u = requireLogin();
        return gson.toJson(Map.of("success", true, "data", achievementService.getOverview(u.getId())));
    }

    public String getPendingAchievements() {
        User u = requireLogin();
        return gson.toJson(Map.of("success", true, "data", achievementService.getPendingNotifications(u.getId())));
    }

    public String acknowledgeAchievements(String body) {
        User u = requireLogin();
        Map<?,?> req = gson.fromJson(body, Map.class);
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) req.get("ids");
        achievementService.acknowledgeNotifications(u.getId(), ids != null ? ids : List.of());
        return gson.toJson(Map.of("success", true));
    }

    public String getInsights() {
        User u = requireLogin();
        return gson.toJson(Map.of("success", true, "data", insightService.generate(u.getId())));
    }

    // ===== 配置 =====

    public String getConfig(String key) {
        // 目前只支持 theme 读取
        User u = userService.getCurrentUser();
        if (u != null && "theme".equals(key)) {
            return gson.toJson(Map.of("success", true, "value", u.getTheme() != null ? u.getTheme() : "dark"));
        }
        return gson.toJson(Map.of("success", true, "value", "dark"));
    }

    public String setConfig(String body) {
        Map<?,?> req = gson.fromJson(body, Map.class);
        String key = (String) req.get("key");
        String value = (String) req.get("value");
        if ("theme".equals(key)) {
            userService.updateProfile(null, null, null, value);
        } else if ("password".equals(key)) {
            try {
                Map<?,?> pw = gson.fromJson(value, Map.class);
                userService.changePassword((String) pw.get("old"), (String) pw.get("new"));
            } catch (Exception e) {
                return gson.toJson(Map.of("success", false, "message", e.getMessage()));
            }
        }
        return gson.toJson(Map.of("success", true));
    }

    // ===== 导出 =====
    public String exportData() {
        User u = requireLogin();
        Map<String, Object> backup = new LinkedHashMap<>();
        // 安全：排除密码哈希，只导出公开资料
        Map<String, Object> safeUser = new LinkedHashMap<>();
        safeUser.put("id", u.getId());
        safeUser.put("username", u.getUsername());
        safeUser.put("nickname", u.getNickname());
        safeUser.put("avatarIndex", u.getAvatarIndex());
        safeUser.put("theme", u.getTheme());
        backup.put("user", safeUser);
        backup.put("assets", assetService.getAll(u.getId()));
        backup.put("exportDate", java.time.LocalDate.now().toString());
        return gson.toJson(Map.of("success", true, "data", backup));
    }

    // ===== 工具 =====

    private User requireLogin() {
        User u = userService.getCurrentUser();
        if (u == null) throw new RuntimeException("未登录");
        return u;
    }

    /** 扫描资产列表中的周期续费，自动续费到期项；返回本次续费数量 */
    private int autoRenewSubscriptions(List<Asset> list) {
        int count = 0;
        for (Asset a : list) {
            String type = a.getAssetType();
            if (type == null || !type.startsWith("SUBSCRIPTION_")
                    || "SUBSCRIPTION_METERED".equals(type)
                    || "SUBSCRIPTION_LIFETIME".equals(type)) continue;
            if (subscriptionService.autoRenewIfDue(a) > 0) count++;
        }
        return count;
    }

    /** 根据资产类型生成创建历程详情 */
    private String buildCreateNote(Asset a) {
        String t = a.getAssetType();
        StringBuilder sb = new StringBuilder("创建「").append(a.getName()).append("」");
        sb.append(" · 购入价 ¥").append(String.format("%.2f", a.getPurchasePrice()));

        if ("LONG_TERM_PER_USE".equals(t)) {
            sb.append(" · 按次分摊");
        } else if ("LONG_TERM_PER_DAY".equals(t)) {
            sb.append(" · 按天均摊");
        } else if ("STOCKPILE".equals(t)) {
            sb.append(" · 初始库存 ").append(a.getCurrentStock() != null ? String.format("%.0f", a.getCurrentStock()) : "0");
            sb.append(" · 安全水位 ").append(a.getSafetyStock() != null ? String.format("%.0f", a.getSafetyStock()) : "0");
        } else if ("COLLECTIBLE".equals(t)) {
            sb.append(" · 收藏状态 · ").append(a.getCollectStatus() != null ? a.getCollectStatus() : "日常使用中");
        } else if (t != null && t.startsWith("SUBSCRIPTION_")) {
            if ("SUBSCRIPTION_METERED".equals(t)) {
                sb.append(" · 按量计费 · 初始余额 ¥").append(String.format("%.2f", a.getApiBalance() != null ? a.getApiBalance() : 0));
            } else if ("SUBSCRIPTION_LIFETIME".equals(t)) {
                sb.append(" · 永久买断");
            } else {
                String cycle = "MONTHLY".equals(a.getBillingCycle()) ? "月" : "QUARTERLY".equals(a.getBillingCycle()) ? "季" : "年";
                sb.append(" · 每").append(cycle).append(" ¥").append(String.format("%.2f", a.getMonthlyCost() != null ? a.getMonthlyCost() : 0));
                sb.append(" · 下次扣款 ").append(a.getNextBillingDate() != null ? a.getNextBillingDate() : "未设");
            }
        } else if ("STORED_TIME_CARD".equals(t)) {
            sb.append(" · 次卡 · 共 ").append(a.getTotalTimes() != null ? a.getTotalTimes() : 0).append(" 次");
            sb.append(" · 剩余 ").append(a.getRemainingTimes() != null ? a.getRemainingTimes() : 0).append(" 次");
            sb.append(" · 充额 ¥").append(String.format("%.2f", a.getTotalTopup() != null ? a.getTotalTopup() : 0));
        } else if ("STORED_AMOUNT_CARD".equals(t)) {
            sb.append(" · 量卡 · 余额 ¥").append(String.format("%.2f", a.getCardBalance() != null ? a.getCardBalance() : 0));
            sb.append(" · 累计充值 ¥").append(String.format("%.2f", a.getTotalTopup() != null ? a.getTotalTopup() : 0));
        }
        return sb.toString();
    }

    /** 为资产列表填充派生字段（天数、成本、状态标识） */
    private void enrichAssets(List<Asset> list) {
        for (Asset a : list) {
            String type = a.getAssetType();
            if (type == null) continue;

            // 计算陪伴天数（所有类型统一）
            if ("STOCKPILE".equals(type)) {
                a.setDaysHeld(DateUtil.daysBetween(a.getPurchaseDate(), DateUtil.today()));
                // 从采购批次计算真实均价 / 最低单价 / 最高单价
                var batches = new com.guicang.repository.PurchaseBatchRepository().findByAsset(a.getId());
                if (!batches.isEmpty()) {
                    int totalQty = batches.stream().mapToInt(com.guicang.model.PurchaseBatch::getQuantity).sum();
                    double totalPrice = batches.stream().mapToDouble(com.guicang.model.PurchaseBatch::getTotalPrice).sum();
                    double minUnit = batches.stream().mapToDouble(b -> b.getTotalPrice() / b.getQuantity()).min().orElse(0);
                    double maxUnit = batches.stream().mapToDouble(b -> b.getTotalPrice() / b.getQuantity()).max().orElse(0);
                    a.setDailyCost(totalQty > 0 ? totalPrice / totalQty : 0);  // 复用 dailyCost 存均价
                    a.setCostRatio(minUnit);   // 复用 costRatio 存史低单价
                    a.setStatusTagVariant(String.valueOf(maxUnit));  // 复用存史高单价（临时）
                }
            } else if ("STORED_TIME_CARD".equals(type) || "STORED_AMOUNT_CARD".equals(type)) {
                a.setDaysHeld(DateUtil.daysBetween(a.getPurchaseDate(), DateUtil.today()));
            }

            if ("LONG_TERM_PER_USE".equals(type)) {
                a.setDaysHeld(longTermService.getDaysHeld(a));
                int uc = a.getUsageCount() != null ? a.getUsageCount() : 0;
                double cpu = uc > 0 ? a.getPurchasePrice() / uc : a.getPurchasePrice();
                a.setDailyCost(cpu);
                a.setCostRatio(a.getPurchasePrice() > 0 ? cpu / a.getPurchasePrice() : 1);
                if (uc > 0) {
                    double ratio = a.getCostRatio();
                    if (ratio <= 0.10) { a.setStatusTagVariant("success"); a.setStatusMessageKey("coat_perfect"); }
                    else if (ratio <= 0.30) a.setStatusMessageKey("coat_great");
                    else if (ratio <= 0.50) a.setStatusMessageKey("coat_good");
                    else if (ratio <= 0.70) a.setStatusMessageKey("coat_warm");
                    else a.setStatusMessageKey("coat_first");
                } else {
                    a.setStatusMessageKey("coat_start");
                }
            } else if ("LONG_TERM_PER_DAY".equals(type)) {
                a.setDaysHeld(longTermService.getDaysHeld(a));
                a.setDailyCost(longTermService.getDailyCost(a));
            } else if ("COLLECTIBLE".equals(type)) {
                a.setDaysHeld(collectibleService.getDaysHeld(a));
            } else if (type.startsWith("SUBSCRIPTION_")) {
                a.setDaysHeld(subscriptionService.getRemainingDays(a));
            }
        }
    }
}
