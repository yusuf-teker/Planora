/**
 * HealthTrack & Planora Unified API Client
 * Connects to Render Backend (https://pulse-7b4z.onrender.com) and Neon PostgreSQL
 */

const DEFAULT_REMOTE_URL = 'https://pulse-7b4z.onrender.com';

// Automatically clean up any corrupted custom URL from earlier sessions
if (typeof window !== 'undefined' && window.localStorage) {
  try {
    localStorage.removeItem('custom_backend_url');
  } catch (_) {}
}

export function getBaseUrl() {
  // If running in development with Vite proxy:
  if (import.meta.env.DEV) {
    return '/api';
  }

  // In production: if hosted on the same origin (e.g. on Render server), use window.location.origin
  if (typeof window !== 'undefined' && window.location && window.location.origin) {
    if (!window.location.origin.includes('localhost') && !window.location.origin.includes('127.0.0.1')) {
      return window.location.origin;
    }
  }

  return DEFAULT_REMOTE_URL;
}

export function setCustomBaseUrl(url) {
  // Deprecated - intentionally no-op to prevent broken URLs
}

async function request(path, options = {}) {
  const baseUrl = getBaseUrl();
  const url = `${baseUrl}${path.startsWith('/') ? path : `/${path}`}`;
  
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  };

  const response = await fetch(url, {
    ...options,
    headers,
  });

  const contentType = response.headers.get('content-type') || '';
  let data = null;
  if (contentType.includes('application/json')) {
    data = await response.json();
  } else {
    data = await response.text();
  }

  if (!response.ok) {
    const errorMsg = (typeof data === 'object' && data?.message) || 
                     (typeof data === 'object' && data?.error) || 
                     (typeof data === 'string' && data) ||
                     `HTTP Hatası: ${response.status} ${response.statusText}`;
    throw new Error(errorMsg);
  }

  return data;
}

// ─────────────────────────────────────────────────────────────
// AUTHENTICATION
// ─────────────────────────────────────────────────────────────

export async function login(identifier, password) {
  return request('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ identifier, password }),
  });
}

// ─────────────────────────────────────────────────────────────
// HEALTHTRACK DATA
// ─────────────────────────────────────────────────────────────

export async function getHealthData(token, since = 0) {
  return request(`/health/sync/pull?since=${since}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
}

export async function getNutritionGoals(token) {
  return request('/health/nutrition/goals', {
    headers: { Authorization: `Bearer ${token}` },
  });
}

// ─────────────────────────────────────────────────────────────
// PLANORA DATA
// ─────────────────────────────────────────────────────────────

export async function getPlanoraTasks(token, fromTime = null, toTime = null) {
  let query = '';
  const params = [];
  if (fromTime) params.push(`from=${fromTime}`);
  if (toTime) params.push(`to=${toTime}`);
  if (params.length > 0) query = `?${params.join('&')}`;

  return request(`/tasks${query}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
}

// ─────────────────────────────────────────────────────────────
// ADMIN ENDPOINTS
// ─────────────────────────────────────────────────────────────

function getAdminHeaders(token, secret) {
  const headers = {};
  if (token) headers['Authorization'] = `Bearer ${token}`;
  if (secret) headers['X-Admin-Secret'] = secret;
  return headers;
}

export async function getAdminStats(token, secret) {
  return request('/admin/stats', {
    headers: getAdminHeaders(token, secret),
  });
}

export async function getAdminUsers(token, query = '', secret) {
  const q = query ? `?query=${encodeURIComponent(query)}` : '';
  return request(`/admin/users${q}`, {
    headers: getAdminHeaders(token, secret),
  });
}

export async function getAdminUserTasks(token, userId, secret) {
  return request(`/admin/users/${userId}/tasks`, {
    headers: getAdminHeaders(token, secret),
  });
}

export async function updateAdminUserRole(token, userId, role, secret) {
  return request(`/admin/users/${userId}/role`, {
    method: 'PUT',
    headers: getAdminHeaders(token, secret),
    body: JSON.stringify({ role }),
  });
}

export async function setAdminUserPremium(token, { userId, email, isPremium, days }, secret) {
  return request('/admin/users/set-premium', {
    method: 'POST',
    headers: getAdminHeaders(token, secret),
    body: JSON.stringify({ userId, email, isPremium, days }),
  });
}

export async function deleteAdminUser(token, userId, secret) {
  return request(`/admin/users/${userId}`, {
    method: 'DELETE',
    headers: getAdminHeaders(token, secret),
  });
}

export async function getAdminPushTargets(token, query = '', secret) {
  const q = query ? `?query=${encodeURIComponent(query)}` : '';
  return request(`/admin/push-targets${q}`, {
    headers: getAdminHeaders(token, secret),
  });
}

export async function sendAdminPush(token, pushPayload, secret) {
  return request('/admin/push', {
    method: 'POST',
    headers: getAdminHeaders(token, secret),
    body: JSON.stringify(pushPayload),
  });
}

// ─────────────────────────────────────────────────────────────
// SYSTEM TELEMETRY & HEALTH
// ─────────────────────────────────────────────────────────────

export async function checkServerPing() {
  const start = performance.now();
  const baseUrl = getBaseUrl();

  try {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 6000);
    const res = await fetch(`${baseUrl}/health`, { signal: controller.signal });
    clearTimeout(timer);
    if (res.ok) {
      const latency = Math.round(performance.now() - start);
      return { ok: true, latency };
    }
  } catch (_) {}

  // Fallback to /ping
  try {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 4000);
    const res = await fetch(`${baseUrl}/ping`, { signal: controller.signal });
    clearTimeout(timer);
    if (res.ok) {
      const latency = Math.round(performance.now() - start);
      return { ok: true, latency };
    }
  } catch (_) {}

  return { ok: false, error: 'Sunucuya ulaşılamıyor' };
}

export async function checkDbStatus(token) {
  try {
    const data = await request('/health/db', {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    });
    return { ok: true, data };
  } catch (err) {
    return { ok: false, error: err.message };
  }
}

