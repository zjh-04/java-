import { createApp, ref, reactive, computed, onMounted, nextTick } from 'vue';
import * as api from './api/bridge.js';

// ==================== 响应式数据 ====================
const user = ref(null);
const state = reactive({
    assets: [],
    archived: [],
    achievements: [],
    dashboard: {},
    insights: []
});
const assets = computed(() => state.assets);
const archived = computed(() => state.archived);
const achievements = computed(() => state.achievements);
const dashboard = computed(() => state.dashboard);
const insights = computed(() => state.insights);
const historyAssetId = ref('');
const historyLogs = ref([]);

// UI 状态
const phase = ref('welcome');
const view = ref('dashboard');
const drawerOpen = ref(false);
const authMode = ref('login');
const authErr = ref('');
const auth_account = ref(''); const auth_password = ref('');
const auth_username = ref(''); const auth_nickname = ref(''); const auth_confirm = ref('');
const addDrawerOpen = ref(false);
const addMode = ref('longterm');
const historyOpen = ref(false);
const editAsset = ref(null);

// 弹窗
const confirmType = ref('');
const confirmTarget = ref(null);
const confirmOpen = ref(false);
const formModalOpen = ref(false);
const formModalMode = ref('');
const formModalTitle = ref('');
const formModalAsset = ref(null);
const toastList = ref([]);

// ==================== 工具函数 ====================
const toastIcons = { success:'fa-circle-check', warning:'fa-triangle-exclamation', danger:'fa-circle-xmark', info:'fa-circle-info' };
const avatarIcons = ['fa-solid fa-user-shield','fa-solid fa-cat','fa-solid fa-fish','fa-solid fa-feather','fa-solid fa-crown','fa-solid fa-robot','fa-solid fa-ghost','fa-solid fa-dragon'];
const avatarIdx = ref(0);
const currentAvatar = ref(avatarIcons[0]);
const showAvatarPicker = ref(false);
const showNicknameModal = ref(false); const editNickname = ref('');
const showPasswordModal = ref(false); const oldPw = ref(''); const newPw = ref(''); const confirmPw = ref('');
function selectAvatar(idx) { avatarIdx.value = idx; currentAvatar.value = avatarIcons[idx]; showAvatarPicker.value = false; }
function saveNickname() { if (editNickname.value.trim()) { user.value.nickname = editNickname.value.trim(); showNicknameModal.value = false; showToast('昵称已更新', '', 'success'); } }
async function changePassword() {
    if (!oldPw.value || !newPw.value) { showToast('请填写完整', '', 'warning'); return; }
    if (newPw.value !== confirmPw.value) { showToast('两次密码不一致', '', 'warning'); return; }
    try {
        const r = await api.setConfig('password', JSON.stringify({ old: oldPw.value, new: newPw.value }));
        if (r.success) { showPasswordModal.value = false; showToast('密码已更新', '', 'success'); }
        else showToast(r.message || '修改失败', '', 'warning');
    } catch(e) { showToast('修改失败', '', 'warning'); }
}
function showToast(title, msg, type='info') {
    toastList.value.push({ title, msg, type, icon: toastIcons[type] || toastIcons.info });
    setTimeout(() => toastList.value.shift(), 3500);
}

// ==================== 核心：资产数据丰富化 ====================
function enrichAsset(a) {
    const t = a.assetType; const price = a.purchasePrice||0; const days = a.daysHeld||0; const uc = a.usageCount||0;
    // === 实体物品标签 ===
    if (t==='LONG_TERM_PER_USE') {
        a._cost = uc>0?(price/uc).toFixed(2):price.toFixed(2); a._ratio = uc>0?(price/uc)/price:1;
        if (uc===0) a._coatMsg=COAT_MSGS.start; else if (uc===1) a._coatMsg=COAT_MSGS.first;
        else if (a._ratio<=0.10) a._coatMsg=COAT_MSGS.perfect; else if (a._ratio<=0.30) a._coatMsg=COAT_MSGS.great;
        else if (a._ratio<=0.50) a._coatMsg=COAT_MSGS.good; else if (a._ratio<=0.70) a._coatMsg=COAT_MSGS.warm; else a._coatMsg=COAT_MSGS.first;
        a._tagClass = a._ratio<=0.10?'success':''; a._tagIcon = a._ratio<=0.10?'fa-solid fa-leaf':'fa-solid fa-star'; a._tagText = a._ratio<=0.10?'物尽其用':'细水长流';
        a._tagStyle = a._ratio<=0.10?'':'background:rgba(229,186,143,0.1);color:var(--primary);';
    } else if (t==='LONG_TERM_PER_DAY') {
        a._tagIcon='fa-solid fa-clock'; a._tagText='按天陪伴'; a._tagStyle='background:rgba(85,135,111,0.08);color:var(--accent-green);'; a._tagClass=''; a.dailyCost = a.dailyCost || price / Math.max(days, 1);
    } else if (t==='COLLECTIBLE') {
        const s=a.collectStatus||'日常使用中'; a._tagIcon=COLLECT_ICONS[s]||'fa-solid fa-check-circle'; a._tagText=s; a._tagStyle=`background:${COLLECT_COLORS[s]?.bg||'rgba(212,199,176,0.06)'};color:${COLLECT_COLORS[s]?.c||'var(--text-main)'};cursor:pointer;`; a._tagClass='';
    } else if (t==='STOCKPILE') {
        const s=a.currentStock||0, sf=a.safetyStock||0;
        a._tagClass=s<=0?'danger':s<=sf?'warning':'success';
        a._tagIcon=s<=0?'fa-solid fa-circle-xmark':s<=sf?'fa-solid fa-hourglass-half':'fa-solid fa-circle-check';
        a._tagText=s<=0?'库存耗尽':s<=sf?'余量轻盈':'状态充盈'; a._tagStyle='';
        a._avgPrice = a.dailyCost||(a.purchasePrice||0);
        a._priceMin = a.costRatio||(a.purchasePrice*0.8);
        a._priceMax = parseFloat(a.statusTagVariant)||(a.purchasePrice*1.2);
        a._stockUnit = (a.notes||'').match(/单位:(.+?);/)?.[1]||'';
        a._displayNotes = (a.notes||'').replace(/单位:.+?;\s*/,'');
    }
    // === 数字会员 / 储值卡 ===
    const isSub = t&&t.startsWith('SUBSCRIPTION_');
    if (isSub) {
        if (t==='SUBSCRIPTION_METERED') { a._priceHtml=`余量: <span>${(a.apiBalance||0).toFixed(2)}</span>`; a._statusHtml='<i class="fa-solid fa-circle-info" style="color:var(--muted);opacity:0.6;"></i> 按量计费提示：此服务无固定截止日，请留意消耗与额度监控。'; a._statusColor='var(--muted)'; a._showTopup=true; a._showAction=true; a._pct=Math.min(100, Math.max(5, ((a.apiBalance||0) / Math.max(a.totalCharged||1, 1)) * 100)); a._critical=false; }
        else if (t==='SUBSCRIPTION_LIFETIME') { a._priceHtml=`${price.toFixed(2)}<span style="font-size:12px;color:var(--muted);font-weight:normal;"> 买断</span>`; a._statusHtml=`<i class="fa-solid fa-infinity"></i> 永久有效 · 已陪伴 <span style="font-weight:600;">${days}</span> 天`; a._statusColor='var(--accent-green)'; a._pct=100; a._critical=false; a._showTopup=false; a._showAction=false; }
        else { const cycle=a.billingCycle==='YEARLY'?'年':a.billingCycle==='QUARTERLY'?'季':'月'; const cd=a.billingCycle==='YEARLY'?365:a.billingCycle==='QUARTERLY'?90:30; a._priceHtml=`${(a.monthlyCost||0).toFixed(2)}<span style="font-size:12px;color:var(--muted);font-weight:normal;"> / ${cycle}</span>`; a._statusHtml=days<=3?`<i class="fa-solid fa-hourglass-half"></i> ${days} 天后自动扣费`:`距离续费还有 ${days} 天`; a._statusColor=days<=3?'var(--warning)':'var(--muted)'; a._critical=days<=3; a._pct=Math.min(100,Math.max(5,(days/cd)*100)); a._showTopup=false; a._showAction=false; }
    } else if (t==='STORED_TIME_CARD') { a._priceHtml=`<span>${a.remainingTimes||0}</span><span style="font-size:12px;color:var(--muted);font-weight:normal;"> 次剩余</span>`; const cumPunched = (a.totalSpent!=null)?a.totalSpent:Math.max(0,(a.totalTimes||0)-(a.remainingTimes||0)); const cumPurchased = a.cumulativePurchased!=null?a.cumulativePurchased:((a.remainingTimes||0) + cumPunched); const unitCost = ((a.totalTopup||0)/Math.max(cumPurchased,1)).toFixed(2); a._statusHtml=`累计购入 ${cumPurchased} 次 · 累计核销 ${cumPunched} · 单次成本 ${unitCost}`; a._statusColor='var(--muted)'; a._showTopup=true; a._showAction=true; a._pct=Math.min(100,Math.max(5,((a.remainingTimes||0)/Math.max(a.totalTimes||1,1))*100)); a._critical=false; }
    else if (t==='STORED_AMOUNT_CARD') { a._priceHtml=`余额 <span>${(a.cardBalance||0).toFixed(2)}</span>`; a._statusHtml=`池上限 ${(a.totalTopup||0).toFixed(0)} · 累计消费 ${(a.totalSpent||0).toFixed(0)}`; a._statusColor='var(--muted)'; a._showTopup=true; a._showAction=true; a._pct=Math.min(100, Math.max(5, ((a.cardBalance||0) / Math.max(a.totalTopup||1, 1)) * 100)); a._critical=false; }
}

// ==================== 核心：全量数据加载（防御式，单API失败不影响其他） ====================
async function loadAll() {
    const results = await Promise.allSettled([
        api.getAssets(), api.getArchivedAssets(), api.getAchievements(), api.getInsights(), api.getDashboard()
    ]);
    const ok = (r) => r.status==='fulfilled' && r.value && r.value.success && r.value.data;
    const [ar, arcr, achr, insr, dashr] = results;

    // assets: 成功后替换整个数组（新引用 = Vue 必定响应）
    if (ok(ar)) {
        const list = [...ar.value.data];
        list.forEach(enrichAsset);
        state.assets = list;
    }
    // archived
    if (ok(arcr)) {
        const list = [...arcr.value.data];
        list.forEach(enrichAsset);
        state.archived = list;
    }
    // achievements — 从后端获取已达成但未播报的成就来弹 toast
    if (ok(achr)) {
        state.achievements = [...achr.value.data];
    }
    try {
        const pending = await api.getPendingAchievements();
        if (pending.success && pending.data && pending.data.length > 0) {
            pending.data.forEach(ac => showToast(ac.name, ac.description||'成就解锁！', 'success'));
            await api.acknowledgeAchievements(pending.data.map(a => a.id));
        }
    } catch (e) { /* 非关键 */ }
    // insights
    if (ok(insr)) { state.insights = [...insr.value.data]; }
    // dashboard
    if (ok(dashr)) {
        Object.keys(state.dashboard).forEach(k => delete state.dashboard[k]);
        Object.assign(state.dashboard, dashr.value.data);
    }
    console.log('[归藏] loadAll: assets='+state.assets.length+' archived='+state.archived.length
        +' achievements='+state.achievements.length+' insights='+state.insights.length);
}

// ==================== 分类 / 颜色映射 ====================
function catOf(a) {
    const t = a.assetType;
    if (t==='STOCKPILE') return 'stockpile';
    if (t==='COLLECTIBLE') return 'record';
    if (t&&t.startsWith('SUBSCRIPTION_')) return 'sub';
    if (t==='STORED_TIME_CARD'||t==='STORED_AMOUNT_CARD') return 'sub';
    return 'longterm';
}
function accentOf(a) {
    const map = {
        LONG_TERM_PER_USE:'var(--accent-green)', LONG_TERM_PER_DAY:'var(--accent-green)',
        STOCKPILE:'var(--accent-amber)', COLLECTIBLE:'var(--accent-rose)',
        SUBSCRIPTION_MONTHLY:'var(--accent-blue)', SUBSCRIPTION_QUARTERLY:'var(--accent-blue)',
        SUBSCRIPTION_YEARLY:'var(--accent-blue)', SUBSCRIPTION_METERED:'var(--accent-rose)',
        SUBSCRIPTION_LIFETIME:'var(--accent-green)', STORED_TIME_CARD:'var(--primary)', STORED_AMOUNT_CARD:'var(--warning)'
    };
    return map[a.assetType] || 'var(--primary)';
}
function borderOf(a) {
    const t = a.assetType;
    if (t==='LONG_TERM_PER_USE'||t==='LONG_TERM_PER_DAY') return 'rgba(138,184,154,0.2)';
    if (t==='STOCKPILE') return 'rgba(207,168,122,0.2)';
    if (t==='COLLECTIBLE') return 'rgba(207,168,122,0.15)';
    if (t&&t.startsWith('SUBSCRIPTION_')&&t!=='SUBSCRIPTION_METERED'&&t!=='SUBSCRIPTION_LIFETIME') return 'rgba(107,138,158,0.2)';
    if (t==='SUBSCRIPTION_METERED') return 'rgba(138,184,154,0.25)';
    if (t==='SUBSCRIPTION_LIFETIME') return 'rgba(166,82,82,0.18)';
    if (t==='STORED_TIME_CARD') return 'rgba(138,184,154,0.18)';
    if (t==='STORED_AMOUNT_CARD') return 'rgba(207,168,122,0.18)';
    return 'rgba(138,184,154,0.2)';
}
function progressColor(a) {
    const t = a.assetType;
    if (t&&t.startsWith('SUBSCRIPTION_')&&t!=='SUBSCRIPTION_METERED'&&t!=='SUBSCRIPTION_LIFETIME') return 'var(--accent-blue)';
    if (t==='SUBSCRIPTION_METERED') return 'var(--accent-rose)';
    if (t==='SUBSCRIPTION_LIFETIME') return 'var(--accent-green)';
    if (t==='STORED_TIME_CARD'||t==='STORED_AMOUNT_CARD') return 'var(--accent-amber)';
    return 'var(--primary)';
}

// ==================== 消息常量 ====================
const COAT_MSGS = { start:'它在角落睡得有点久了，下一个好日子，带它一起去看看世界吧。', first:'太棒了，开启了第一次重逢！它正从标签变成你生活里真实的一部分。', warm:'感知到它高频陪伴的温度了吗？谢谢你没有让它在角落里孤单落灰。', good:'它渐渐成了你日常里的一部分。每一次使用，都是你对盲目消费主义的一次优雅胜诉。', great:'它默默地融入了你生活的底色。你正在用克制和珍惜，回应当初的心动选择。', perfect:'你们已经是形影不离的老朋友了。它见过你凌晨出门，也陪你在深夜里发过呆，达成了真正不负相遇的圆满。' };
const COLLECT_STATUS = ['日常使用中', '完美珍藏中', '计划转手中'];
const COLLECT_ICONS = { '日常使用中':'fa-solid fa-check-circle', '完美珍藏中':'fa-solid fa-gem', '计划转手中':'fa-solid fa-share' };
const COLLECT_COLORS = { '日常使用中':{bg:'rgba(212,199,176,0.06)',c:'var(--text-main)'}, '完美珍藏中':{bg:'rgba(166,82,82,0.08)',c:'var(--accent-rose)'}, '计划转手中':{bg:'rgba(207,168,122,0.08)',c:'var(--accent-amber)'} };

// ==================== 派生 computed ====================
const physical = computed(() => assets.value.filter(a => ['LONG_TERM_PER_USE','LONG_TERM_PER_DAY','STOCKPILE','COLLECTIBLE'].includes(a.assetType)));
const digital = computed(() => assets.value.filter(a => { const t=a.assetType; return t&&t.startsWith('SUBSCRIPTION_')&&t!=='SUBSCRIPTION_METERED'&&t!=='SUBSCRIPTION_LIFETIME'; }));
const metered = computed(() => assets.value.filter(a => a.assetType==='SUBSCRIPTION_METERED'));
const lifetime = computed(() => assets.value.filter(a => a.assetType==='SUBSCRIPTION_LIFETIME'));
const timeCards = computed(() => assets.value.filter(a => a.assetType==='STORED_TIME_CARD'));
const amountCards = computed(() => assets.value.filter(a => a.assetType==='STORED_AMOUNT_CARD'));
const allDigital = computed(() => [...digital.value, ...metered.value, ...lifetime.value, ...timeCards.value, ...amountCards.value]);
const completedCount = computed(() => achievements.value.filter(a=>a.completed).length);
const totalValue = computed(() => assets.value.reduce((s,a)=>{
    let v = a.purchasePrice||0;
    if (a.totalTopup) v += a.totalTopup;
    if (a.totalCharged) v += a.totalCharged;
    return s + v;
},0));
const achGroups = computed(() => {
    const order = ['物品收集','细水长流','储备幸福','收藏纪念','数字订阅','资产总览','里程碑'];
    return order.map(cat=>({cat,items:achievements.value.filter(a=>a.category===cat)})).filter(g=>g.items.length);
});

// ==================== 表单数据 ====================
const f_name = ref(''); const f_price = ref(''); const f_date = ref(new Date().toISOString().slice(0,10));
const f_icon = ref(''); const f_notes = ref('');
const f_dim = ref('perDay'); const f_usageCount = ref(0); const f_collectStatus = ref('日常使用中');
const f_stockQty = ref(10); const f_stockSafe = ref(2); const f_stockUnit = ref('');
const f_storeType = ref('time'); const f_cardTimes = ref(20); const f_cardConsumed = ref(0); const f_cardAmount = ref(''); const f_cardBalance = ref(''); const f_cardTopup = ref('');
const f_subType = ref('fixed'); const f_subCycle = ref('按月续费'); const f_subNextDate = ref(new Date().toISOString().slice(0,10)); const f_apiBalance = ref(100);
const f_err = ref('');
const ICONS = ['fa-solid fa-shirt','fa-solid fa-gem','fa-solid fa-box','fa-solid fa-boxes-stacked','fa-solid fa-credit-card','fa-solid fa-camera','fa-solid fa-mobile-screen','fa-solid fa-film','fa-solid fa-microchip','fa-solid fa-cloud','fa-solid fa-dumbbell','fa-solid fa-spa','fa-solid fa-utensils','fa-solid fa-infinity','fa-solid fa-book','fa-solid fa-paintbrush','fa-solid fa-headphones','fa-solid fa-laptop','fa-solid fa-tv','fa-solid fa-gamepad','fa-solid fa-clock','fa-solid fa-wine-glass','fa-solid fa-mug-hot','fa-solid fa-bicycle','fa-solid fa-campground','fa-solid fa-wand-sparkles','fa-solid fa-heart','fa-solid fa-star','fa-solid fa-gift','fa-solid fa-wrench'];
const DEFAULT_ICONS = { longterm:'fa-solid fa-box', stockpile:'fa-solid fa-box', recordOnly:'fa-solid fa-gem', storedCard:'fa-solid fa-credit-card', digitalSub:'fa-solid fa-microchip' };

// ==================== 抽屉：新建 vs 编辑 ====================
// 价格联动：购入价变化时，自动同步到子字段（除非子字段已被手动编辑过）
const _dirty = new Set();
function markDirty(ref) { _dirty.add(ref); }
function syncPriceDefaults() {
    const p = f_price.value;
    if (!_dirty.has('cardAmount')) f_cardAmount.value = p;
    if (!_dirty.has('cardBalance')) f_cardBalance.value = p;
    if (!_dirty.has('cardTopup')) f_cardTopup.value = p;
    if (!_dirty.has('apiBalance')) f_apiBalance.value = p;
}
function resetForm() { f_name.value=''; f_price.value=''; f_date.value=new Date().toISOString().slice(0,10); f_icon.value=''; f_notes.value=''; f_err.value=''; f_stockUnit.value=''; f_usageCount.value=0; _dirty.clear(); }
function updateSubNextDate() {
    const d = f_date.value ? new Date(f_date.value + 'T00:00:00') : new Date();
    if (f_subCycle.value.includes('季')) d.setDate(d.getDate() + 90);
    else if (f_subCycle.value.includes('年')) d.setDate(d.getDate() + 365);
    else d.setDate(d.getDate() + 30);
    f_subNextDate.value = d.toISOString().slice(0,10);
}
function openAddDrawer(mode) { resetForm(); editAsset.value = null; addMode.value = mode || 'longterm'; f_icon.value = DEFAULT_ICONS[addMode.value] || ''; addDrawerOpen.value = true; updateSubNextDate(); }
async function openEditAsset(a) {
    // 先从后端拉最新数据
    let latest = a;
    try { const r = await api.getAssetById(a.id); if (r.success && r.data) { latest = r.data; enrichAsset(latest); } } catch(e) {}

    const t = latest.assetType;
    addMode.value = t==='LONG_TERM_PER_USE'||t==='LONG_TERM_PER_DAY'?'longterm':t==='STOCKPILE'?'stockpile':t==='COLLECTIBLE'?'recordOnly':t&&t.startsWith('SUBSCRIPTION_')?'digitalSub':t==='STORED_TIME_CARD'||t==='STORED_AMOUNT_CARD'?'storedCard':'longterm';
    // 通用字段
    f_name.value = latest.name||'';
    f_price.value = latest.purchasePrice||'';
    f_date.value = latest.purchaseDate||'';
    f_icon.value = latest.icon||'';
    f_notes.value = latest.notes||'';
    // 类型专属字段 —— 全部从 DB 取实时值回填
    if (t==='LONG_TERM_PER_USE') { f_dim.value = 'perUse'; f_usageCount.value = latest.usageCount||0; }
    else if (t==='LONG_TERM_PER_DAY') { f_dim.value = 'perDay'; }
    else if (t==='COLLECTIBLE') { f_collectStatus.value = latest.collectStatus||'日常使用中'; }
    else if (t==='STOCKPILE') { f_stockQty.value = latest.currentStock||0; f_stockSafe.value = latest.safetyStock||0; }
    else if (t==='STORED_TIME_CARD') { f_storeType.value = 'time'; f_cardTimes.value = latest.totalTimes||0; f_cardConsumed.value = (latest.totalTimes||0) - (latest.remainingTimes||0); f_cardAmount.value = latest.totalTopup||''; }
    else if (t==='STORED_AMOUNT_CARD') { f_storeType.value = 'amount'; f_cardBalance.value = latest.cardBalance||''; f_cardTopup.value = latest.totalTopup||''; }
    else if (t&&t.startsWith('SUBSCRIPTION_')) {
        if (t==='SUBSCRIPTION_METERED') { f_subType.value = 'metered'; f_apiBalance.value = latest.apiBalance||''; }
        else if (t==='SUBSCRIPTION_LIFETIME') { f_subType.value = 'permanent'; }
        else { f_subType.value = 'fixed'; f_subCycle.value = 'MONTHLY'===latest.billingCycle?'按月续费':'QUARTERLY'===latest.billingCycle?'按季续费':'按年续费'; f_subNextDate.value = latest.nextBillingDate||''; }
    }
    addDrawerOpen.value = true;
    editAsset.value = latest;
}

// ==================== 操作：每个都 API + loadAll 双保险 ====================
async function doCheckIn(a) {
    const r = await api.checkIn(a.id);
    if (r.success) {
        await loadAll();
    } else { showToast('操作失败', r.message || '打卡未成功', 'warning'); }
}
async function doConsume(a) {
    const r = await api.consumeStock(a.id, 1, '消耗');
    if (!r.success) showToast('消耗失败', r.message || '请重试', 'warning');
    await loadAll();
}
async function doRestock(a, qty, total) {
    const r = await api.restock(a.id, parseInt(qty)||0, parseFloat(total)||0);
    if (r.success) {
        if (r.compare) {
            // 先挂到旧资产上立即显示比价条
            const old = state.assets.find(x => x.id === a.id);
            if (old) old._feedback = r.compare;
            // toast 用纯文本短消息，对齐 HTML 示例卡片的语气
            const toastMsg = r.compare.verdict === 'good' ? '省钱成功！本次购买触发史低极优判定。' :
                             r.compare.verdict === 'bad' ? '本次单价触及史高，已记录入库。' :
                             '补货完毕，价格处于历史平稳波动区间。';
            const toastType = r.compare.verdict === 'good' ? 'success' : r.compare.verdict === 'bad' ? 'warning' : 'info';
            showToast('价格省心雷达', toastMsg, toastType);
        }
        await loadAll();
        // loadAll 后用新资产重新挂上比价条（8秒后自动消失）
        if (r.compare) {
            const fresh = state.assets.find(x => x.id === a.id);
            if (fresh) { fresh._feedback = r.compare; setTimeout(() => { if(fresh._feedback===r.compare) fresh._feedback=null; }, 8000); }
        }
    } else { showToast('补货失败', r.message || '请重试', 'warning'); await loadAll(); }
}
async function doDelete() {
    if (confirmTarget.value) {
        const r = await api.deleteAsset(confirmTarget.value.id);
        if (r.success) showToast('已删除', '资产已永久抹除', 'warning');
        else showToast('删除失败', r.message || '', 'danger');
    }
    confirmOpen.value = false;
    await loadAll();
}
async function doArchive() {
    if (confirmTarget.value) {
        const r = await api.archiveAsset(confirmTarget.value.id);
        if (r.success) showToast('已归档', '资产已移入封存仓库', 'info');
        else showToast('归档失败', r.message || '', 'warning');
    }
    confirmOpen.value = false;
    await loadAll();
}
async function doRestore(a) {
    const r = await api.restoreAsset(a.id);
    if (r.success) showToast('已唤醒', '资产已恢复', 'success');
    else showToast('唤醒失败', r.message || '', 'warning');
    await loadAll();
}
async function doPunch(a) {
    const r = await api.punchCard(a.id);
    if (r.success) {
        if (r.remaining===0) showToast('卡券完全履约！', '次数已用完', 'success');
        await loadAll();
    } else { showToast('核销失败', r.message || '次数不足', 'warning'); }
}
async function doTopup(a, amount, times) {
    const r = await api.topup(a.id, parseFloat(amount)||0, parseInt(times)||0);
    if (!r.success) showToast('充值失败', r.message || '请重试', 'warning');
    await loadAll();
}
async function doSpend(a, amount) {
    const r = await api.spend(a.id, parseFloat(amount)||0);
    if (!r.success) showToast('消费失败', r.message || '余额不足', 'warning');
    await loadAll();
}
async function doCycleStatus(a) {
    const r = await api.updateStatus(a.id);
    if (r.success) showToast('状态已更新', r.status || '', 'info');
    else showToast('状态切换失败', r.message || '', 'warning');
    await loadAll();
}

// ==================== 资产提交：创建 / 编辑 ====================
async function submitAddAsset() {
    if (!f_name.value.trim()) { f_err.value = '请输入名称'; return; }
    const payload = { name: f_name.value.trim(), purchasePrice: parseFloat(f_price.value)||0, purchaseDate: f_date.value, icon: f_icon.value, notes: f_notes.value };
    const m = addMode.value;
    if (m==='longterm') { payload.assetType = f_dim.value==='perUse'?'LONG_TERM_PER_USE':'LONG_TERM_PER_DAY'; if (f_dim.value==='perUse') payload.usageCount = parseInt(f_usageCount.value)||0; }
    else if (m==='stockpile') { payload.assetType='STOCKPILE'; payload.currentStock=parseFloat(f_stockQty.value)||0; payload.safetyStock=parseFloat(f_stockSafe.value)||0; payload.notes=(f_stockUnit.value?'单位:'+f_stockUnit.value+'; ':'')+(f_notes.value||''); }
    else if (m==='recordOnly') { payload.assetType='COLLECTIBLE'; payload.collectStatus=f_collectStatus.value; }
    else if (m==='storedCard') { payload.assetType=f_storeType.value==='time'?'STORED_TIME_CARD':'STORED_AMOUNT_CARD'; const pp=parseFloat(f_price.value)||0; if(f_storeType.value==='time'){payload.totalTimes=parseInt(f_cardTimes.value)||0; const consumed=parseInt(f_cardConsumed.value)||0; payload.remainingTimes=Math.max(0,(payload.totalTimes||0)-consumed); payload.totalTopup=parseFloat(f_cardAmount.value)||pp;}else{payload.cardBalance=parseFloat(f_cardBalance.value)||pp; payload.totalTopup=parseFloat(f_cardTopup.value)||pp;} }
    else if (m==='digitalSub') { if(f_subType.value==='metered'){payload.assetType='SUBSCRIPTION_METERED'; const pp=parseFloat(f_price.value)||0; payload.apiBalance=parseFloat(f_apiBalance.value)||pp; payload.totalCharged=payload.apiBalance;} else if(f_subType.value==='permanent'){payload.assetType='SUBSCRIPTION_LIFETIME';} else {payload.assetType=f_subCycle.value.includes('季')?'SUBSCRIPTION_QUARTERLY':f_subCycle.value.includes('年')?'SUBSCRIPTION_YEARLY':'SUBSCRIPTION_MONTHLY'; payload.billingCycle=f_subCycle.value.includes('季')?'QUARTERLY':f_subCycle.value.includes('年')?'YEARLY':'MONTHLY'; payload.nextBillingDate=f_subNextDate.value; const pp=parseFloat(f_price.value)||0; payload.monthlyCost=payload.billingCycle==='QUARTERLY'?pp/3:payload.billingCycle==='YEARLY'?pp/12:pp;} }

    f_err.value = '';
    try {
        if (editAsset.value) {
            // === 编辑模式：把所有当前表单值传回后端 ===
            const r = await api.updateAsset(editAsset.value.id, payload);
            if (r.success) { addDrawerOpen.value = false; editAsset.value = null; await loadAll(); showToast('已更新', payload.name+' 信息已保存', 'success'); }
            else f_err.value = r.message || '更新失败';
        } else {
            // === 创建模式 ===
            const r = await api.createAsset(payload);
            if (r.success) {
                // 两阶段：① 后端返回的资产立即注入本地 ② 全量刷新保一致
                if (r.data) { enrichAsset(r.data); state.assets = [...state.assets, r.data]; }
                addDrawerOpen.value = false;
                await loadAll();
                showToast('创建成功', payload.name+' 已挂载', 'success');
            } else f_err.value = r.message || '创建失败';
        }
    } catch(e) { f_err.value = e.message || '网络异常'; }
}

// ==================== 弹窗辅助 ====================
function stockPct(a) { const s=a.currentStock||0, sf=a.safetyStock||1; return Math.max(5, Math.min(100, s/Math.max(s+sf,1)*100)); }
function priceDotPos(a) { const min=a._priceMin||0, max=a._priceMax||1, cur=a._avgPrice||0; if(max<=min) return 50; return Math.max(5,Math.min(95,((cur-min)/(max-min))*100)); }
const ACT_LABELS = { CHECK_IN:'记录陪伴', CONSUME:'消耗', RESTOCK:'补货入库', TOPUP:'充值', SPEND:'消费', PUNCH:'核销', STATUS_CHANGE:'状态变更', CREATE:'创建资产', UPDATE_META:'编辑元数据' };
function actLabel(t) { return ACT_LABELS[t] || t; }
function openConfirm(type, a) { confirmType.value = type; confirmTarget.value = a; confirmOpen.value = true; }
function openHistory(a) { historyAssetId.value = a.id; historyOpen.value = true; historyLogs.value = []; api.getHistory(a.id).then(r => { if(r.success) historyLogs.value = r.data||[]; }); }

const fm_amount = ref(0); const fm_qty = ref(1);
async function openFormModal(mode, a) {
    // 先从后端拉取最新资产数据（避免使用过期前端缓存）
    let latest = a;
    try {
        const r = await api.getAssetById(a.id);
        if (r.success && r.data) { latest = r.data; enrichAsset(latest); }
    } catch(e) { /* 网络异常则退回到前端缓存 */ }

    formModalMode.value = mode; formModalAsset.value = latest;
    if (mode==='restock') { formModalTitle.value = latest.name + ' 快捷补货录入'; fm_qty.value = 5; const avg = latest._avgPrice||(latest.purchasePrice||0); fm_amount.value = parseFloat((avg * fm_qty.value).toFixed(2)); }
    else if (mode==='topup_time') { formModalTitle.value = latest.name + ' 充值次数'; fm_amount.value = 0; fm_qty.value = 1; }
    else if (mode==='topup_amount') { formModalTitle.value = latest.name + ' 充值金额'; fm_amount.value = 0; }
    else if (mode==='spend') { formModalTitle.value = latest.name + ' 记录消费'; fm_amount.value = 0; }
    // 确保所有金额和数量都是数字类型（输入框可能返回字符串）
    fm_amount.value = parseFloat(fm_amount.value)||0;
    fm_qty.value = parseInt(fm_qty.value)||1;
    formModalOpen.value = true;
}
async function submitFormModal() {
    const a = formModalAsset.value;
    try {
        if (formModalMode.value==='restock') await doRestock(a, fm_qty.value, fm_amount.value);
        else if (formModalMode.value==='topup_time') await doTopup(a, fm_amount.value, fm_qty.value);
        else if (formModalMode.value==='topup_amount'||formModalMode.value==='spend') {
            if (formModalMode.value==='topup_amount') await doTopup(a, fm_amount.value, 0);
            else await doSpend(a, fm_amount.value);
        }
    } catch(e) { showToast('操作异常', e.message || '请重试', 'danger'); }
    formModalOpen.value = false;
}

// ==================== 认证 ====================
async function doAuth() {
    authErr.value = '';
    try {
        if (authMode.value==='login') {
            const r = await api.login(auth_account.value, auth_password.value);
            if (r.success) { user.value = r.user; phase.value = 'main'; await loadAll(); }
            else authErr.value = r.message || '登录失败';
        } else {
            const uname = auth_username.value || auth_account.value;
            if (auth_password.value !== auth_confirm.value) { authErr.value = '两次密码不一致'; return; }
            const r = await api.register(uname, auth_password.value, auth_nickname.value||uname);
            if (r.success) { const lr = await api.login(uname, auth_password.value); if (lr.success) { user.value = lr.user; phase.value = 'main'; await loadAll(); } else authErr.value = '注册成功，请登录'; }
            else authErr.value = r.message || '注册失败';
        }
    } catch(e) { authErr.value = e.message || '网络错误'; }
}

// ==================== 挂载 ====================
const app = createApp({
    setup() {
        function toggleTheme() { document.body.classList.toggle('light'); }
        async function skipWelcome() {
            try { const s = await api.getSession(); if (s.loggedIn) { user.value = s.user; phase.value = 'main'; await loadAll(); } else phase.value = 'auth'; }
            catch(e) { phase.value = 'auth'; }
        }
        function cycleCollect(a) { doCycleStatus(a); }
        setTimeout(() => { if (phase.value==='welcome') skipWelcome(); }, 3200);

        return {
            phase, view, drawerOpen, authMode, authErr, auth_account, auth_password, auth_username, auth_nickname, auth_confirm,
            addDrawerOpen, addMode, f_name, f_price, f_date, f_icon, f_notes, f_dim, f_usageCount, f_collectStatus, f_stockQty, f_stockSafe, f_stockUnit,
            f_storeType, f_cardTimes, f_cardConsumed, f_cardAmount, f_cardBalance, f_cardTopup, f_subType, f_subCycle, f_subNextDate, f_apiBalance, f_err,
            confirmOpen, confirmType, confirmTarget, formModalOpen, formModalMode, formModalTitle, fm_amount, fm_qty,
            historyOpen, historyLogs, historyAssetId, editAsset, toastList, ICONS, COLLECT_STATUS, COLLECT_ICONS, COLLECT_COLORS,
            user, assets, archived, achievements, dashboard, insights, physical, digital, metered, lifetime, timeCards, amountCards, allDigital, completedCount, totalValue, achGroups,
            COAT_MSGS, catOf, accentOf, borderOf, actLabel, loadAll, showToast, toggleTheme, skipWelcome,
            doAuth, submitAddAsset, openAddDrawer, resetForm, updateSubNextDate, syncPriceDefaults, markDirty,
            avatarIcons, avatarIdx, currentAvatar, showAvatarPicker, selectAvatar,
            showNicknameModal, editNickname, showPasswordModal, oldPw, newPw, confirmPw, saveNickname, changePassword,
            stockPct, priceDotPos, progressColor, openEditAsset,
            doCheckIn, doConsume, doPunch, doDelete, doArchive, doRestore, openConfirm, openHistory, openFormModal, submitFormModal, cycleCollect
        };
    }
});

app.mount('#app');
