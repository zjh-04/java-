# 「归藏」— 个人资产管理

「归藏」—— 此间安放 · 此身相伴 · 此心所系

## 1. 项目架构

```
┌─ Swing JFrame (桌面壳) ──────────────────────────────┐
│  ┌─ JCEF Browser (Chromium 内核) ──────────────────┐  │
│  │  Vue 3 前端 (src/main/resources/web/index.html)  │  │
│  │  ┌──────────┐ ┌──────────┐ ┌────────────────┐   │  │
│  │  │  5 页面   │ │ Reactive │ │  fetch /api/*  │   │  │
│  │  │ Views     │ │  State   │ │  JS Bridge     │   │  │
│  │  └──────────┘ └──────────┘ └───────┬────────┘   │  │
│  └────────────────────────────────────┼────────────┘  │
│                                       │                │
│  ┌─ 内嵌 HTTP Server (localhost:18080) ┼───────────┐  │
│  │  JavaBackend.java (路由分发)       │            │  │
│  │  ├ UserService / AssetService                   │  │
│  │  ├ StockpileService / StoredCardService         │  │
│  │  ├ LongTermService / CollectibleService          │  │
│  │  ├ SubscriptionService / AchievementService     │  │
│  │  └ InsightService                               │  │
│  │         ↓                                        │  │
│  │  Repository → SQLite (data/assets.db)           │  │
│  └─────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────┘
```

- **前端**：Vue 3 单页应用，无构建工具，直接用 `<script type="module">` + ES import map
- **后端**：Java 内嵌 HTTP 服务器 (`com.sun.net.httpserver`)，API 返回 JSON
- **数据库**：SQLite，单文件。IDE 开发时存 `./data/assets.db`；EXE 打包后存 `%APPDATA%\Guicang\data\assets.db`
- **桌面壳**：Swing + JCEF (Chromium Embedded Framework)，降级方案为系统默认浏览器

## 2. 快速开始

### 环境要求

- **JDK** 17+
- **Maven** 3.8+
- **推荐 IDE**：VS Code（已配 `.vscode/launch.json`）或 IntelliJ IDEA（已配 `.idea/`）

### IDE 中运行

1. 打开 `src/main/java/com/guicang/App.java`
2. 点击 `main` 方法上方的 `Run` 按钮（或按 F5）

### 命令行运行

```bash
# 编译
mvn clean package

# 浏览器模式（秒开，推荐日常开发用）
java -jar target/guicang-1.0.0.jar --browser

# 桌面窗口模式（JCEF 内嵌 Chromium，首次需下载 ~150MB 原生库）
java -jar target/guicang-1.0.0.jar
```

### 命令行参数

| 参数 | 说明 |
|------|------|
| `(无参数)` | 桌面窗口模式：Swing + JCEF 内嵌 Chromium |
| `--browser` | 浏览器模式：启动 HTTP 服务后自动用系统浏览器打开，右下角托盘图标常驻 |
| `--no-jcef` | 同 `--browser` |
| `--help` / `-h` | 显示帮助信息 |

### 打包为独立 EXE

```bash
# 1. 编译 fat JAR
mvn clean package

# 2. 裁剪最小 JRE（约 49MB，无需用户装 Java）
jlink --add-modules java.base,java.desktop,java.sql,java.naming,jdk.httpserver \
      --output target/jre-min --strip-debug --compress=2 --no-header-files --no-man-pages

# 3. 浏览器版（秒开，托盘图标，无控制台）
jpackage --name "guicang" --input target --main-jar guicang-1.0.0.jar \
         --main-class com.guicang.App --runtime-image target/jre-min \
         --arguments "--browser" --type app-image --dest dist

# 4. 桌面版（JCEF 独立窗口，带控制台查看下载进度）
jpackage --name "guicang-desktop" --input target --main-jar guicang-1.0.0.jar \
         --main-class com.guicang.App --runtime-image target/jre-min \
         --type app-image --dest dist --win-console
```

生成在 `dist/` 下，拷贝整个文件夹即可分发到其他 Windows 电脑（无需装 Java）。

| 版本 | 文件夹 | 体验 |
|------|--------|------|
| 浏览器版 | `dist/guicang/` | 系统浏览器打开，秒启动，托盘图标退出 |
| 桌面版 | `dist/guicang-desktop/` | 独立桌面窗口，首次需下载 JCEF 原生库 |

## 3. 项目结构

```
src/main/java/com/guicang/
├── App.java                   # 主入口
├── bridge/JavaBackend.java    # HTTP API 路由分发 → JSON 响应
├── config/
│   ├── AppConfig.java         # 窗口尺寸、端口等常量
│   └── DatabaseConfig.java    # SQLite 初始化与连接管理
├── model/
│   ├── Asset.java             # 统一资产实体（11 种 asset_type）
│   ├── User.java              # 用户实体
│   ├── Achievement.java       # 成就定义
│   ├── UserAchievement.java   # 用户成就进度
│   ├── PurchaseBatch.java     # 囤货采购批次
│   └── UsageLog.java          # 操作流水
├── repository/
│   ├── AssetRepository.java
│   ├── UserRepository.java
│   ├── AchievementRepository.java
│   ├── PurchaseBatchRepository.java
│   └── UsageLogRepository.java
├── service/
│   ├── UserService.java       # 认证与资料管理
│   ├── AssetService.java      # 资产 CRUD
│   ├── LongTermService.java   # 长期主义（打卡、日均成本）
│   ├── StockpileService.java  # 囤货（库存、补货、比价）
│   ├── StoredCardService.java # 次卡/储值卡（核销、充值）
│   ├── SubscriptionService.java # 数字订阅（续费倒计时）
│   ├── CollectibleService.java  # 收藏状态（三态循环）
│   ├── AchievementService.java  # 成就系统（36 成就）
│   └── InsightService.java    # 智能洞察
└── util/
    ├── DateUtil.java          # 日期计算
    ├── Validator.java         # 输入校验
    └── Categories.java        # 物品分类常量

src/main/resources/
├── db/schema.sql              # DDL + 36 条成就预置数据
└── web/
    ├── index.html             # Vue 3 单页应用
    ├── css/theme.css          # 喜茶手绘风样式（深色/浅色双模式）
    └── js/
        ├── app.js             # Vue 组件、API 调用、数据 enrich
        ├── config.js          # 前端常量
        ├── api/bridge.js      # fetch 封装
        └── lib/vue.esm-browser.prod.js  # Vue 3 运行时
```

## 4. 11 种资产类型

| 类型 | 常量 | 说明 |
|------|------|------|
| 细水长流·按次 | `LONG_TERM_PER_USE` | 按使用次数分摊成本 |
| 细水长流·按天 | `LONG_TERM_PER_DAY` | 按持有天数均摊成本 |
| 储备幸福 | `STOCKPILE` | 囤货库存管理、多批次均价、比价 |
| 收藏状态 | `COLLECTIBLE` | 纯陪伴追踪，三态循环（使用中/珍藏中/转手中） |
| 周期续费·按月 | `SUBSCRIPTION_MONTHLY` | 固定周期扣费订阅 |
| 周期续费·按季 | `SUBSCRIPTION_QUARTERLY` | |
| 周期续费·按年 | `SUBSCRIPTION_YEARLY` | |
| 按量计费 | `SUBSCRIPTION_METERED` | 弹性计费，余额管理 |
| 永久有效 | `SUBSCRIPTION_LIFETIME` | 买断制 |
| 储值次卡 | `STORED_TIME_CARD` | 按次数消耗 |
| 储值量卡 | `STORED_AMOUNT_CARD` | 按金额消费 |

## 5. API 路由

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/login` | 登录 |
| POST | `/api/register` | 注册 |
| GET | `/api/session` | 当前会话用户 |
| GET/POST | `/api/assets` | 列表 / 创建资产 |
| GET | `/api/assets/archived` | 已归档资产 |
| GET/PUT/DELETE | `/api/assets/{id}` | 单资产 CRUD |
| POST | `/api/assets/{id}/check-in` | 打卡 |
| POST | `/api/assets/{id}/consume` | 消耗 |
| POST | `/api/assets/{id}/restock` | 补货 |
| POST | `/api/assets/{id}/punch` | 次卡核销 |
| POST | `/api/assets/{id}/topup` | 充值 |
| POST | `/api/assets/{id}/spend` | 消费 |
| PUT | `/api/assets/{id}/status` | 切换收藏状态 |
| POST | `/api/assets/{id}/compare` | 比价 |
| GET | `/api/assets/{id}/history` | 操作历程 |
| POST | `/api/assets/{id}/archive` | 归档 |
| POST | `/api/assets/{id}/restore` | 恢复 |
| GET | `/api/dashboard` | 仪表盘数据 |
| GET | `/api/achievements` | 成就列表 |
| GET | `/api/insights` | 智能洞察 |
| GET/POST | `/api/config` | 读取/修改配置 |
| GET | `/api/export` | 导出备份 |

## 6. 参考文档

| 文件 | 说明 |
|------|------|
| `chonggou_prd.md` | 全栈重构架构设计文档：架构图、数据模型、API 路由、重构策略 |
| `DESIGN.md` | UI 设计规格：色彩体系、手绘风组件规范、卡片布局 |
| `ceshi_original.html` | 原始纯前端原型（mock 数据） |
| `ceshi_final.html` | 最终 UI 参考实现（理想效果参考） |

> UI 调整时先看 `ceshi_final.html` 的预期效果，再到 `index.html` 里实现。

---

*最后更新：2026-06-06*
