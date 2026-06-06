/** 前端常量定义 — AOCI config.js[XC7T] */
export const API_BASE = '/api';
export const API_TIMEOUT = 10000;

/** 11 种资产类型枚举 */
export const ASSET_TYPES = [
    'LONG_TERM_PER_USE', 'LONG_TERM_PER_DAY',
    'STOCKPILE', 'COLLECTIBLE',
    'SUBSCRIPTION_MONTHLY', 'SUBSCRIPTION_QUARTERLY', 'SUBSCRIPTION_YEARLY',
    'SUBSCRIPTION_METERED', 'SUBSCRIPTION_LIFETIME',
    'STORED_TIME_CARD', 'STORED_AMOUNT_CARD'
];

export const ASSET_TYPE_MAP = {
    LONG_TERM_PER_USE:    { label: '细水长流·按次', cssClass: 'card-longterm', category: 'longterm' },
    LONG_TERM_PER_DAY:    { label: '细水长流·按天', cssClass: 'card-longterm', category: 'longterm' },
    STOCKPILE:            { label: '储备幸福',       cssClass: 'card-stockpile', category: 'stockpile' },
    COLLECTIBLE:          { label: '收藏状态',       cssClass: 'card-collectible', category: 'record' },
    SUBSCRIPTION_MONTHLY: { label: '周期续费·按月', cssClass: 'card-subscription', category: 'subscription' },
    SUBSCRIPTION_QUARTERLY:{ label: '周期续费·按季',cssClass: 'card-subscription', category: 'subscription' },
    SUBSCRIPTION_YEARLY:  { label: '周期续费·按年', cssClass: 'card-subscription', category: 'subscription' },
    SUBSCRIPTION_METERED: { label: '按量计费',       cssClass: 'card-metered', category: 'subscription' },
    SUBSCRIPTION_LIFETIME:{ label: '永久有效',       cssClass: 'card-lifetime', category: 'subscription' },
    STORED_TIME_CARD:     { label: '储值次卡',       cssClass: 'card-stored', category: 'stored' },
    STORED_AMOUNT_CARD:   { label: '储值量卡',       cssClass: 'card-stored', category: 'stored' }
};

export const COLLECT_STATUSES = ['日常使用中', '完美珍藏中', '计划转手中'];

/** 7 种成就分类 */
export const ACHIEVEMENT_CATEGORIES = [
    '物品收集', '细水长流', '储备幸福', '收藏纪念', '数字订阅', '资产总览', '里程碑'
];

/** 消息键值枚举（对应 messageMap.js） */
export const MESSAGE_KEYS = {
    coat: ['start', 'first', 'warm', 'good', 'great', 'perfect'],
    stock: ['full', 'low', 'empty']
};

/** 阈值常量 */
export const THRESHOLD_VALUES = {
    COAT_PERFECT_RATIO: 0.10,   // ≤10% → 物尽其用
    COAT_GREAT_RATIO:  0.30,
    COAT_GOOD_RATIO:   0.50,
    COAT_WARM_RATIO:   0.70,
    STOCK_LOW_RATIO:   1.0,     // 库存 ≤ 安全线 → 余量轻盈
    SUB_CRITICAL_DAYS: 3,       // ≤3天 → 续费预警
};

/** FontAwesome 图标映射（默认图标按资产类型） */
export const FONTAWESOME_ICON_MAP = {
    LONG_TERM_PER_USE:    'fa-solid fa-star',
    LONG_TERM_PER_DAY:    'fa-solid fa-star',
    STOCKPILE:            'fa-solid fa-box',
    COLLECTIBLE:          'fa-solid fa-gem',
    SUBSCRIPTION_MONTHLY: 'fa-solid fa-rotate',
    SUBSCRIPTION_QUARTERLY:'fa-solid fa-rotate',
    SUBSCRIPTION_YEARLY:  'fa-solid fa-rotate',
    SUBSCRIPTION_METERED: 'fa-solid fa-bolt',
    SUBSCRIPTION_LIFETIME:'fa-solid fa-infinity',
    STORED_TIME_CARD:     'fa-solid fa-ticket',
    STORED_AMOUNT_CARD:   'fa-solid fa-wallet'
};
