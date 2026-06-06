  🏗️  "归藏" 全栈重构架构设计

  一、总体架构

  ┌──────────────────────────────────────────────────────────────────┐
  │                   Java 桌面壳 (Swing + JCEF)                      │
  │  ┌────────────────────────────────────────────────────────────┐  │
  │  │                JFrame (900×600, setMinSize)                │  │
  │  │  ┌──────────────────────────────────────────────────────┐  │  │
  │  │  │          JCEF Browser (Chromium 完整内核)            │  │  │
  │  │  │  ┌────────────────────────────────────────────────┐  │  │  │
  │  │  │  │          Vue 3 前端 (index.html)               │  │  │  │
  │  │  │  │  ┌──────────┐ ┌──────────┐ ┌───────────────┐  │  │  │  │
  │  │  │  │  │  Views   │ │  State   │ │  JS Bridge    │  │  │  │  │
  │  │  │  │  │ (5 pages)│ │ (reactive)│ │  Caller       │  │  │  │  │
  │  │  │  │  └──────────┘ └──────────┘ └───────┬───────┘  │  │  │  │
  │  │  │  └────────────────────────────────────┼──────────┘  │  │  │
  │  │  └───────────────────────────────────────┼─────────────┘  │  │
  │  │                                          │                 │  │
  │  │    window.javaBackend.xxx()              │  JCEF JS回调    │  │
  │  │    (CefMessageRouter / JS Bindings)      │                 │  │
  │  ├──────────────────────────────────────────┼─────────────────┤  │
  │  │              Java 业务层                  │                 │  │
  │  │  ┌───────────────────────────────────────┼───────────────┐  │  │
  │  │  │  JavaBackend (注入到 JCEF Browser)    │               │  │  │
  │  │  │  getAllAssets() / checkIn() / login() ...             │  │  │
  │  │  ├───────────────────────────────────────────────────────┤  │  │
  │  │  │  Service Layer (业务逻辑)                              │  │  │
  │  │  │  UserService / AssetService / LongTermService         │  │  │
  │  │  │  StockpileService / AchievementService / InsightService│  │  │
  │  │  ├───────────────────────────────────────────────────────┤  │  │
  │  │  │  Repository Layer (数据访问)                           │  │  │
  │  │  │  JDBC → SQLite                                       │  │  │
  │  │  └───────────────────────────────────────────────────────┘  │  │
  │  └────────────────────────────────────────────────────────────┘  │
  │                                                                  │
  │  ┌────────────────────────────────────────────────────────────┐  │
  │  │  SQLite 数据库文件 (项目本地目录，单文件)                    │  │
  │  │  assets.db — 用户表 / 资产表 / 流水表 / 成就表 / 批次表   │  │
  │  └────────────────────────────────────────────────────────────┘  │
  └──────────────────────────────────────────────────────────────────┘

  核心变化（对比原始 HTTP 方案）：
  - 废弃 HTTP REST Server → 改为 JCEF Browser 内嵌 + JS 回调桥接
  - 前端不再是独立浏览器页面 → 运行在 Swing JFrame 内的 Chromium 中
  - JCEF 提供完整 Chromium 内核 → color-mix() / CSS Grid / 所有现代 CSS 100% 兼容
  - Vue 3 不再走 CDN → Vue 库和所有静态资源打包到项目本地目录
  - 数据流：前端 JS → window.javaBackend.xxx() → Java Service → SQLite

  核心原则不变：HTML 定义"有什么功能、长什么样"，Java 负责"数据怎么算、怎么存、怎么验证"。

  ---
  二、HTML 中隐含的完整数据模型 → 数据库设计

  HTML 暴露了 9 种资产类型，梳理如下：

  资产分类体系（来自 HTML 各卡片）
  │
  ├── 🟢 实体物品 (Physical)
  │   ├── 细水长流·按次   → 单价打卡，每用一次均价下降
  │   ├── 细水长流·按天   → 按持有天数算日均成本
  │   ├── 储备幸福         → 库存管理 + 多批次均价 + 消耗记录 + 比价
  │   └── 收藏状态         → 纯记录 + 状态切换(日常使用/完美珍藏/计划转手)
  │
  ├── 🔵 数字订阅 (Digital Subscription)
  │   ├── 周期续费·按月   → 固定月/季/年费 + 续费倒计时
  │   ├── 周期续费·按季
  │   ├── 周期续费·按年
  │   ├── 按量计费         → 预充值余额 + 按调用消耗
  │   └── 永久有效         → 一次性买断
  │
  └── 🟠 会员储值 (Stored-value / Membership)
      ├── 储值次卡         → 固定次数 + 按次核销 + 可续次
      └── 储值量卡         → 储值余额 + 按金额消费

  2.1 数据库表设计（以 SQLite 为例）

  -- ==================== 用户 ====================
  CREATE TABLE users (
      id            INTEGER PRIMARY KEY AUTOINCREMENT,
      username      TEXT    NOT NULL UNIQUE,          -- 登录账号 (不可修改)
      password_hash TEXT    NOT NULL,                 -- bcrypt 哈希
      nickname      TEXT    NOT NULL,                 -- 显示昵称 (可修改)
      email         TEXT,                             -- 可选邮箱
      avatar_index  INTEGER DEFAULT 0,               -- 头像图标索引
      theme         TEXT    DEFAULT 'dark',           -- 'dark' | 'light'
      created_at    TEXT    NOT NULL                  -- 注册日期
  );

  -- ==================== 统一物品表 ====================
  -- 用 asset_type 区分 9 种类型，每种类型的专属字段可 NULL
  CREATE TABLE assets (
      id              TEXT    PRIMARY KEY,            -- UUID 短码
      user_id         INTEGER NOT NULL REFERENCES users(id),
      asset_type      TEXT    NOT NULL,               -- 见下方 AssetType 枚举
      name            TEXT    NOT NULL,
      icon            TEXT,                           -- FontAwesome class, 如 fa-solid fa-shirt
      category        TEXT,                           -- 分类标签
      purchase_price  REAL,                           -- 购入价/首充金额
      purchase_date   TEXT,                           -- yyyy-MM-dd
      notes           TEXT,                           -- 备注

      -- 细水长流·按次
      usage_count     INTEGER DEFAULT 0,             -- 打卡次数

      -- 细水长流·按天
      expected_lifespan_years INTEGER,               -- 可选：预期使用年限，后端用于计算预期日均成本对比，前端不必须展示

      -- 储备幸福 (囤货)
      current_stock   REAL,                           -- 当前库存
      safety_stock    REAL,                           -- 安全库存线

      -- 收藏状态（手镯/翡翠/相机 — 纯陪伴追踪，不计均摊，无序列号/保修字段）
      collect_status  TEXT,                           -- '日常使用中' | '完美珍藏中' | '计划转手中'

      -- 周期续费 (订阅)
      billing_cycle   TEXT,                           -- 'MONTHLY' | 'QUARTERLY' | 'YEARLY'
      monthly_cost    REAL,                           -- 每期费用
      next_billing_date TEXT,                         -- 下次扣费日期

      -- 按量计费
      api_balance     REAL,                           -- 当前余额
      total_charged   REAL,                           -- 累计充值

      -- 储值次卡
      remaining_times INTEGER,                        -- 剩余次数
      total_times     INTEGER,                        -- 累计购买次数
      total_topup     REAL,                           -- 累计充值金额

      -- 储值量卡
      card_balance    REAL,                           -- 当前储值余额
      total_spent     REAL,                           -- 累计消费

      -- 通用状态
      is_archived     INTEGER DEFAULT 0,             -- 0=正常, 1=已归档(仓库)
      created_at      TEXT,
      updated_at      TEXT
  );

  -- ==================== 消耗/使用历史 ====================
  CREATE TABLE usage_logs (
      id          INTEGER PRIMARY KEY AUTOINCREMENT,
      asset_id    TEXT    NOT NULL REFERENCES assets(id),
      action_type TEXT    NOT NULL,  -- 'CHECK_IN' | 'CONSUME' | 'PUNCH' | 'TOPUP' | 'SPEND' | 'RESTOCK' | 'STATUS_CHANGE'
      quantity    REAL,              -- 消耗数量/金额
      unit_price  REAL,              -- 操作时单价(用于比价)
      notes       TEXT,
      created_at  TEXT    NOT NULL
  );

  -- ==================== 囤货采购批次 ====================
  CREATE TABLE purchase_batches (
      id          TEXT PRIMARY KEY,
      asset_id    TEXT NOT NULL REFERENCES assets(id),
      quantity    INTEGER NOT NULL,
      total_price REAL    NOT NULL,
      batch_date  TEXT    NOT NULL
  );

  -- ==================== 成就系统 ====================
  CREATE TABLE achievement_defs (
      id          TEXT PRIMARY KEY,        -- 'a01' ~ 'a36'
      name        TEXT NOT NULL,
      description TEXT,
      icon        TEXT,                    -- FontAwesome class
      category    TEXT NOT NULL,           -- 成就分类
      goal_type   TEXT NOT NULL,           -- 达成条件类型
      goal_value  REAL NOT NULL            -- 达成阈值
  );

  CREATE TABLE user_achievements (
      user_id         INTEGER NOT NULL REFERENCES users(id),
      achievement_id  TEXT    NOT NULL REFERENCES achievement_defs(id),
      progress        REAL    DEFAULT 0,
      is_completed    INTEGER DEFAULT 0,
      completed_date  TEXT,
      PRIMARY KEY (user_id, achievement_id)
  );

  -- ==================== 通知/洞察 ====================
  -- (轻量方案：洞察实时计算不存储；也可缓存结果于此)

  2.2 AssetType 枚举（对应 HTML 9 种卡片）

  public enum AssetType {
      LONG_TERM_PER_USE,    // 细水长流·按次 — 始祖鸟
      LONG_TERM_PER_DAY,    // 细水长流·按天 — iPhone
      STOCKPILE,            // 储备幸福 — 抽纸
      COLLECTIBLE,          // 收藏状态 — 手镯/翡翠/相机
      SUBSCRIPTION_MONTHLY, // 周期续费·按月 — Netflix/Dropbox
      SUBSCRIPTION_QUARTERLY, // 周期续费·按季 — Adobe CC
      SUBSCRIPTION_YEARLY,  // 周期续费·按年 — iCloud+
      SUBSCRIPTION_METERED, // 按量计费 — OpenAI API
      SUBSCRIPTION_LIFETIME,// 永久有效 — Infuse Pro
      STORED_TIME_CARD,     // 储值次卡 — 健身/SPA
      STORED_AMOUNT_CARD    // 储值量卡 — 餐厅
  }

  ---
  三、Java 后端 + JCEF 桌面壳架构设计

  3.1 技术选型

  ┌─────────────┬────────────────────────────────────────┬──────────────────────────────────────────────────┐
  │    层面     │               最终选择                 │                      理由                        │
  ├─────────────┼────────────────────────────────────────┼──────────────────────────────────────────────────┤
  │ 窗口框架    │ Swing JFrame + JCEF (Chromium 内核)    │ 完整 Chromium → color-mix/CSS Grid 等全兼容      │
  ├─────────────┼────────────────────────────────────────┼──────────────────────────────────────────────────┤
  │ JS ↔ Java   │ JCEF CefMessageRouter / JS Bindings    │ 前端直接调用 Java 方法，无需 HTTP 协议栈          │
  │ 通信        │ 注入 window.javaBackend 桥接对象        │                                                  │
  ├─────────────┼────────────────────────────────────────┼──────────────────────────────────────────────────┤
  │ 数据库      │ SQLite (via JDBC, 项目本地目录)        │ 零外部依赖，单文件，桌面应用标配                  │
  ├─────────────┼────────────────────────────────────────┼──────────────────────────────────────────────────┤
  │ SQL 访问    │ JDBC Template (原生 JDBC)              │ 最轻量，无额外 ORM 依赖，SQL 完全可控             │
  ├─────────────┼────────────────────────────────────────┼──────────────────────────────────────────────────┤
  │ 密码        │ BCrypt (jbcrypt)                       │ 本地数据库也不能明文存密码                        │
  ├─────────────┼────────────────────────────────────────┼──────────────────────────────────────────────────┤
  │ JSON        │ Gson                                  │ Java ↔ JS 桥接传 JSON 字符串                     │
  ├─────────────┼────────────────────────────────────────┼──────────────────────────────────────────────────┤
  │ 构建        │ Maven + maven-shade-plugin (fat-jar)   │ 依赖管理 + 打包单 JAR 双击运行                    │
  ├─────────────┼────────────────────────────────────────┼──────────────────────────────────────────────────┤
  │ JCEF 管理   │ jcefmaven (自动下载 Chromium 二进制)    │ Maven 依赖引入，首次构建自动下载 ~200MB            │
  ├─────────────┼────────────────────────────────────────┼──────────────────────────────────────────────────┤
  │ 静态资源    │ 全部本地化，不依赖 CDN                  │ 桌面软件必须离线可用                              │
  └─────────────┴────────────────────────────────────────┴──────────────────────────────────────────────────┘

  3.2 Java 项目结构（JCEF 嵌入版）

  java大作业/
  ├── pom.xml
  ├── src/main/java/com/guizang/
  │   ├── App.java                          ← 入口: 创建 JFrame → 初始化 CefApp → 加载 index.html
  │   │                                         注入 JavaBackend 到 JS 上下文 → 显示窗口
  │   ├── config/
  │   │   ├── DatabaseConfig.java           ← SQLite 连接 + 启动时执行 DDL 建表
  │   │   └── AppConfig.java                ← 窗口尺寸(900×600)、最小宽高、标题"归藏"
  │   │
  │   ├── bridge/                           ← ★ JCEF JS 桥接对象
  │   │   └── JavaBackend.java              ← 所有暴露给前端的 Java 方法
  │   │        // JCEF 注入方式:
  │   │        // CefMessageRouter / JS Bindings
  │   │        // 前端调用: window.javaBackend.getAllAssets()
  │   │
  │   ├── service/                          ← 业务逻辑层 (大部分复用+扩展)
  │   │   ├── UserService.java              ← 注册/登录 + BCrypt 验证
  │   │   ├── AssetService.java             ← 统一 CRUD
  │   │   ├── LongTermService.java          ← 日均成本、陪伴打卡 + 返回状态标识给前端
  │   │   ├── StockpileService.java         ← 库存、批次、均价、比价（事务保护）
  │   │   ├── CollectibleService.java       ← 收藏状态管理
  │   │   ├── SubscriptionService.java      ← ★新增
  │   │   ├── StoredCardService.java        ← ★新增
  │   │   ├── AchievementService.java       ← 36 成就检查（每次操作后自动调用）
  │   │   └── InsightService.java           ← 智能洞察生成（返回计算好的文本列表）
  │   │
  │   ├── repository/                       ← 数据访问层 (替代原 dao/)
  │   │   ├── UserRepository.java
  │   │   ├── AssetRepository.java
  │   │   ├── UsageLogRepository.java
  │   │   ├── PurchaseBatchRepository.java
  │   │   └── AchievementRepository.java
  │   │
  │   ├── model/                            ← 数据模型
  │   │   ├── User.java
  │   │   ├── Asset.java
  │   │   ├── UsageLog.java
  │   │   ├── PurchaseBatch.java
  │   │   ├── Achievement.java
  │   │   └── UserAchievement.java
  │   │
  │   └── util/                             ← 工具类 (大部分复用)
  │       ├── DateUtil.java
  │       ├── Validator.java
  │       └── Categories.java
  │
  └── src/main/resources/
      ├── db/migration/
      │   └── V1__init.sql                  ← 建表 + 成就定义 + 默认数据
      ├── web/                              ← ★ 前端文件 (打包进 JAR)
      │   ├── index.html
      │   ├── css/
      │   ├── js/
      │   │   ├── vue.global.prod.js        ← Vue 3 本地库文件
      │   │   └── ...                       ← 其他 JS 模块
      │   └── fonts/
      │       ├── fa-solid-900.woff2        ← FontAwesome 本地字体
      │       └── MuYaoSuiXinTi.woff2       ← 手绘风字体本地文件
      └── application.properties

  3.3 JS 桥接通信机制（JCEF JS Bindings）

  Java 端注入（JCEF）：
  ```
  // JCEF CefApp 初始化后，通过 CefMessageRouter 注入 Java 回调
  // JavaBackend 的每个 public 方法映射为 JS 全局函数
  CefBrowser browser = client.createBrowser("...");
  // 注入桥接: 前端调用 window.javaBackend.xxx() → Java 方法
  browser.executeJavaScript(
      "window.javaBackend = {"
      + "  getAllAssets: function() { return JavaBackend.getAllAssets(); },"
      + "  login: function(u,p) { return JavaBackend.login(u,p); },"
      + "  checkIn: function(id) { return JavaBackend.checkIn(id); },"
      + "  ..."
      + "};", "", 0
  );
  ```

  前端调用方式：
  ```javascript
  // 所有数据交互走 JCEF JS 桥接，无需 fetch/HTTP
  const assets = JSON.parse(window.javaBackend.getAllAssets())
  const result = JSON.parse(window.javaBackend.checkIn(assetId))
  ```

  桥接方法清单（JavaBackend 暴露的全部方法）：

  ═══ 认证 ═══
  login(username, password)         → JSON { success, message, user? }
  register(username, password, nickname) → JSON { success, message }
  getSessionUser()                  → JSON { loggedIn, user? }

  ═══ 资产 ═══
  getAllAssets()                    → JSON [ Asset, ... ]
  getAssetById(id)                  → JSON Asset
  createAsset(json)                 → JSON { success, asset }
  updateAsset(id, json)             → JSON { success, asset }
  deleteAsset(id)                   → JSON { success }
  archiveAsset(id)                  → JSON { success }
  restoreAsset(id)                  → JSON { success }

  ═══ 操作 ═══
  checkIn(id)                       → JSON { asset, dashboard_update, new_achievements }
  consumeStock(id, qty, notes)      → JSON { ... }
  restock(id, qty, totalPrice)      → JSON { ... }
  punchCard(id)                     → JSON { ... }
  topup(id, amount, times?)         → JSON { ... }
  spend(id, amount)                 → JSON { ... }
  updateCollectStatus(id, status)   → JSON { ... }
  comparePrice(id, externalPrice)   → JSON { ... }

  ═══ 大盘/成就/洞察 ═══
  getDashboard()                    → JSON { ... }
  getAchievements()                 → JSON [ AchievementProgress, ... ]
  getInsights()                     → JSON [ Insight, ... ]

  ═══ 配置 ═══
  getConfig(key)                    → JSON { value }
  setConfig(key, value)             → void
  getHistory(assetId)               → JSON [ UsageLog, ... ]
  exportData()                      → JSON { ... }  全量备份

  3.4 关键业务逻辑说明

  陪伴打卡 (check-in) — 最核心的交互：
  每点一次打卡按钮:
    1. usage_count += 1
    2. 计算当前均摊 = purchasePrice / usage_count
    3. 根据 ratio 返回对应文案 (对应 HTML 中 coatMsg 的 5 档文案)
    4. 写入 usage_logs
    5. 触发成就重新检查 (日省一文 / 物尽其用 等)
    6. 返回更新后的资产数据 + 可能的新成就通知

  囤货补货 + 比价反馈：
  补货时:
    1. 创建 PurchaseBatch 记录
    2. 重新计算综合均价 = Σ(批次总金额) / Σ(批次数量)
    3. 与历史最低/最高价对比，返回比价文案
    4. 更新库存，检查是否触发库存预警

  ---
  四、Vue 3 前端重构设计（离线桌面版）

  4.0 Vue 3 引入方式：本地文件而非 CDN

  桌面应用必须离线可用 → Vue 3 运行时库下载到项目 `src/main/resources/web/js/vue.global.prod.js`，
  HTML 中用相对路径引入：

  ```html
  <script src="./js/vue.global.prod.js"></script>
  ```

  同理，FontAwesome 图标字体文件（.woff2）和手绘风字体（沐瑶随心体等）全部下载到
  `web/fonts/` 目录，CSS 中 `@font-face` 的 `src: url()` 全部改为相对路径。

  4.1 技术选型：Vue 3 + CSS Grid 自适应布局

  核心理由：

  1. 本应用的 UI 复杂度主要来自 "同一份数据驱动多个视图，且每个视图有多层阈值条件判断"——这正是 Vue 的响应式系统要解决的问题

  2. 当前 HTML 已有大量状态驱动 UI 的模式：
     - if 库存 < 3 → 标签变黄
     - if ratio ≤ 10% → 成本数字变绿 + 标签从"细水长流"变成"物尽其用"
     - if 次卡耗尽 → 按钮禁用 + Toast 通知
     - 大盘 + 物尽其用页 + 侧边栏统计 → 共享同一份资产数据

  3. Vue 3 vs Vanilla JS 对比（聚焦本项目的实际场景）：

  ┌──────────────────────┬────────────────────────────────────┬───────────────────────────────────────┐
  │        场景          │         Vanilla JS 做法            │           Vue 3 做法                   │
  ├──────────────────────┼────────────────────────────────────┼───────────────────────────────────────┤
  │ 打卡后更新 UI        │ 手动改 7+ 个 DOM 元素              │ 改一个 ref 值，所有绑定自动更新       │
  │                      │ (coatCount, coatCost, dashCoatCost,│ (v-text / v-bind 自动同步)            │
  │                      │  dashCoatDays, coatMsg, coatTag...)│                                       │
  ├──────────────────────┼────────────────────────────────────┼───────────────────────────────────────┤
  │ 多阈值颜色/文案切换  │ 手写 if-else 链 + classList.add   │ computed 属性自动推导                  │
  │                      │ + innerHTML 拼接，容易漏更新点     │ :class 绑定 + 条件自动求值             │
  ├──────────────────────┼────────────────────────────────────┼───────────────────────────────────────┤
  │ 9 种资产卡片渲染     │ 每种类型写一套 innerHTML 模板字符串│ 一个 AssetCard 组件，v-if/v-else-if    │
  │                      │ 或者一个巨型 switch-case 函数      │ 按 asset_type 切换模板区块             │
  ├──────────────────────┼────────────────────────────────────┼───────────────────────────────────────┤
  │ 列表渲染 (36 成就)   │ achievements.map(...).join('')     │ v-for="ach in achievements"            │
  │                      │ + 每次数据变手动 rebuildGrid()     │ 数据变 → 自动重渲染                    │
  ├──────────────────────┼────────────────────────────────────┼───────────────────────────────────────┤
  │ 条件渲染             │ element.style.display = 'none'     │ v-if / v-show                          │
  │ (抽屉开闭/空态提示)  │ classList.toggle('open')          │ 声明式，逻辑清晰                       │
  ├──────────────────────┼────────────────────────────────────┼───────────────────────────────────────┤
  │ 表单双向绑定         │ input.addEventListener + 手动取值  │ v-model 自动同步                       │
  │ (登录/新增资产)      │                                    │                                       │
  ├──────────────────────┼────────────────────────────────────┼───────────────────────────────────────┤
  │ 状态共享             │ 手写发布/订阅 EventEmitter         │ reactive() + provide/inject            │
  │ (大盘↔卡片↔侧边栏)  │ 或者在全局变量上手动调用刷新函数   │ 或一个简单的 shared reactive store     │
  └──────────────────────┴────────────────────────────────────┴───────────────────────────────────────┘

  Vue 3 本地文件的代价：
  - Vue 运行时 ~120KB，作为本地文件加载极快，离线 100% 可用
  - 不能用 .vue 单文件（无构建工具链），用 `defineComponent({ template })` 或直接在 HTML 中写 `v-` 指令
  - 学习成本：ref / computed / v-if / v-for / watch 半天可上手，都是 JS 概念的直接映射

  4.2 前端文件结构（Vue 3 CDN 模式）

  frontend/
  ├── index.html                  ← 主 HTML 骨架
  │                                  引入: Vue 3 CDN + FontAwesome CDN
  │                                  包含: #app 挂载点 + 全局 CSS + 手绘风格覆写
  │
  ├── css/
  │   ├── theme.css               ← CSS 变量 + 双主题 (从 HTML <style> 抽取)
  │   ├── components.css          ← 组件样式 (卡片/按钮/表单/抽屉/进度条)
  │   └── handdrawn.css           ← 喜茶手绘风格覆写
  │
  ├── js/
  │   ├── app.js                  ← 入口: createApp(), 注册全局组件, 挂载
  │   ├── config.js               ← API_BASE_URL, 常量定义
  │   │
  │   ├── api/                    ← API 调用层 (纯函数，返回 Promise)
  │   │   ├── client.js           ← fetch 封装 (自动带 JWT、统一错误处理)
  │   │   ├── auth.js             ← login(), register(), getMe()
  │   │   ├── assets.js           ← CRUD + 操作 (checkIn, consume, restock...)
  │   │   ├── dashboard.js        ← getDashboard()
  │   │   ├── achievements.js     ← getAchievements(), checkAchievements()
  │   │   └── insights.js         ← getInsights()
  │   │
  │   ├── stores/                 ← Vue 响应式状态 (替代手写发布/订阅)
  │   │   ├── authStore.js        ← reactive({ user, token, isLoggedIn })
  │   │   └── assetStore.js       ← reactive({ assets: [], archivedAssets: [] })
  │   │                              + 派生数据: computed 计算大盘统计、侧边栏数据
  │   │
  │   ├── composables/            ← Vue 组合式函数 (可复用逻辑)
  │   │   ├── useAuth.js          ← 登录态管理、token 持久化到 localStorage
  │   │   ├── useAssets.js        ← 资产加载/缓存/操作
  │   │   ├── useAchievements.js  ← 成就数据 + 新解锁检测 + 通知
  │   │   ├── useTheme.js         ← 主题切换 + localStorage 持久化
  │   │   └── useToast.js         ← Toast 通知队列管理
  │   │
  │   ├── components/             ← Vue 组件 (用 defineComponent 或 template 字符串)
  │   │   ├── AppShell.js         ← 整体布局: 侧边栏 + 主画布 + 用户抽屉
  │   │   ├── AuthOverlay.js      ← 登录/注册浮层
  │   │   ├── WelcomeOverlay.js   ← 欢迎页开场动画
  │   │   ├── NavSidebar.js       ← 左侧导航栏
  │   │   ├── UserDrawer.js       ← 用户侧边抽屉 (头像/统计/设置)
  │   │   ├── AssetCard.js        ← ★核心: 资产卡片组件
  │   │   │                         根据 asset.asset_type 自动渲染对应模板:
  │   │   │                         - 细水长流·按次: 打卡按钮 + 单次成本 + 5 档文案
  │   │   │                         - 细水长流·按天: 持有天数 + 日均成本
  │   │   │                         - 储备幸福: 库存量杯 + 消耗/补货按钮 + 比价条
  │   │   │                         - 收藏状态: 陪伴天数 + 状态标签(三态循环)
  │   │   │                         - 周期续费: 续费倒计时 + 进度条 + 暂停按钮
  │   │   │                         - 按量计费: 余额 + 充值/消费
  │   │   │                         - 储值次卡: 剩余次数 + 核销/充值
  │   │   │                         - 储值量卡: 余额 + 充值/消费
  │   │   │                         - 永久有效: 固定 100% 进度条
  │   │   ├── AssetDrawer.js      ← 右侧抽屉: 新增/编辑资产表单
  │   │   ├── AssetHistory.js     ← 时光轴历史抽屉
  │   │   ├── ToastContainer.js   ← Toast 通知容器
  │   │   ├── ConfirmModal.js     ← 通用确认弹窗
  │   │   ├── ProgressBar.js      ← 液体填充进度条
  │   │   ├── AchievementCard.js  ← 单张成就卡片 (图标/进度条/锁定态)
  │   │   └── InsightItem.js      ← 单条洞察建议
  │   │
  │   └── utils/
  │       ├── formatter.js        ← 金额/日期格式化
  │       ├── daysCalculator.js   ← 天数计算
  │       ├── messageMap.js       ← ★前端文案映射表 (后端返回 key，前端查表得中文文案)
  │       └── validators.js       ← 前端表单验证

  4.3 核心重构：从硬编码到 Vue 响应式数据驱动

  重构前（当前 HTML — 硬编码 + 手动 DOM 操作）：
  // 每种卡片单独一套全局变量 + 手动更新函数
  let coatPrice = 4500, coatCount = 0;
  function clickCoat() {
      coatCount++;
      document.getElementById('coatCount').innerText = coatCount;        // 改按钮
      document.getElementById('coatCost').innerText = currentCost;        // 改价格
      document.getElementById('dashCoatCost').innerText = currentCost;    // 改大盘
      document.getElementById('dashCoatDays').innerText = cd;             // 改天数
      // 手动判断 5 档文案 + 颜色
      if (ratio <= 0.10) {
          msg.innerText = "你们已经是形影不离的老朋友了...";
          document.getElementById('coatCost').classList.add('status-perfect');
          tag.className = "tag success";
          tag.innerHTML = '<i class="fa-solid fa-leaf"></i> 物尽其用';
      } else if (ratio <= 0.30) { /* ... */ }
      // ... 还有 4 个分支

      refreshInsightAndAchievements();  // 手动刷新成就+洞察+侧边栏
  }

  let currentStock = 2;
  function consumeStock() { currentStock--; /* 手动更新库存 UI + 标签颜色 */ }
  let gymTimes = 12, gymTotalTopup = 600;
  function clickGym() { gymTimes--; /* 手动更新 UI + 按钮禁用 */ }

  重构后（Vue 3 — 声明式数据绑定）：
  // === AssetCard 组件核心逻辑（伪代码示意） ===
  // 组件接收一个 asset 对象（来自 API），所有 UI 自动跟随数据变化

  <template>
    <div class="asset-card" :data-category="asset.category" :class="{ 'critical-alert': isLowStock }">
      <!-- 头部：名称 + 标签 -->
      <div class="card-meta">
        <h4><i :class="asset.icon"></i> {{ asset.name }}</h4>
        <span class="tag" :class="tagClass">
          <i :class="tagIcon"></i> {{ tagText }}
        </span>
      </div>

      <!-- 核心数值（自动变色） -->
      <div class="card-core-value">
        <span class="num" :class="{ 'status-perfect': isStatusPerfect }">
          {{ formattedCost }}
        </span>
      </div>

      <!-- 人文文案（自动根据阈值切换） -->
      <p>{{ statusMessage }}</p>

      <!-- 操作栏 -->
      <div class="card-action-row">
        <button @click="handleAction">{{ actionLabel }}</button>
      </div>
    </div>
  </template>

  <script>
  // 所有状态是 ref/reactive，所有推导是 computed
  const coatCount = ref(asset.usage_count)

  const currentCost = computed(() =>
      coatCount.value > 0 ? (asset.purchase_price / coatCount.value).toFixed(2) : asset.purchase_price
  )
  const costRatio = computed(() =>
      coatCount.value > 0 ? (asset.purchase_price / coatCount.value) / asset.purchase_price : 1
  )
  const isStatusPerfect = computed(() => costRatio.value <= 0.10)

  // 文案自动映射（阈值在前端判断，messageMap 在前端维护）
  const statusMessage = computed(() => {
      if (coatCount.value === 0) return MESSAGES.coat.start
      if (costRatio.value <= 0.10) return MESSAGES.coat.perfect    // ← 可随时改，不动后端
      if (costRatio.value <= 0.30) return MESSAGES.coat.great
      if (costRatio.value <= 0.50) return MESSAGES.coat.good
      if (costRatio.value <= 0.70) return MESSAGES.coat.warm
      return MESSAGES.coat.first
  })

  const tagClass = computed(() => isStatusPerfect.value ? 'tag success' : 'tag')
  const tagText = computed(() => isStatusPerfect.value ? '物尽其用' : '细水长流')
  const tagIcon = computed(() => isStatusPerfect.value ? 'fa-leaf' : 'fa-star')

  // 打卡：一行代码，全部 UI 自动更新
  async function handleAction() {
      const updated = await api.assets.checkIn(asset.id)
      Object.assign(asset, updated)   // 更新响应式数据 → UI 自动刷新
      achievementStore.checkAll()     // 触发成就检查
      dashboardStore.refresh()        // 大盘自动更新
  }
  </script>

  这种模式下，当你想把 "物尽其用" 改成 "不负相遇" 时，
  只需改 messageMap.js 中的一行，无需碰业务逻辑，无需碰后端 API。

  4.4 阈值驱动的 UI 变化（完整梳理）

  这是"归藏"产品温度感的核心，必须在前端架构中明确建模：

  ═══ 细水长流·按次（始祖鸟冲锋衣）═══
  ┌─────────────┬──────────────┬─────────────────────────┬────────────────────┐
  │   ratio     │  数字颜色    │       标签              │      人文文案      │
  ├─────────────┼──────────────┼─────────────────────────┼────────────────────┤
  │ 打卡 1 次   │ 默认黑色     │ "细水长流" 奶茶色 tag  │ "开启了第一次重逢" │
  │ 0.50~0.70   │ 默认         │ "细水长流" 奶茶色 tag  │ "感知到它高频陪伴" │
  │ 0.30~0.50   │ 默认         │ "细水长流" 奶茶色 tag  │ "消费泡沫已被斩断" │
  │ 0.10~0.30   │ 默认         │ "细水长流" 奶茶色 tag  │ "它默默融入日常"   │
  │ **≤ 0.10**  │ **铜绿 #55876F** │ **"物尽其用" 铜绿** │ **"形影不离的老朋友"** │
  └─────────────┴──────────────┴─────────────────────────┴────────────────────┘

  触发条件: ratio = (price / usage_count) / price ≤ 0.10 即打卡次数 ≥ 10 次

  ═══ 储备幸福（得宝抽纸）═══
  ┌────────────┬──────────────────┬──────────────────────┐
  │  库存水位  │     标签         │     行为             │
  ├────────────┼──────────────────┼──────────────────────┤
  │ stock ≥ 3  │ "状态充盈" green │ 正常                 │
  │ 0 < s < 3  │ "余量轻盈" amber │ warning Toast 提醒   │
  │ stock = 0  │ 红色警告         │ danger Toast "库存耗尽" │
  └────────────┴──────────────────┴──────────────────────┘

  ═══ 储值次卡（健身/SPA）═══
  ┌────────────┬──────────────────────────┐
  │ 剩余次数   │     行为                  │
  ├────────────┼──────────────────────────┤
  │ > 0        │ 按钮可用，进度条递减      │
  │ = 0        │ 按钮禁用，Toast "卡券完全履约" │
  └────────────┴──────────────────────────┘

  4.5 API 响应设计原则：后端返回结构化数据，前端掌控展示

  正确的分工方式（示例：打卡 check-in 的 API 响应）：

  {
    // ✅ 原始数据 — 后端算，前端用
    "usage_count": 150,
    "current_cost_per_use": 30.00,
    "cost_ratio": 0.067,
    "days_held": 365,

    // ✅ 状态标识符 — 后端定性，前端用 key 查文案表
    "status_tag": {
      "variant": "success",              // CSS class: tag success / tag warning / tag
      "message_key": "coat_perfect"      // 前端 messageMap["coat_perfect"] → 中文文案
    },

    // ✅ 大盘需要同步的数据
    "dashboard_update": {
      "daily_avg_cost": 45.00
    },

    // ✅ 新解锁的成就（有则返回）
    "new_achievements": [
      { "id": "a09", "name": "物尽其用", "icon": "fa-trophy" }
    ]
  }

  前端 messageMap.js：
  export const MESSAGES = {
    coat: {
      start:   "它在衣柜角落睡得有点久了，下一个雨天，带它一起去看看世界吧。",
      first:   "太棒了，开启了第一次重逢！它正披在肩上为你遮风挡雨。",
      warm:    "感知到它高频陪伴的温度了吗？单次相遇成本已经开始平稳下降...",
      good:    "消费泡沫已被成功斩断一半！每一次使用，都是你对盲目消费主义的一次优雅胜诉。",
      great:   "它默默地融入了你的日常，成为了不用言语的底色...",
      perfect: "你们已经是形影不离的老朋友了，每一次使用都在榨干它当初昂贵的消费泡沫..."
    },
    stock: {
      full:    "状态充盈",
      low:     "余量轻盈",
      empty:   "库存耗尽"
    }
  }

  这样设计后：
  - 想改"物尽其用"为"不负相遇" → 改 messageMap.js 一行，后端不用动
  - 想调整 ratio ≤ 8% 才变绿 → 改 computed 里的阈值常量，后端不用动
  - 想新增一种文案档位 → 前后端各加一个 message_key，属于契约变更

  4.6 认证流程（WebView 本地版）

  1. 页面加载 → 调用 window.javaBackend.getSessionUser()
  2. 返回 { loggedIn: false } → 主页渲染挂起，强制显示登录遮罩
  3. 返回 { loggedIn: true } → 隐藏遮罩，调用 getAllAssets() 等加载数据
  4. 用户提交登录 → 前端收集账号密码 → javaBackend.login(user, pass)
     - Java 用 BCrypt 验证，返回 { success: true/false, message }
     - 失败 → 前端显示错误提示，遮罩不关闭
     - 成功 → 前端隐藏遮罩，加载主页
  5. 无 token 概念 — 因为是本地单用户桌面应用，WebView 进程即会话

  4.7 窗口缩放自适应与 DPI 适配（★ 新增）

  4.7.1 Java 外壳保底

  Java 窗口侧设定最小尺寸，防止用户缩太小导致布局崩溃：

  ```
  // Swing JFrame
  frame.setMinimumSize(new Dimension(900, 600));
  ```

  4.7.2 前端 CSS Grid 弹性布局

  替代写死的 px 卡片宽度，使用 CSS Grid 自适应：

  ```css
  /* 资产卡片网格 — 根据窗口宽度自动换行 */
  .items-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 20px;
  }

  /* 大盘三栏 — 窄窗口时自动折行 */
  .dashboard-row {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 20px;
  }

  /* 成就网格 — 同理 */
  .achievement-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
      gap: 16px;
  }
  ```

  适配效果：
  - 窗口 ≥ 1024px 宽 → 资产卡片一行 3~4 张
  - 窗口 900~1024px → 一行 2~3 张
  - 窗口 = 900px (最小) → 一行 2 张，所有内容仍可读

  4.7.3 Windows DPI 高分辨率缩放

  JCEF 内嵌 Chromium 内核会自动响应 Windows 系统缩放设置（125% / 150% / 200%）。
  前端 CSS 中文字大小使用相对单位配合：

  ```css
  html { font-size: 100%; }  /* 浏览器基准 16px，Chromium 会根据 DPI 自动缩放 */
  h2 { font-size: 1.5rem; }   /* 24px → DPI 125% 时自动变 30px */
  .value { font-size: 2rem; } /* 32px → DPI 150% 时自动变 48px */
  ```

  所有图标（FontAwesome）和 SVG 手绘滤镜也会随 Chromium 内核自动等比缩放，无需额外处理。

  4.8 桌面端原生通知（★ 新增）

  前端保留 showWinToast() 用于普通交互提示。
  Java 后端在检测到严重事件时（库存耗尽、订阅即将到期），可直接拉起 Windows 原生通知：

  ```
  // Java 端 (Swing JOptionPane)
  JOptionPane.showMessageDialog(
      frame,
      "得宝抽纸库存已耗尽，请尽快补货。",
      "归藏 — 库存预警",
      JOptionPane.WARNING_MESSAGE
  );
  ```

  前端预留 JS 调用入口，供 Java 侧在合适时机触发。

  ---
  五、原 Java 代码的复用情况

  ┌─────────────────────────────────┬─────────────┬─────────────────────────────────────────────────────┐
  │           原 Java 类            │  复用程度   │                        说明                         │
  ├─────────────────────────────────┼─────────────┼─────────────────────────────────────────────────────┤
  │ model/Item.java                 │ 🟡 大改     │ 扩展为 Asset.java，字段从 13 个增加到 25+           │
  ├─────────────────────────────────┼─────────────┼─────────────────────────────────────────────────────┤
  │ model/User.java                 │ 🟡 中改     │ 增加 nickname, avatarIndex, theme 字段              │
  ├─────────────────────────────────┼─────────────┼─────────────────────────────────────────────────────┤
  │ model/Achievement.java          │ 🟢 小改     │ 基本不变，成就数量从 18 扩到 36                     │
  ├─────────────────────────────────┼─────────────┼─────────────────────────────────────────────────────┤
  │ model/PurchaseBatch.java        │ 🟢 保留     │ 基本不变                                            │
  ├─────────────────────────────────┼─────────────┼─────────────────────────────────────────────────────┤
  │ model/Consumption.java          │ 🟡 改名     │ 变为 UsageLog，action 类型更丰富                    │
  ├─────────────────────────────────┼─────────────┼─────────────────────────────────────────────────────┤
  │ dao/ItemDao.java                │ 🔴 重写     │ .dat 文件 → SQL 数据库，完全重写                    │
  ├─────────────────────────────────┼─────────────┼─────────────────────────────────────────────────────┤
  │ dao/UserDao.java                │ 🔴 重写     │ 同上                                                │
  ├─────────────────────────────────┼─────────────┼─────────────────────────────────────────────────────┤
  │ dao/AchievementDao.java         │ 🔴 重写     │ 同上，成就定义从硬编码改为 SQL 初始化               │
  ├─────────────────────────────────┼─────────────┼─────────────────────────────────────────────────────┤
  │ service/LongTermService.java    │ 🟢 逻辑复用 │ 核心计算逻辑不变，去掉格式化输出（改为返回 JSON）   │
  ├─────────────────────────────────┼─────────────┼─────────────────────────────────────────────────────┤
  │ service/StockpileService.java   │ 🟢 逻辑复用 │ 均价计算、库存预警、比价逻辑全部保留                │
  ├─────────────────────────────────┼─────────────┼─────────────────────────────────────────────────────┤
  │ service/RecordService.java      │ 🟡 拆分     │ 收藏状态部分抽取到 CollectibleService               │
  ├─────────────────────────────────┼─────────────┼─────────────────────────────────────────────────────┤
  │ service/AchievementService.java │ 🟡 扩展     │ 检查逻辑扩展至 36 成就，新增订阅/次卡相关的检查条件 │
  ├─────────────────────────────────┼─────────────┼─────────────────────────────────────────────────────┤
  │ service/AdviceService.java      │ 🟡 扩展     │ 洞察类型从 6 种扩展到覆盖新资产类型                 │
  ├─────────────────────────────────┼─────────────┼─────────────────────────────────────────────────────┤
  │ service/UserService.java        │ 🟡 重写     │ 增加 JWT 签发、密码哈希                             │
  ├─────────────────────────────────┼─────────────┼─────────────────────────────────────────────────────┤
  │ util/DateUtil.java              │ 🟢 保留     │ 基本不变                                            │
  ├─────────────────────────────────┼─────────────┼─────────────────────────────────────────────────────┤
  │ util/Validator.java             │ 🟢 保留     │ 基本不变                                            │
  ├─────────────────────────────────┼─────────────┼─────────────────────────────────────────────────────┤
  │ util/Categories.java            │ 🟢 保留     │ 基本不变                                            │
  ├─────────────────────────────────┼─────────────┼─────────────────────────────────────────────────────┤
  │ view/* (全部 Swing)             │ 🔴 废弃     │ 被 HTML/CSS 前端完全替代                            │
  └─────────────────────────────────┴─────────────┴─────────────────────────────────────────────────────┘

  ---
  六、推荐的开发阶段

  Phase 1 ─ 后端基础设施 (Java)
  ├── 搭建 Maven 项目 + 引入依赖 (Javalin + SQLite + JWT + bcrypt)
  ├── 设计并创建数据库表
  ├── 实现 UserRepository + AuthController (注册/登录/JWT)
  ├── 实现 AssetRepository + AssetController (CRUD)
  └── 用 Postman/curl 验证所有 API

  Phase 2 ─ 后端业务逻辑 (Java)
  ├── 实现 LongTermService.checkIn() 打卡逻辑
  ├── 实现 StockpileService 补货/消耗/比价
  ├── 实现 SubscriptionService 续费倒计时
  ├── 实现 StoredCardService 核销/充值
  ├── 实现 AchievementService (36 成就)
  ├── 实现 InsightService 智能洞察
  └── 实现 DashboardController 大盘汇总

  Phase 3 ─ 前端重构 (JS)
  ├── 从 HTML 抽取 CSS 到独立文件
  ├── 搭建 JS 模块结构 (api/ state/ views/ components/)
  ├── 实现 api/client.js (fetch 封装 + JWT 拦截)
  ├── 逐个页面重构:
  │   ├── 登录/注册 → 接 API
  │   ├── 大盘 → 接 /api/dashboard
  │   ├── 物尽其用 → 接 /api/assets + 打卡 API
  │   ├── 会员中心 → 接 /api/assets + 核销/续费 API
  │   ├── 成就殿堂 → 接 /api/achievements
  │   └── 封存仓库 → 接 /api/assets?archived=1
  └── Toast 通知、错误处理、Loading 状态

  Phase 4 ─ 打磨
  ├── 数据导出/备份功能
  ├── 主题切换与用户偏好持久化
  ├── 安全加固 (CORS 配置、输入消毒、请求频率限制)
  └── 部署文档/打包

  ---
  七、关键设计决策

  ┌────────────────────────────┬──────────────────────────────────┬───────────────────────────────────────────────────────┐
  │           决策点           │              选择                │                       核心理由                        │
  ├────────────────────────────┼──────────────────────────────────┼───────────────────────────────────────────────────────┤
  │ 前后端通信                 │ JCEF JS 桥接 (JavaBackend)       │ 零网络开销，同步调用，无需 HTTP 协议栈                │
  ├────────────────────────────┼──────────────────────────────────┼───────────────────────────────────────────────────────┤
  │ 窗口框架                   │ Swing JFrame + JCEF (Chromium)    │ 完整 Chromium 内核 → color-mix/CSS Grid 等全兼容      │
  ├────────────────────────────┼──────────────────────────────────┼───────────────────────────────────────────────────────┤
  │ 数据库                     │ SQLite (项目本地目录)            │ 单文件、零服务、事务安全，桌面标配                    │
  ├────────────────────────────┼──────────────────────────────────┼───────────────────────────────────────────────────────┤
  │ 密码存储                   │ BCrypt 哈希                      │ 本地数据库也不能明文                                  │
  ├────────────────────────────┼──────────────────────────────────┼───────────────────────────────────────────────────────┤
  │ 前端框架                   │ Vue 3 (本地 .js 文件，非 CDN)    │ 响应式系统 + v-for 避免 innerHTML 拼接 + 离线可用     │
  ├────────────────────────────┼──────────────────────────────────┼───────────────────────────────────────────────────────┤
  │ 卡片布局                   │ CSS Grid auto-fill + minmax      │ 窗口拉大自动多列，拉小自动折行，无媒体查询断点        │
  ├────────────────────────────┼──────────────────────────────────┼───────────────────────────────────────────────────────┤
  │ 最小窗口                   │ Java 侧 setMinSize(900, 600)      │ 保底物理限制，防止布局崩溃                            │
  ├────────────────────────────┼──────────────────────────────────┼───────────────────────────────────────────────────────┤
  │ DPI 适配                   │ rem + Chromium 内核自动缩放       │ Windows 125%/150% 系统缩放自动生效                    │
  ├────────────────────────────┼──────────────────────────────────┼───────────────────────────────────────────────────────┤
  │ 静态资源                   │ 全部本地目录，相对路径引用       │ 断网环境 100% 正常运行                                │
  ├────────────────────────────┼──────────────────────────────────┼───────────────────────────────────────────────────────┤
  │ 成就/洞察计算              │ Java 后端计算，返回结果 JSON     │ 前端只渲染，不保存任何业务规则                        │
  ├────────────────────────────┼──────────────────────────────────┼───────────────────────────────────────────────────────┤
  │ 金融操作                   │ Java SQLite 事务保护             │ 扣减库存 + 写入流水 原子操作，杜绝不一致              │
  ├────────────────────────────┼──────────────────────────────────┼───────────────────────────────────────────────────────┤
  │ 用户会话                   │ JCEF 进程即会话 (无 token)       │ 本地桌面应用无需 JWT，关闭窗口即登出                  │
  ├────────────────────────────┼──────────────────────────────────┼───────────────────────────────────────────────────────┤
  │ 文案归属                   │ 前端 messageMap.js               │ 后端返回 message_key，前端查表，改文案不动后端        │
  ├────────────────────────────┼──────────────────────────────────┼───────────────────────────────────────────────────────┤
  │ 阈值归属                   │ 前端 computed                    │ 后端返回原始数据 (ratio)，前端自判 UI 状态            │
  └────────────────────────────┴──────────────────────────────────┴───────────────────────────────────────────────────────┘

  ---
  八、总结

  最终产物：**一个 Windows 原生可执行程序**（双击 JAR），启动后打开标准 Win32 窗口，内部是 JCEF Chromium 渲染的 "归藏" 页面。

  架构一句话：**Swing JFrame + JCEF Chromium 内核渲染 HTML/Vue/CSS + JS 桥接调用 Java 业务逻辑 + SQLite 本地数据库**。

  各层职责：
  - Java 负责：数据怎么算（日均成本、均价、成就）、数据怎么存（SQLite + 事务）、用户怎么验证（BCrypt）、窗口怎么管（最小尺寸、DPI）
  - Vue 3 负责：响应式数据驱动 UI、v-for 替代 innerHTML、CSS Grid 自适应布局
  - CSS 负责：喜茶手绘风视觉、深浅双主题、所有静态资源离线可用
  - 文案/阈值在前端维护（messageMap.js + computed），改展示无需碰 Java

  前后端修改范围概览：

  ┌──────────────────────────────────────────────────────────────────────┐
  │ 改什么              → 只需改哪端                                     │
  ├──────────────────────────────────────────────────────────────────────┤
  │ CSS 样式/布局/动画  → 前端 CSS                                       │
  │ 卡片文案措辞        → 前端 messageMap.js                             │
  │ 阈值数值            → 前端 computed 中的常量                         │
  │ 图标                → 前端模板                                       │
  │ Toast 消息文本      → 前端 Toast 调用处                              │
  │ 新增资产类型        → Java Model/Service + JS Bridge 方法 + 前端模板 │
  │ 新增数据字段        → DB Schema + Java + 前端                        │
  │ 成就规则变更        → Java AchievementService                        │
  │ 计算公式改变        → Java Service                                   │
  │ 认证规则            → Java UserService                               │
  │ 窗口大小限制        → Java AppConfig                                 │
  │ DPI/缩放            → 自动（Chromium 内核处理）                       │
  └──────────────────────────────────────────────────────────────────────┘

  离线打包清单（随 JAR 一起分发）：
  ├── vue.global.prod.js          ← Vue 3 运行时（~120KB）
  ├── fonts/fa-solid-900.woff2    ← FontAwesome 图标字体
  ├── fonts/MuYaoSuiXinTi.woff2   ← 手绘风字体
  └── 所有 CSS/JS 文件