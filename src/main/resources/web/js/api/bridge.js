/**
 * API 桥接层 — fetch 封装，调用 Java HTTP Server
 */
import { API_BASE } from '../config.js';

async function request(method, path, body) {
    const opts = { method, headers: { 'Content-Type': 'application/json' } };
    if (body) opts.body = JSON.stringify(body);
    const res = await fetch(API_BASE + path, opts);
    const data = await res.json();
    if (!data.success && data.message) {
        console.warn('[API]', path, data.message);
    }
    return data;
}

// ===== 认证 =====
export const login = (username, password) =>
    request('POST', '/login', { username, password });

export const register = (username, password, nickname) =>
    request('POST', '/register', { username, password, nickname });

export const getSession = () =>
    request('GET', '/session');

// ===== 资产 =====
export const getAssets = () =>
    request('GET', '/assets');

export const getArchivedAssets = () =>
    request('GET', '/assets/archived');

export const createAsset = (asset) =>
    request('POST', '/assets', asset);

export const updateAsset = (id, asset) =>
    request('PUT', '/assets/' + id, asset);

export const deleteAsset = (id) =>
    request('DELETE', '/assets/' + id);

export const archiveAsset = (id) =>
    request('POST', '/assets/' + id + '/archive');

export const restoreAsset = (id) =>
    request('POST', '/assets/' + id + '/restore');

// ===== 操作 =====
export const checkIn = (id) =>
    request('POST', '/assets/' + id + '/check-in');

export const consumeStock = (assetId, quantity, notes) =>
    request('POST', '/assets/' + assetId + '/consume', { assetId, quantity, notes });

export const restock = (assetId, quantity, totalPrice) =>
    request('POST', '/assets/' + assetId + '/restock', { assetId, quantity, totalPrice });

export const punchCard = (id) =>
    request('POST', '/assets/' + id + '/punch');

export const topup = (assetId, amount, times) =>
    request('POST', '/assets/' + assetId + '/topup', { assetId, amount, times: times || 0 });

export const spend = (assetId, amount) =>
    request('POST', '/assets/' + assetId + '/spend', { assetId, amount });

export const updateStatus = (id) =>
    request('PUT', '/assets/' + id + '/status');

export const comparePrice = (assetId, externalPrice) =>
    request('POST', '/assets/' + assetId + '/compare', { assetId, externalPrice });

export const getHistory = (id) =>
    request('GET', '/assets/' + id + '/history');

// ===== 仪表盘/成就/洞察 =====
export const getDashboard = () =>
    request('GET', '/dashboard');

export const getAchievements = () =>
    request('GET', '/achievements');

export const getPendingAchievements = () =>
    request('GET', '/achievements/pending');

export const acknowledgeAchievements = (ids) =>
    request('POST', '/achievements/acknowledge', { ids });

export const getInsights = () =>
    request('GET', '/insights');

// ===== 配置 =====
export const getConfig = (key) =>
    request('GET', '/config?key=' + encodeURIComponent(key));

export const setConfig = (key, value) =>
    request('POST', '/config', { key, value });

// ===== 导出 =====
export const exportData = () =>
    request('GET', '/export');

// ===== 单项资产 =====
export const getAssetById = (id) =>
    request('GET', '/assets/' + id);
