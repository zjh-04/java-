# 「归藏」— 个人资产管理手账

> **v1.0 · 此间安放 · 此身相伴 · 此心所系**

---

## 一、程序功能概述

「归藏」是一款**桌面端个人资产管理工具**，帮助用户以有温度的方式记录、追踪、盘点自己所拥有的物品与数字订阅。核心理念是"万物归而藏之"——通过陪伴打卡、成本均摊、库存预警等机制，让用户感知每一件物品的陪伴价值，践行克制消费与断舍离的生活美学。

### 核心业务场景

| 场景 | 描述 |
|------|------|
| **长期物品追踪** | 记录衣服/数码产品等，按使用次数或持有天数计算单次/日均成本，打卡陪伴 |
| **囤货库存管理** | 管理日用品等消耗品库存，多批次补货、均价计算、价格区间比对 |
| **收藏品记录** | 纯陪伴追踪（不计成本），支持"日常使用中/完美珍藏中/计划转手中"三态切换 |
| **数字订阅管理** | 管理 Netflix/iCloud 等周期续费订阅，自动续费倒计时，剩余天数预警 |
| **储值卡管理** | 健身次卡/餐厅储值卡等，按次核销或按金额消费，余额预警 |
| **资产总览** | 月度固定流速、日均均摊、按量计费注资总额、总资产价值一目了然 |
| **成就系统** | 36 项成就（7 大类），从"初识资产"到"归藏大师"，激励持续记录 |
| **智能洞察** | 今日物语：自动生成陪伴天数提醒、库存预警、续费倒计时等个性化洞察 |

---

## 二、产品特色

- **手绘美学风格**：手绘风设计语言——不对称圆角、炭笔边框、硬纸叠阴影、蜡笔涂抹底色，视觉友好，深色"柔墨砚"模式 + 浅色"燕麦奶白"模式
- **便捷的操作模式**：多种快捷按钮设计/双击名称即可编辑/直观可视化，使记录更省力
- **阈值驱动的 UI 变化**：不同使用次数/成本比例对应不同的温暖文案、颜色、标签、进度条变化
- **成就激励体系**：36 项成就分 7 大类

---

## 三、程序模块结构

### 整体架构

```
┌─ Swing JFrame (桌面壳, 1040×700) ──────────────────────────┐
│  ┌─ JCEF Browser (Chromium 内核) ────────────────────────┐  │
│  │  Vue 3 前端 (index.html + app.js + theme.css)         │  │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────────────────┐   │  │
│  │  │  5 页面   │ │ Reactive │ │  bridge.js           │   │  │
│  │  │  (v-show) │ │  State   │ │  (fetch /api/*)      │   │  │
│  │  └──────────┘ └──────────┘ └──────────┬───────────┘   │  │
│  └───────────────────────────────────────┼───────────────┘  │
│                                          │                   │
│  ┌─ JDK HttpServer (localhost:18080) ────┼───────────────┐  │
│  │  App.ApiHandler (路由分发)            │                │  │
│  │  ├ JavaBackend.java (30+ API 方法)                   │  │
│  │  ├ Service Layer (8 个服务)                          │  │
│  │  │   UserService / AssetService / LongTermService     │  │
│  │  │   StockpileService / CollectibleService            │  │
│  │  │   SubscriptionService / StoredCardService          │  │
│  │  │   AchievementService / InsightService              │  │
│  │  └ Repository Layer (5 个仓库, JDBC)                  │  │
│  │      UserRepo / AssetRepo / UsageLogRepo              │  │
│  │      PurchaseBatchRepo / AchievementRepo              │  │
│  └──────────────────────────────────────────────────────┘  │
│                          ↓                                  │
│  SQLite (data/assets.db, 6 张表)                           │
└────────────────────────────────────────────────────────────┘
```

### Java 后端模块（按职责分层）

```
src/main/java/com/guicang/
├── App.java                      ← 主入口：启动 SQLite → HTTP Server → JCEF 窗口
│                                   内嵌 ApiHandler + StaticFileHandler
├── config/                       ← 配置层
│   ├── AppConfig.java            ← 窗口尺寸、HTTP 端口
│   └── DatabaseConfig.java       ← SQLite 连接、DDL 执行、数据目录解析
├── bridge/                       ← API 路由层
│   └── JavaBackend.java          ← 30+ HTTP API 方法实现
├── service/                      ← 业务逻辑层（8 个服务）
│   ├── UserService.java          ← 注册/登录/密码修改/资料更新
│   ├── AssetService.java         ← 统一 CRUD + 增量更新
│   ├── LongTermService.java      ← 打卡、成本计算、5 档状态判定
│   ├── StockpileService.java     ← 库存/批次/均价/比价（事务保护）
│   ├── CollectibleService.java   ← 收藏三态循环
│   ├── SubscriptionService.java  ← 自动续费（synchronized 防并发）
│   ├── StoredCardService.java    ← 次卡/量卡/按量（事务保护）
│   ├── AchievementService.java   ← 36 成就全量检查
│   └── InsightService.java       ← 6 类智能洞察生成
├── repository/                   ← 数据访问层（5 个仓库，原生 JDBC）
│   ├── UserRepository.java       ← 用户 CRUD + BCrypt 验证
│   ├── AssetRepository.java      ← 11 种类型统一 SQL 构建
│   ├── UsageLogRepository.java   ← 操作流水
│   ├── PurchaseBatchRepository.java ← 采购批次
│   └── AchievementRepository.java   ← 成就进度 upsert
├── model/                        ← 数据实体（6 个类）
│   ├── User.java                 ← 用户（含 BCrypt 密码哈希）
│   ├── Asset.java                ← 统一资产（25+ 持久字段 + 5 派生字段）
│   ├── UsageLog.java             ← 操作流水
│   ├── PurchaseBatch.java        ← 采购批次
│   ├── Achievement.java          ← 成就定义
│   └── UserAchievement.java      ← 用户成就进度（含 is_notified）
└── util/                         ← 工具类
    ├── DateUtil.java              ← today() / daysBetween() / addDays()
    ├── Validator.java             ← 用户名/密码格式校验
    └── Categories.java            ← 物品分类常量
```

### 前端模块（Vue 3 单文件应用）

```
src/main/resources/web/
├── index.html                    ← 全部模板 + Vue 指令（~300 行）
├── css/theme.css                 ← 全部样式：变量/双主题/组件/手绘覆写（~1260 行）
└── js/
    ├── app.js                    ← Vue app 逻辑：setup() / enrich / 操作 / 弹窗（~470 行）
    ├── api/bridge.js             ← fetch 封装层，30 个 API 导出函数
    ├── config.js                 ← 常量定义：11 种 AssetType 枚举、阈值、图标映射
    └── lib/vue.esm-browser.prod.js ← Vue 3 ES Module 运行时（~120KB）
```

### 数据库表（6 张表）

| 表名 | 说明 |
|------|------|
| `users` | 用户：username, password_hash (BCrypt), nickname, email, avatar_index, theme |
| `assets` | 统一资产表：11 种 asset_type 共用，专属字段可 NULL |
| `usage_logs` | 操作流水：CHECK_IN / CONSUME / RESTOCK / PUNCH / TOPUP / SPEND 等 |
| `purchase_batches` | 囤货采购批次：quantity, total_price, batch_date |
| `achievement_defs` | 成就定义（36 条预置）：a01~a36, name, icon, category, goal_value, level |
| `user_achievements` | 用户成就进度：progress, is_completed, completed_date, is_notified |

---

## 四、所用技术与依赖

### 技术栈

| 层面 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 语言 | Java | 17+ | 后端业务逻辑 |
| 前端框架 | Vue 3 (ES Module) | 3.x | 响应式 UI，无构建工具 |
| 桌面壳 | Swing + JCEF | — | JFrame 窗口 + Chromium 内核渲染 |
| 内嵌浏览器 | JCEF (jcefmaven) | 146.0.10 | Chromium Embedded Framework for Java |
| HTTP 服务器 | `com.sun.net.httpserver` | JDK 内置 | 内嵌 HTTP，端口 18080 |
| 数据库 | SQLite (sqlite-jdbc) | 3.53.2.0 | 本地单文件数据库 |
| JSON | Gson | 2.11.0 | Java ↔ JSON 序列化 |
| 密码 | jBCrypt | 0.4 | BCrypt 密码哈希 |
| 构建 | Maven + maven-shade-plugin | 3.6.0 | 依赖管理 + fat-jar 打包 |
| CSS 图标 | FontAwesome 6.5.1 | CDN | 全部图标（约 2000+ 图标） |

### 第三方依赖及来源

| 依赖 | 来源 | 许可证 | 用途 |
|------|------|--------|------|
| [jcefmaven](https://github.com/jcefmaven/jcefmaven) | https://github.com/jcefmaven/jcefmaven | MIT | JCEF Maven 集成，自动管理 Chromium 原生库 |
| [sqlite-jdbc](https://github.com/xerial/sqlite-jdbc) | https://github.com/xerial/sqlite-jdbc | Apache 2.0 | SQLite JDBC 驱动 |
| [gson](https://github.com/google/gson) | https://github.com/google/gson | Apache 2.0 | JSON 序列化/反序列化 |
| [jbcrypt](https://github.com/jeremyh/jBCrypt) | https://github.com/jeremyh/jBCrypt | ISC | BCrypt 密码哈希 |
| [Vue 3](https://vuejs.org/) | https://vuejs.org/ | MIT | 前端响应式框架 |
| [FontAwesome 6](https://fontawesome.com/) | https://fontawesome.com/ | CC BY 4.0 (Free) | 图标字体，CDN 加载 |

---

## 五、参考文档

| 文件 | 说明 |
|------|------|
| [`chonggou_prd.md`](./chonggou_prd.md) | 全栈重构架构设计文档：完整架构图、数据模型、API 路由表、前后端详细设计 |
| [`DESIGN.md`](./DESIGN.md) | UI 设计规格说明书：色彩体系、全局尺寸布局、11 种卡片精确设计、手绘风格规范 |
| `ceshi_original.html` | 原始纯前端原型（mock 数据） |
| `ceshi_final.html` | 最终 UI 参考实现（理想效果参考） |

---

## 六、未来方向
- 数据导出
- 自定义头像
- 接入常用消费软件自动记录
- 接入AI智能管理
- 转向安卓端，手机拍照录入

---

## 七、AI 使用声明
- 本项目由人工设计所有产品需求、UI呈现和互动体验，调整ai生成的前端效果，检查ai代码的后端问题并进行代码重构。
- 本项目中使用ai进行了基础代码框架搭建、部分文案/文档书写、调试bug。
---

## 八、快速开始

### 环境要求

- **JDK** 17+
- **Maven** 3.8+

### IDE 中运行

1. 打开 `src/main/java/com/guicang/App.java`
2. 点击 `main` 方法上方的 `Run` 按钮（或按 F5）

### 命令行运行

```bash
# 编译
mvn clean package

# 浏览器模式（秒启动，日常开发推荐）
java -jar target/guicang-1.0.0.jar --browser

# 桌面窗口模式（JCEF 内嵌 Chromium，首次需下载 ~150MB 原生库）
java -jar target/guicang-1.0.0.jar
```

### 打包为独立 EXE

```bash
# 1. 编译 fat JAR
mvn clean package

# 2. 裁剪最小 JRE（约 49MB）
jlink --add-modules java.base,java.desktop,java.sql,java.naming,jdk.httpserver \
      --output target/jre-min --strip-debug --compress=2 --no-header-files --no-man-pages

# 3. 浏览器版（托盘图标，秒开）
jpackage --name "guicang" --input target --main-jar guicang-1.0.0.jar \
         --main-class com.guicang.App --runtime-image target/jre-min \
         --arguments "--browser" --type app-image --dest dist

# 4. 桌面版（JCEF 独立窗口）
jpackage --name "guicang-desktop" --input target --main-jar guicang-1.0.0.jar \
         --main-class com.guicang.App --runtime-image target/jre-min \
         --type app-image --dest dist --win-console
```

---

*最后更新：2026-06-08*
