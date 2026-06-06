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
│  ┌─ 内嵌 HTTP Server (localhost:8765) ┼────────────┐  │
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
- **数据库**：SQLite，单文件 `data/assets.db`
- **桌面壳**：Swing + JCEF (Chromium Embedded Framework)，降级方案为系统默认浏览器

## 2. 依赖 & 推荐 IDE

- **JDK** 17+
- **Maven** 3.8+（`pom.xml` 管理依赖，IDEA自带）
- **主要依赖**：`gson` (JSON)、`jbcrypt` (密码哈希)、`jcefmaven` (Chromium 嵌入)、`sqlite-jdbc`
- **推荐 IDE**：IntelliJ IDEA（项目已是 `.idea/` 配置好的工程），直接 Open → 选 pom.xml 导入

## 3. 运行入口

| 入口 | 文件路径 | 说明 |
|------|----------|------|
| 后端主入口 | `src/main/java/com/guicang/App.java` → 运行 `com.guicang.App` 的 `main()` | **推荐**。启动 SQLite → HTTP Server → JCEF 窗口 → 加载前端 |
| 旧版入口 | `src/Main.java` → 运行 `Main` 的 `main()` | 纯 Swing 版，不再维护 |

前端代码在 `src/main/resources/web/`：

| 文件 | 说明 |
|------|------|
| `index.html` | Vue 3 单页应用（实际运行的前端） |
| `js/app.js` | 所有 Vue 组件逻辑、API 调用、数据 enricher |
| `js/api/bridge.js` | fetch 封装，对 `App.java` 内嵌 HTTP API 发请求 |
| `css/theme.css` | 喜茶手绘风样式（深色/浅色双模式） |

启动方式：IDE 中运行 `App.main()`。

## 4. 参考文档 & HTML 文件说明

| 文件 | 作用                                                                    |
|------|-----------------------------------------------------------------------|
| `chonggou_prd.md` | 全栈重构架构设计文档（仅参考，实际实现有所不同）：架构图、数据模型（11 种 asset_type）、API 路由表、重构策略       |
| `DESIGN.md` | UI 设计规格说明书（仅参考，实际实现有所不同）：色彩体系（深/浅双模式 CSS 变量）、手绘风组件规范、卡片布局                      |
| `ceshi_original.html` | 原始纯前端原型（无后端，mock 数据），用于快速验证 UI 交互和布局                                  |
| `ceshi_final.html` | 最终 UI 参考实现（无后端，mock 数据），**理想 UI 效果参考这个文件**：卡片样式、比价条文案、封存仓库卡片、量杯 SVG 等 |
| `src/main/resources/web/index.html` | **实际运行的 Vue 3 前端**，对接后端 API，功能从 mock 变为真实数据                           |

> UI 调整时先看 `ceshi_final.html` 想要的效果，再到 `index.html` 里实现。

## 5. TODO — 需要测试和关注
0. 旧的冗余代码清理干净（已删一部分）
1. 前后端连通性
2. 时间逻辑
   - 长期主义卡片：陪伴天数是否每天自动递增、日均成本是否下降
   - 订阅卡片：剩余续费天数是否随日期变化、临近扣费是否变红
   - 首页仪表盘：月度流速、日均均摊、总量统计是否正确

3. 安全性 & 异常处理
   - 密码存储、明文不落盘
   - 异常抛出，边界处理

4. 前端交互或显示明显有问题的地方

5. 可选：展示文案措辞、ui不好看的细节修改等

---

*最后更新：2026-06-06*
