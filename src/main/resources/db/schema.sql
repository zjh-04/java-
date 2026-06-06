-- ============================================
-- 归藏 — SQLite 数据库初始化 DDL
-- ============================================

-- ==================== 用户 ====================
CREATE TABLE IF NOT EXISTS users (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    username        TEXT    NOT NULL UNIQUE,          -- 登录账号 (不可修改)
    password_hash   TEXT    NOT NULL,                 -- BCrypt 哈希
    nickname        TEXT    NOT NULL,                 -- 显示昵称 (可修改)
    email           TEXT,                             -- 可选邮箱
    avatar_index    INTEGER DEFAULT 0,               -- 头像图标索引
    theme           TEXT    DEFAULT 'dark',           -- 'dark' | 'light'
    created_at      TEXT    NOT NULL                  -- yyyy-MM-dd
);

-- ==================== 统一资产表 ====================
-- 11 种 asset_type，专属字段可 NULL
CREATE TABLE IF NOT EXISTS assets (
    id              TEXT    PRIMARY KEY,              -- UUID 短码 8 位
    user_id         INTEGER NOT NULL REFERENCES users(id),
    asset_type      TEXT    NOT NULL,               -- 见下方注释
    name            TEXT    NOT NULL,
    icon            TEXT,                           -- FontAwesome class, 如 fa-solid fa-shirt
    category        TEXT,                           -- 分类标签
    purchase_price  REAL,                           -- 购入价 / 首充金额
    purchase_date   TEXT,                           -- yyyy-MM-dd
    notes           TEXT,                           -- 备注

    -- 细水长流·按次 (LONG_TERM_PER_USE)
    usage_count     INTEGER DEFAULT 0,

    -- 细水长流·按天 (LONG_TERM_PER_DAY)
    expected_lifespan_years INTEGER,               -- 可选：预期使用年限

    -- 储备幸福 (STOCKPILE)
    current_stock   REAL,
    safety_stock    REAL,

    -- 收藏状态 (COLLECTIBLE) — 纯陪伴追踪，无序列号/保修
    collect_status  TEXT,                           -- '日常使用中'|'完美珍藏中'|'计划转手中'

    -- 周期续费 (SUBSCRIPTION_*)
    billing_cycle       TEXT,                       -- 'MONTHLY'|'QUARTERLY'|'YEARLY'
    monthly_cost        REAL,
    next_billing_date   TEXT,

    -- 按量计费 (SUBSCRIPTION_METERED)
    api_balance     REAL,
    total_charged   REAL,

    -- 储值次卡 (STORED_TIME_CARD)
    remaining_times INTEGER,
    total_times     INTEGER,

    -- 储值量卡 (STORED_AMOUNT_CARD) + 次卡共用
    total_topup     REAL,

    -- 储值量卡
    card_balance    REAL,
    total_spent     REAL,

    -- 通用状态
    is_archived     INTEGER DEFAULT 0,             -- 0=正常, 1=已归档(仓库)
    created_at      TEXT,
    updated_at      TEXT
);

CREATE INDEX IF NOT EXISTS idx_assets_user_type ON assets(user_id, asset_type);
CREATE INDEX IF NOT EXISTS idx_assets_user_archived ON assets(user_id, is_archived);

/*
asset_type 枚举（11 种）：
  LONG_TERM_PER_USE       — 细水长流·按次（始祖鸟打卡）
  LONG_TERM_PER_DAY       — 细水长流·按天（iPhone 日均）
  STOCKPILE               — 储备幸福（囤货库存）
  COLLECTIBLE             — 收藏状态（手镯/翡翠/相机）
  SUBSCRIPTION_MONTHLY    — 周期续费·按月（Netflix）
  SUBSCRIPTION_QUARTERLY  — 周期续费·按季（Adobe CC）
  SUBSCRIPTION_YEARLY     — 周期续费·按年（iCloud+）
  SUBSCRIPTION_METERED    — 按量计费（OpenAI API）
  SUBSCRIPTION_LIFETIME   — 永久有效（Infuse Pro）
  STORED_TIME_CARD        — 储值次卡（健身/SPA）
  STORED_AMOUNT_CARD      — 储值量卡（餐厅）
*/

-- ==================== 操作流水 ====================
CREATE TABLE IF NOT EXISTS usage_logs (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    asset_id    TEXT    NOT NULL REFERENCES assets(id),
    action_type TEXT    NOT NULL,  -- CHECK_IN|CONSUME|PUNCH|TOPUP|SPEND|RESTOCK|STATUS_CHANGE
    quantity    REAL,              -- 消耗数量 / 金额
    unit_price  REAL,              -- 操作时单价
    notes       TEXT,
    created_at  TEXT    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_usage_logs_asset ON usage_logs(asset_id, created_at DESC);

-- ==================== 囤货采购批次 ====================
CREATE TABLE IF NOT EXISTS purchase_batches (
    id          TEXT PRIMARY KEY,                   -- UUID
    asset_id    TEXT NOT NULL REFERENCES assets(id),
    quantity    INTEGER NOT NULL,
    total_price REAL    NOT NULL,
    batch_date  TEXT    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_batches_asset ON purchase_batches(asset_id);

-- ==================== 成就定义 ====================
CREATE TABLE IF NOT EXISTS achievement_defs (
    id          TEXT PRIMARY KEY,                   -- 'a01' ~ 'a36'
    name        TEXT NOT NULL,
    description TEXT,
    icon        TEXT,                               -- FontAwesome class
    category    TEXT NOT NULL,
    goal_type   TEXT NOT NULL,
    goal_value  REAL NOT NULL,
    level       INTEGER DEFAULT 1                   -- 1=铜 2=银 3=金
);

-- ==================== 用户成就进度 ====================
CREATE TABLE IF NOT EXISTS user_achievements (
    user_id         INTEGER NOT NULL REFERENCES users(id),
    achievement_id  TEXT    NOT NULL REFERENCES achievement_defs(id),
    progress        REAL    DEFAULT 0,
    is_completed    INTEGER DEFAULT 0,
    completed_date  TEXT,
    PRIMARY KEY (user_id, achievement_id)
);

-- ==================== 36 条成就预置 ====================
INSERT OR IGNORE INTO achievement_defs (id, name, description, icon, category, goal_type, goal_value, level) VALUES
-- 物品收集（4）
('a01','初识资产','拥有第 1 件物品','fa-trophy','物品收集','total_items',1,1),
('a02','小有所成','累计拥有 5 件物品','fa-medal','物品收集','total_items',5,1),
('a03','琳琅满目','累计拥有 10 件物品','fa-award','物品收集','total_items',10,2),
('a04','博物收藏家','累计拥有 20 件物品','fa-crown','物品收集','total_items',20,3),
-- 细水长流（6）
('a05','初次相遇','完成第 1 次陪伴打卡','fa-trophy','细水长流','checkin_count',1,1),
('a06','常伴左右','累计陪伴打卡 10 次','fa-medal','细水长流','checkin_count',10,1),
('a07','习惯成自然','累计陪伴打卡 30 次','fa-award','细水长流','checkin_count',30,2),
('a08','百次相伴','累计陪伴打卡 100 次','fa-crown','细水长流','checkin_count',100,3),
('a09','物尽其用','有物品单次成本降至原价 10%','fa-trophy','细水长流','cost_ratio_10',1,1),
('a10','日积月累','有物品按天陪伴超过 365 天','fa-medal','细水长流','days_held',365,2),
-- 储备幸福（7）
('a11','未雨绸缪','拥有第 1 件囤货物品','fa-trophy','储备幸福','stockpile_count',1,1),
('a12','满载而归','完成 1 次补货入库','fa-medal','储备幸福','restock_count',1,1),
('a13','库存告急','某件囤货库存低于安全线','fa-award','储备幸福','stock_low',1,2),
('a14','弹尽粮绝','某件囤货物品全部消耗完毕','fa-trophy','储备幸福','stock_empty',1,1),
('a15','仓廪丰实','拥有 3 件以上囤货物品','fa-crown','储备幸福','stockpile_count',3,3),
('a16','温柔相遇','触发 1 次史低价购入','fa-trophy','储备幸福','low_price',1,1),
('a17','砍价高手','触发 3 次史低价购入','fa-medal','储备幸福','low_price',3,1),
-- 收藏纪念（4）
('a18','珍视之物','拥有第 1 件收藏状态物品','fa-trophy','收藏纪念','collectible_count',1,1),
('a19','岁月珍藏','收藏物品陪伴超过 30 天','fa-medal','收藏纪念','days_held',30,1),
('a20','传家之宝','收藏物品陪伴超过 365 天','fa-award','收藏纪念','days_held',365,2),
('a21','物语收藏家','拥有 3 件以上收藏状态物品','fa-crown','收藏纪念','collectible_count',3,3),
-- 数字订阅（6）
('a22','初试订阅','绑定第 1 个数字化订阅','fa-trophy','数字订阅','subscription_count',1,1),
('a23','数字游民','拥有 3 个以上订阅服务','fa-medal','数字订阅','subscription_count',3,1),
('a24','精打细算','暂停/归档 1 个不常用订阅','fa-award','数字订阅','archived_sub',1,2),
('a25','终身相伴','拥有 1 个永久有效订阅','fa-trophy','数字订阅','lifetime_sub',1,1),
('a26','储值达人','拥有 2 个以上储值卡','fa-medal','数字订阅','stored_card_count',2,1),
('a27','订阅掌控者','同时有续费+按量+永久+储值','fa-crown','数字订阅','all_types',4,3),
-- 资产总览（5）
('a28','万元户','资产总价值超过 ¥10,000','fa-trophy','资产总览','total_value',10000,1),
('a29','资产丰盈','资产总价值超过 ¥50,000','fa-medal','资产总览','total_value',50000,2),
('a30','多元配置','同时有实体+数字+收藏','fa-award','资产总览','diversify',3,2),
('a31','仓库管理员','仓库中有过 1 件归档物品','fa-trophy','资产总览','archived',1,1),
('a32','全面掌控','解锁全部 5 种资产类型','fa-crown','资产总览','type_count',5,3),
-- 里程碑（4）
('a33','日省一文','有 1 件物品单次成本低于 ¥1','fa-trophy','里程碑','cost_under_1',1,1),
('a34','日省百文','有 3 件物品单次成本低于 ¥10','fa-medal','里程碑','cost_under_10',3,1),
('a35','高价也从容','购入价格高于历史最高价','fa-award','里程碑','high_price',1,2),
('a36','归藏大师','解锁全部 36 个成就中的 25 个','fa-crown','里程碑','unlocked',25,3);
