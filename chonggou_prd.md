# 「归藏」全栈架构设计文档

> **版本**: v1.0 · **最后更新**: 2026-06-08

---

## 一、总体架构

```
┌──────────────────────────────────────────────────────────────────┐
│               Java 桌面应用 (Swing JFrame + JCEF)                  │
│                                                                    │
│   ┌────────────────────────────────────────────────────────────┐  │
│   │            JFrame (1040×700, min 900×640)                  │  │
│   │  ┌──────────────────────────────────────────────────────┐  │  │
│   │  │         JCEF Browser (Chromium 内核)                  │  │  │
│   │  │  ┌────────────────────────────────────────────────┐  │  │  │
│   │  │  │         Vue 3 前端 (index.html)                 │  │  │  │
│   │  │  │  ┌──────────┐  ┌──────────┐  ┌──────────────┐  │  │  │  │
│   │  │  │  │  5 Views │  │ reactive │  │  bridge.js   │  │  │  │  │
│   │  │  │  │  (v-show)│  │  state   │  │  (fetch API) │  │  │  │  │
│   │  │  │  └──────────┘  └──────────┘  └──────┬───────┘  │  │  │  │
│   │  │  └────────────────────────────────────┼───────────┘  │  │  │
│   │  └───────────────────────────────────────┼──────────────┘  │  │
│   │                                          │                  │  │
│   │      fetch('/api/...')                   │  HTTP            │  │
│   │      (JSON 往返)                         │  localhost:18080 │  │
│   └──────────────────────────────────────────┼──────────────────┘  │
│                                              │                     │
│   ┌──────────────────────────────────────────┼──────────────────┐  │
│   │           内嵌 HTTP 服务器 (JDK HttpServer)                  │  │
│   │  ┌───────────────────────────────────────┴────────────────┐  │  │
│   │  │  JavaBackend — API 路由分发 (App.java 内部类)          │  │  │
│   │  │  /api/login  /api/assets  /api/dashboard  ...          │  │  │
│   │  ├────────────────────────────────────────────────────────┤  │  │
│   │  │  Service Layer (8 个服务)                               │  │  │
│   │  │  UserService  AssetService  LongTermService             │  │  │
│   │  │  StockpileService  CollectibleService                   │  │  │
│   │  │  SubscriptionService  StoredCardService                 │  │  │
│   │  │  AchievementService  InsightService                     │  │  │
│   │  ├────────────────────────────────────────────────────────┤  │  │
│   │  │  Repository Layer (5 个仓库，原生 JDBC)                  │  │  │
│   │  │  UserRepository  AssetRepository  UsageLogRepository    │  │  │
│   │  │  PurchaseBatchRepository  AchievementRepository         │  │  │
│   │  └────────────────────────────────────────────────────────┘  │  │
│   └─────────────────────────────────────────────────────────────┘  │
│                                                                    │
│   ┌─────────────────────────────────────────────────────────────┐  │
│   │        SQLite 数据库 (assets.db，项目本地或 %APPDATA%)       │  │
│   │  6 张表: users / assets / usage_logs / purchase_batches      │  │
│   │          achievement_defs / user_achievements                │  │
│   └─────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
```

### 架构关键事实

| 维度 | 实际实现 |
|------|---------|
| **运行模式** | 双模式：① JCEF 桌面窗口（默认）② `--browser` 浏览器模式（系统托盘图标） |
| **前后端通信** | `com.sun.net.httpserver.HttpServer` 内嵌 HTTP 服务器，`localhost:18080`，前端 `fetch('/api/...')` |
| **桌面壳** | Swing JFrame + JCEF (jcefmaven 自动管理 Chromium 原生库)，JCEF 不可用时降级为系统浏览器 |
| **数据库** | SQLite via JDBC，路径智能解析：开发环境 `./data/assets.db`，打包后 `%APPDATA%\Guicang\data\assets.db` |
| **UI 渲染** | JCEF Browser 加载 `http://localhost:18080/index.html` → Chromium 完整内核渲染 |
| **前端框架** | Vue 3 ES Module（importmap 方式引入 `vue.esm-browser.prod.js`），**不使用构建工具** |
| **前端文件结构** | 单文件架构：1 个 HTML + 1 个 CSS + 1 个 app.js + bridge.js + config.js |
| **密码存储** | BCrypt (jbcrypt)，注册和修改密码时自动哈希 |
| **JSON 序列化** | Gson（Java 端），前端 fetch + Response.json() |
| **构建系统** | Maven + maven-shade-plugin（打 fat-jar） |
| **用户会话** | 内存 `currentUser` 字段（UserService 单例），无 JWT / Token，服务器重启即登出 |
| **静态资源** | 全部打包在 JAR 的 `/web/` 路径下，`App.class.getResourceAsStream()` 加载，离线 100% 可用 |

---

## 二、数据模型与数据库设计

### 2.1 资产分类体系（11 种 AssetType）

```
实体物品 (Physical)
├── LONG_TERM_PER_USE     细水长流·按次  — 单价打卡，每用一次均价下降
├── LONG_TERM_PER_DAY     细水长流·按天  — 按持有天数算日均成本
├── STOCKPILE             储备幸福       — 库存管理 + 多批次均价 + 消耗 + 比价
└── COLLECTIBLE           收藏状态       — 纯记录 + 三态切换(日常使用/完美珍藏/计划转手)

数字订阅 (Digital Subscription)
├── SUBSCRIPTION_MONTHLY  周期续费·按月  — 固定月费 + 续费倒计时 + 自动续费
├── SUBSCRIPTION_QUARTERLY周期续费·按季  — 同上，周期 90 天
├── SUBSCRIPTION_YEARLY   周期续费·按年  — 同上，周期 365 天
├── SUBSCRIPTION_METERED  按量计费       — 预充值余额 + 按调用消耗
└── SUBSCRIPTION_LIFETIME 永久有效       — 一次性买断，进度条 100%

会员储值 (Stored-value)
├── STORED_TIME_CARD      储值次卡       — 固定次数 + 按次核销 + 可续次
└── STORED_AMOUNT_CARD    储值量卡       — 储值余额 + 按金额消费
```

### 2.2 数据库表（6 张表，SQLite）

```sql
-- users — 用户表
-- 字段: id, username (UNIQUE), password_hash (BCrypt), nickname, email,
--        avatar_index (DEFAULT 0), theme (DEFAULT 'dark'), created_at

-- assets — 统一资产表（单表 11 种类型，专属字段可 NULL）
-- 通用: id (UUID 8位), user_id FK, asset_type, name, icon, category,
--        purchase_price, purchase_date, notes
-- 按次: usage_count
-- 按天: expected_lifespan_years
-- 囤货: current_stock, safety_stock
-- 收藏: collect_status
-- 订阅: billing_cycle, monthly_cost, next_billing_date
-- 按量: api_balance, total_charged
-- 次卡: remaining_times, total_times, cumulative_purchased
-- 量卡: card_balance, total_spent
-- 共用: total_topup
-- 状态: is_archived (DEFAULT 0), created_at, updated_at

-- usage_logs — 操作流水
-- 字段: id, asset_id FK, action_type (CHECK_IN|CONSUME|PUNCH|TOPUP|SPEND|
--        RESTOCK|STATUS_CHANGE|CREATE|UPDATE_META|RENEW),
--        quantity, unit_price, notes, created_at

-- purchase_batches — 囤货采购批次
-- 字段: id (UUID), asset_id FK, quantity, total_price, batch_date

-- achievement_defs — 成就定义（36 条预置数据）
-- 字段: id (a01~a36), name, description, icon, category,
--        goal_type, goal_value, level (1=铜 2=银 3=金)

-- user_achievements — 用户成就进度
-- 字段: user_id FK, achievement_id FK, progress (0~1),
--        is_completed, completed_date, is_notified
```

---

## 三、Java 后端架构

### 3.1 项目结构

```
src/main/java/com/guicang/
├── App.java                      ← 主入口：启动 SQLite → 启动 HTTP 服务器 → 创建 JCEF 窗口
│                                   内嵌 ApiHandler (路由分发) + StaticFileHandler (静态文件)
├── config/
│   ├── AppConfig.java            ← 窗口尺寸(1040×700, min 900×640), HTTP 端口(18080)
│   └── DatabaseConfig.java       ← SQLite 连接管理, DDL 自动执行, 数据目录智能解析
├── bridge/
│   └── JavaBackend.java          ← HTTP API 全量路由实现 (约 30 个方法)
│                                   认证 / 资产 CRUD / 操作 / 大盘 / 成就 / 配置 / 导出
├── service/
│   ├── UserService.java          ← 注册/登录/密码修改/资料更新, BCrypt 验证
│   ├── AssetService.java         ← 资产 CRUD, 增量更新, 历程写入
│   ├── LongTermService.java      ← 打卡/日均成本/ratio 计算/5 档状态判定
│   ├── StockpileService.java     ← 库存+批次均价+消耗+补货+比价, 全部事务保护
│   ├── CollectibleService.java   ← 收藏三态循环, 陪伴天数
│   ├── SubscriptionService.java  ← 续费倒计时, 自动续费(synchronized 防并发)
│   ├── StoredCardService.java    ← 次卡核销/充值, 量卡消费/充值, 按量充值/消耗, 全部事务保护
│   ├── AchievementService.java   ← 36 成就全量检查, 进度更新, 通知管理
│   └── InsightService.java       ← 智能洞察生成 (6 类洞察), HTML 高亮包裹
├── repository/
│   ├── UserRepository.java       ← 用户 CRUD + BCrypt
│   ├── AssetRepository.java      ← 资产 CRUD (动态 SQL 构建，11 种类型统一处理)
│   ├── UsageLogRepository.java   ← 流水查询+写入
│   ├── PurchaseBatchRepository.java ← 批次查询+写入
│   └── AchievementRepository.java   ← 成就定义查询, 进度更新(upsert), 通知管理
├── model/
│   ├── User.java                 ← 用户实体
│   ├── Asset.java                ← 统一资产实体 (25+ 字段 + 5 个派生字段)
│   ├── UsageLog.java             ← 操作流水
│   ├── PurchaseBatch.java        ← 采购批次
│   ├── Achievement.java          ← 成就定义
│   └── UserAchievement.java      ← 用户成就进度
└── util/
    ├── DateUtil.java              ← today(), daysBetween(), addDays()
    ├── Validator.java             ← 用户名/密码格式校验
    └── Categories.java            ← 分类常量
```

### 3.2 HTTP API 路由表

所有 API 挂载在 `http://localhost:18080/api/` 下，`StaticFileHandler` 处理 `/` 下的静态文件。

#### 认证
| 方法 | 路径 | 说明 | 请求体 |
|------|------|------|--------|
| POST | `/api/login` | 登录 | `{username, password}` → `{success, message, user}` |
| POST | `/api/register` | 注册 | `{username, password, nickname}` → `{success, message}` |
| GET | `/api/session` | 获取当前会话 | → `{loggedIn, user?}` |

#### 资产 CRUD
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/assets` | 获取全部资产（自动续费 + 派生字段填充） |
| POST | `/api/assets` | 创建资产（自动写初始批次/历程/成就检查） |
| GET | `/api/assets/{id}` | 获取单个资产 |
| PUT | `/api/assets/{id}` | 更新资产（增量更新，自动写 UPDATE_META 历程） |
| DELETE | `/api/assets/{id}` | 删除资产（级联删除 usage_logs + purchase_batches） |
| GET | `/api/assets/archived` | 获取归档资产 |

#### 资产操作
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/assets/{id}/check-in` | 按次打卡 |
| POST | `/api/assets/{id}/consume` | 囤货消耗 `{assetId, quantity, notes}` |
| POST | `/api/assets/{id}/restock` | 补货入库 `{assetId, quantity, totalPrice}` → 返回比价结果 |
| POST | `/api/assets/{id}/punch` | 次卡核销 → 返回剩余次数 |
| POST | `/api/assets/{id}/topup` | 充值（自动判断按量/次卡/量卡） |
| POST | `/api/assets/{id}/spend` | 消费（自动判断按量/量卡） |
| PUT | `/api/assets/{id}/status` | 收藏状态三态循环 |
| POST | `/api/assets/{id}/compare` | 价格比较 |
| GET | `/api/assets/{id}/history` | 获取操作历史 |
| POST | `/api/assets/{id}/archive` | 归档 |
| POST | `/api/assets/{id}/restore` | 恢复归档 |

#### 大盘/成就/洞察
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/dashboard` | 大盘数据（月流速/日均/按量注资/资产总价值/最近续费天数/成就统计） |
| GET | `/api/achievements` | 全部 36 成就进度 |
| GET | `/api/achievements/pending` | 已完成但未播报的成就（用于 Toast） |
| POST | `/api/achievements/acknowledge` | 标记成就已播报 `{ids: [...]}` |
| GET | `/api/insights` | 智能洞察列表 |

#### 配置/导出
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/config?key=theme` | 读取主题配置 |
| POST | `/api/config` | 写入配置（theme / password） |
| GET | `/api/export` | 全量数据导出（不含密码哈希） |

### 3.3 关键业务逻辑

#### 打卡 (check-in)
```
1. usage_count += 1
2. 计算单次成本 = purchasePrice / usageCount
3. 计算 costRatio = 单次成本 / purchasePrice
4. 根据 ratio 判定 5 档状态:
   ratio ≤ 0.10 → tagVariant="success", messageKey="coat_perfect"
   ratio ≤ 0.30 → messageKey="coat_great"
   ratio ≤ 0.50 → messageKey="coat_good"
   ratio ≤ 0.70 → messageKey="coat_warm"
   否则 → messageKey="coat_first" (usageCount=0 → "coat_start")
5. 写入 usage_logs (CHECK_IN)
6. 触发 achievementService.checkAll()
7. 返回更新后的资产 + 新解锁成就列表
```

#### 囤货补货 + 比价
```
1. 创建 PurchaseBatch 记录（事务保护）
2. recalcStock() → currentStock = 总入库 - 总消耗
3. comparePrice() → 本次单价 vs 历史价格区间:
   - ≤ 史低 → verdict="good", 绿色反馈
   - ≥ 史高 → verdict="bad", 琥珀反馈
   - 居中 → verdict="neutral", 显示百分位
4. 触发成就检查
5. 返回批次 + 比价结果（前端显示比价条，8 秒后自动消失）
```

#### 周期续费自动续费
```
autoRenewIfDue() — 每次 getAllAssets() 和 getDashboard() 时调用:
1. synchronized 方法 + DB 重读防并发
2. 若 remainingDays ≤ 0: 从原扣费日往后推算新日期
3. 循环跳过已错过的周期，直到落在未来
4. 写入 usage_logs (RENEW)
5. 订阅的 remainingDays 用于前端倒计时展示和进度条高度
```

#### 资产数据丰富化 (enrichAssets)
```
每次从 DB 读取资产后，JavaBackend 调用 enrichAssets() 填充派生字段:
- daysHeld: DateUtil.daysBetween(purchaseDate, today)
- 按次: dailyCost = purchasePrice / usageCount, costRatio = dailyCost / purchasePrice
- 按天: dailyCost = purchasePrice / max(daysHeld, 1)
- 囤货: dailyCost = 综合均价(所有批次)
          costRatio = 史低单价
          statusTagVariant = 史高单价
- 收藏: daysHeld
- 订阅: daysHeld = remainingDays (用于前端展示)
```

---

## 四、Vue 3 前端架构

### 4.1 文件结构

```
src/main/resources/web/
├── index.html                ← 单文件 HTML (全部模板 + Vue 指令)
├── css/
│   └── theme.css             ← 全部 CSS (变量 + 双主题 + 组件 + 手绘覆写)
└── js/
    ├── app.js                ← 全部 Vue 逻辑 (createApp, setup, 所有方法, 500行)
    ├── api/
    │   └── bridge.js         ← fetch 封装 (所有 API 调用函数, 30 个导出)
    ├── config.js             ← 常量定义 (API_BASE, AssetType 枚举, 阈值, 图标映射)
    └── lib/
        └── vue.esm-browser.prod.js  ← Vue 3 运行时 (ES Module 版, ~120KB)
```

### 4.2 技术要点

- **Vue 引入方式**: `<script type="importmap">` + ES Module import `from 'vue'`。不使用构建工具，不写 `.vue` 单文件组件
- **状态管理**: Vue 3 `reactive()` + `ref()` + `computed()`，不使用 Vuex/Pinia
- **模板**: 全部写在 `index.html` 中的 `<template v-if="phase==='main'">` 内部，使用 `v-if`/`v-show`/`v-for`/`:class`/`@click` 等指令
- **路由**: 无 vue-router，用 `view` ref 变量 + `v-show` 切换 5 个视图
- **API 调用**: `bridge.js` 中 `request(method, path, body)` → `fetch(API_BASE + path)` → JSON
- **认证流**: 页面加载 → `api.getSession()` → 若 `loggedIn=false` 显示登录遮罩 → 登录成功设置 `phase='main'` → `loadAll()` 批量加载数据
- **文案归属**: 前端 `app.js` 中的 `COAT_MSGS` 对象维护 6 档文案，后端只返回原始数据 (`usageCount`, `costRatio`)，前端 `enrichAsset()` 根据阈值自行判断显示哪些文案
- **阈值归属**: 前端 `enrichAsset()` 中的 computed 逻辑判定 UI 状态（如 `ratio≤0.10 → status-perfect` 变绿），后端返回的数据中包含 `costRatio` 原始值

### 4.3 Vue 响应式数据设计

```
state (reactive)
├── assets: Asset[]           ← 全部未归档资产
├── archived: Asset[]         ← 归档资产
├── achievements: AchievementProgress[]  ← 36 成就进度
├── dashboard: {}             ← 大盘数据
└── insights: Insight[]       ← 今日物语

computed 派生
├── physical                  ← assets 中 4 种实体类型
├── digital                   ← 周期续费类
├── metered                   ← 按量计费类
├── lifetime                  ← 永久有效类
├── timeCards                 ← 储值次卡类
├── amountCards               ← 储值量卡类
├── allDigital                ← digital + metered + lifetime + timeCards + amountCards
├── completedCount            ← achievements 中已解锁数量
├── totalValue                ← 全部资产总价值（含后续充值）
└── achGroups                 ← 成就按 7 个分类分组
```

### 4.4 5 个视图

| 视图 | view 值 | 内容 |
|------|---------|------|
| 仪表盘 | `dashboard` | 三栏大盘卡片(月流速/日均/按量注资) + 会员总览(5 个数字) + 今日物语 |
| 物尽其用 | `physical` | 全部实体物品卡片(grid)，按类型渲染不同模板 |
| 会员中心 | `digital` | 全部数字订阅+储值卡(sub-grid) |
| 成就殿堂 | `achievements` | 36 成就网格，7 分类标题，进度条+锁定态 |
| 封存仓库 | `warehouse` | 归档资产列表，支持查看历程+重启唤醒 |

### 4.5 弹窗/抽屉系统

| 组件 | Vue 控制变量 | 类型 | 宽度 |
|------|-------------|------|------|
| 新建/编辑抽屉 | `addDrawerOpen` | 右侧滑入 `modal-overlay` | 420px |
| 历史时间轴 | `historyOpen` | 右侧滑入 `modal-overlay` | 380px |
| 删除/归档确认 | `confirmOpen` + `confirmType` | 居中 `pop-modal-overlay` | 330px |
| 快捷操作表单 | `formModalOpen` + `formModalMode` | 居中 `pop-modal-overlay` | 330px |
| 头像选择 | `showAvatarPicker` | 居中 `pop-modal-overlay` | 330px |
| 修改昵称 | `showNicknameModal` | 居中 `pop-modal-overlay` | 330px |
| 修改密码 | `showPasswordModal` | 居中 `pop-modal-overlay` | 330px |

---

## 五、成就系统（36 条，7 大类）

| 分类 | 数量 | ID 范围 | 关键成就 |
|------|------|---------|---------|
| 物品收集 | 4 | a01~a04 | 1件→5件→10件→20件 |
| 细水长流 | 6 | a05~a10 | 打卡1次→10次→30次→100次→物尽其用→日积月累(365天) |
| 储备幸福 | 8 | a11~a17, a35 | 囤货1件→补货1次→库存告急→耗尽→3件→史低1次→史低3次→高价也从容 |
| 收藏纪念 | 4 | a18~a21 | 1件→30天→365天→3件 |
| 数字订阅 | 6 | a22~a27 | 1个→3个→归档→永久→储值2→4种全有 |
| 资产总览 | 5 | a28~a32 | 万元户→5万→多元化→仓库→5种资产类型 |
| 里程碑 | 3 | a33, a34, a36 | 日省一文→日省百文→归藏大师(25个解锁) |

### 通知机制
- 每次操作后 `achievementService.checkAll()` 全量检查
- 前后对比已解锁集合 → 差集为新解锁成就
- `is_notified=0` 的新解锁成就 → 前端 `loadAll()` 时通过 `/api/achievements/pending` 获取 → Toast 逐个弹窗
- Toast 后调用 `/api/achievements/acknowledge` 标记已播报

---

## 六、关键公式

| 公式 | 计算方式 | 触发时机 |
|------|---------|---------|
| 单次成本 | `purchasePrice ÷ usageCount` | 每次打卡 |
| 成本比例 ratio | `单次成本 ÷ purchasePrice` | 每次打卡 |
| 日均成本(按天) | `purchasePrice ÷ max(daysHeld, 1)` | 页面加载(自然递增) |
| 大盘日均 | 所有 LONG_TERM 资产的 `(purchasePrice ÷ daysHeld)` 算术平均 | 每次操作后 |
| 库存 | `Σ批次入库 - Σ消耗` (事务保护) | 每次消耗/补货 |
| 综合均价 | `Σ批次总金额 ÷ Σ批次数量` | 每次补货 |
| 续费剩余天数 | `nextBillingDate - today` (≥0) | 页面加载 |
| 订阅进度条 | `剩余天数 ÷ 周期天数 × 100%` | 页面加载 |
| 次卡进度 | `剩余次数 ÷ totalTimes × 100%` | 充值后进度条重置 |
| 量卡进度 | `余额 ÷ totalTopup × 100%` | 充值后进度条重置 |
| 按量进度 | `apiBalance ÷ totalCharged × 100%` | 充值后进度条重置 |

---

## 七、CSS 架构

单文件 `theme.css` (~1260 行)，结构如下：

```
1. @font-face (手绘字体)
2. :root / body.light CSS 变量 (双主题，共 ~60 个变量)
3. 全局 reset (* { box-sizing, user-select })
4. 布局组件 (window-container, sidebar, main-canvas, view-header, user-drawer)
5. 欢迎页 + 登录注册 (.welcome-overlay, .auth-overlay)
6. 按钮系统 (.btn-primary, .btn-action-sm, .btn-secondary-sm, .quick-action-btn)
7. 大盘 (.dashboard-row, .dash-card, .timeline-container)
8. 实体卡片 (.asset-card, .card-meta, .card-core-value, .tag, .stock-cup-container)
9. 数字订阅卡片 (.sub-card, .sub-progress-bar)
10. 抽屉 (.modal-overlay, .drawer-content, .form-group, .segmented-control)
11. 弹窗 (.pop-modal-overlay, .pop-modal-content)
12. 时间轴 (.history-timeline)
13. Toast (.toast-container, .win-toast)
14. 成就 (.achievement-card, .ach-icon, .ach-progress-bar)
15. 洞察 (.insight-panel, .insight-item)
16. 手绘覆写 (最重要的一块，!important 优先级最高):
    - 不对称圆角: 255px 15px 225px 15px / 15px 225px 15px 255px
    - 炭笔框: 1.5px solid var(--primary-dark)
    - 硬阴影: 3px 3px 0px rgba(34,34,34,0.1)
    - 按钮悬浮: color-mix + translate(2px,2px)
    - 蜡笔底色: ::before 伪元素 + data-category 属性驱动
    - 进度条: 全高度液体填充 (position:absolute; height:100%)
    - 全局去荧光: * { box-shadow:none!important; text-shadow:none!important }
```

### 色彩体系

- **跨模式共用色（9 个）**: `--primary` (#CBAF88 奶茶), `--success` (#8FAF96 抹茶绿), `--warning` (#D9A87C 焦糖琥珀), `--danger` (#D98880 草莓粉), `--accent-green` (#55876F), `--accent-blue` (#6B8A9E), `--accent-amber` (#B38650), `--accent-rose` (#A65252), 4 个蜡笔色
- **深色模式变量（12 个）**: 暗灰底色系 (#1E2022, #272A2E, …) + 白色文字 (#E8E8E8, #FFFFFF)
- **浅色模式变量（12 个）**: 燕麦奶白系 (#F6F5F0, #FFFFFF, …) + 炭黑文字 (#222222)
- 主题切换: `document.body.classList.toggle('light')`，偏好通过 `setConfig('theme', ...)` 持久化

---

## 八、窗口与 DPI 适配

| 参数 | 值 |
|------|-----|
| 初始窗口 | 1040 × 700 |
| 最小窗口 | 900 × 640 |
| 窗口标题 | "归藏" |
| HTTP 端口 | 18080 |
| DPI 缩放 | Chromium 内核自动响应 Windows 125%/150%/200% 系统缩放 |
| CSS 基准 | `html { font-size: 100% }` → 浏览器基准 16px，随 DPI 自动缩放 |
| 弹性布局 | CSS Grid `auto-fill, minmax()` 替代固定 px 宽度，窗口缩放自动换行 |

**网格自适应断点**:
- `items-grid`: `repeat(auto-fill, minmax(280px, 1fr))` — 窄窗口 2 列 → 宽窗口 4 列
- `sub-grid`: `repeat(auto-fill, minmax(240px, 1fr))` — 窄窗口 2 列 → 宽窗口 5 列
- `dashboard-row`: `repeat(auto-fit, minmax(220px, 1fr))` — 窄窗口 2 列 → 宽窗口 3 列
- `achievement-grid`: `repeat(auto-fill, minmax(240px, 1fr))`

---

## 九、运行模式与部署

### 9.1 双模式启动

```
java -jar guicang.jar           → JCEF 桌面窗口模式（默认）
java -jar guicang.jar --browser → 跳过 JCEF，启动 HTTP 服务 + 系统托盘 + 打开系统浏览器
java -jar guicang.jar --no-jcef → 同 --browser
```

### 9.2 浏览器模式流程
1. 启动 HTTP 服务器 (localhost:18080)
2. 用 `Desktop.getDesktop().browse()` 打开系统默认浏览器
3. 创建系统托盘图标（金色"归"字圆点），右键菜单：打开归藏 / 退出
4. 关闭窗口即退出服务

### 9.3 JCEF 桌面模式流程
1. 初始化 SQLite → 启动 HTTP 服务器
2. 通过 jcefmaven 加载 Chromium 原生库
3. 创建 JCEF Browser 嵌入 Swing JFrame，加载 `http://localhost:18080`
4. 关闭窗口 → browser.close() → cefApp.dispose() → 数据库连接关闭

### 9.4 降级策略
若 JCEF 初始化失败（缺少原生库 / 环境不兼容），自动降级为系统浏览器模式。

### 9.5 离线打包清单
所有文件打包在 fat-jar 的 classpath 中：
- `web/index.html` — 前端入口
- `web/css/theme.css` — 全部样式
- `web/js/app.js` + `bridge.js` + `config.js` — 前端逻辑
- `web/js/lib/vue.esm-browser.prod.js` — Vue 3 ES Module 运行时
- `web/fonts/` — 手绘字体 (MuYaoSuiXinTi.woff2)
- `db/schema.sql` — DDL + 36 成就预置数据
- FontAwesome 通过 CDN 加载（`cdnjs.cloudflare.com`），**不是离线**的

---

## 十、关键设计决策

| 决策点 | 选择 | 理由 |
|--------|------|------|
| 前后端通信 | 内嵌 HTTP Server (JDK) + fetch | 零外部依赖，JDK 自带，JCEF/浏览器通用 |
| 前端框架 | Vue 3 ES Module (importmap) | 响应式系统解决多视图状态同步，importmap 无需构建工具 |
| 前端文件组织 | 单文件 app.js (500 行) | 当前规模下单文件足够，无需模块拆分 |
| 数据库 | SQLite via JDBC | 单文件、零服务、事务安全 |
| 密码存储 | BCrypt | 本地数据库也不能明文 |
| 用户会话 | 内存 currentUser (无 Token) | 本地桌面应用，关闭即登出 |
| 窗口框架 | Swing JFrame + JCEF Chromium | 完整 CSS 支持，现代 Web 标准全兼容 |
| 构建 | Maven shade-plugin (fat-jar) | 双击运行，单文件分发 |
| 金融操作 | SQLite 事务 (BEGIN/COMMIT/ROLLBACK) | 库存变动 + 流水写入原子操作 |
| 成就检查 | 每次操作后全量遍历 36 条 | 数据量小，全量检查简单可靠 |
| 订阅续费 | 每次 getAllAssets 时自动续费 | 懒续费模式，无需定时任务 |
| 文案归属 | 前端 messageMap (COAT_MSGS) | 后端返回原始数据，前端查表渲染，改文案不动后端 |
| 阈值归属 | 前端 enrichAsset() 中的条件判定 | 后端返回 costRatio，前端根据阈值决定 UI 状态（颜色/标签/文案） |
| DPI 适配 | rem + Chromium 内核自动缩放 | Windows 缩放设置自动生效，无需手动处理 |
| 静态资源 | classpath 内嵌 | 离线 100% 可用（除 FontAwesome CDN） |

---
