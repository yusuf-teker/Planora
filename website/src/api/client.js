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

async function executeFetch(url, options) {
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  };

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 35000); // 35s timeout for cold starts

  try {
    const response = await fetch(url, {
      ...options,
      headers,
      signal: controller.signal,
    });
    clearTimeout(timeoutId);

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
  } catch (err) {
    clearTimeout(timeoutId);
    if (err.name === 'AbortError') {
      throw new Error('Sunucu yanıt vermedi (Zaman aşımı). Render sunucusu uyanıyor olabilir, lütfen birkaç saniye sonra tekrar deneyin.');
    }
    throw err;
  }
}

async function request(path, options = {}) {
  const baseUrl = getBaseUrl();
  const url = `${baseUrl}${path.startsWith('/') ? path : `/${path}`}`;

  try {
    return await executeFetch(url, options);
  } catch (firstErr) {
    // If request failed and it wasn't a 4xx client auth error, retry once after a short pause (helps when Render is waking up)
    const isAuthError = firstErr.message?.includes('401') || firstErr.message?.includes('403');
    if (!isAuthError) {
      try {
        await new Promise((r) => setTimeout(r, 1200));
        return await executeFetch(url, options);
      } catch (retryErr) {
        throw new Error(retryErr.message || 'Sunucuya bağlanılamadı. Lütfen internet bağlantınızı ve sunucu durumunu kontrol edin.');
      }
    }
    throw firstErr;
  }
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

export async function deleteAccount(token) {
  return request('/users/me', {
    method: 'DELETE',
    headers: { Authorization: `Bearer ${token}` },
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

export async function pushHealthData(token, payload) {
  return request('/health/sync/push', {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
    body: JSON.stringify(payload),
  });
}

export async function seedSampleHealthData(token, targetDate = new Date().toISOString().split('T')[0]) {
  const now = Date.now();
  const samplePayload = {
    foods: [
      {
        id: `sample_food_breakfast_${now}`,
        date: targetDate,
        rawText: '2 haşlanmış yumurta, 50g beyaz peynir, 5 zeytin, 1 dilim kızarmış tam buğday ekmeği ve açık çay',
        summary: 'Zengin Proteinli Akdeniz Kahvaltısı',
        totalCalories: 430,
        itemsJson: JSON.stringify([
          { name: 'Haşlanmış Yumurta (2 adet)', calories: 150, protein: 13, carbs: 1, fat: 10 },
          { name: 'Tam Yağlı Beyaz Peynir (50g)', calories: 130, protein: 8, carbs: 1, fat: 11 },
          { name: 'Siyah Zeytin (5 adet)', calories: 45, protein: 0.5, carbs: 1, fat: 4.5 },
          { name: 'Tam Buğday Ekmeği (1 dilim)', calories: 105, protein: 4, carbs: 21, fat: 1.5 }
        ]),
        updatedAt: now,
        isDeleted: false
      },
      {
        id: `sample_food_lunch_${now}`,
        date: targetDate,
        rawText: '180g ızgara tavuk göğsü, mevsim salata zeytinyağlı, 1 kase mercimek çorbası ve maden suyu',
        summary: 'Izgara Tavuk Göğsü & Mercimek Çorbası',
        totalCalories: 620,
        itemsJson: JSON.stringify([
          { name: 'Izgara Tavuk Göğsü (180g)', calories: 300, protein: 54, carbs: 0, fat: 6 },
          { name: 'Mercimek Çorbası (1 kase)', calories: 180, protein: 10, carbs: 26, fat: 4 },
          { name: 'Zeytinyağlı Akdeniz Salata', calories: 140, protein: 2, carbs: 7, fat: 12 }
        ]),
        updatedAt: now,
        isDeleted: false
      },
      {
        id: `sample_food_dinner_${now}`,
        date: targetDate,
        rawText: 'Fırında somon balığı, buharda brokoli ve havuç, 4 yemek kaşığı basmati pirinç pilavı',
        summary: 'Omega-3 Fırın Somon & Buharda Sebzeler',
        totalCalories: 580,
        itemsJson: JSON.stringify([
          { name: 'Fırın Somon Fileto (150g)', calories: 330, protein: 34, carbs: 0, fat: 20 },
          { name: 'Basmati Pilavı (1 porsiyon)', calories: 170, protein: 3, carbs: 36, fat: 1.5 },
          { name: 'Buharda Brokoli & Havuç', calories: 80, protein: 3, carbs: 14, fat: 0.5 }
        ]),
        updatedAt: now,
        isDeleted: false
      }
    ],
    nutritionGoals: {
      id: `sample_goal_${now}`,
      calories: 2150,
      proteinG: 135.0,
      carbsG: 210.0,
      fatG: 65.0,
      fiberG: 32.0,
      vitaminCMg: 95.0,
      vitaminDUg: 15.0,
      vitaminB12Ug: 2.5,
      ironMg: 14.0,
      calciumMg: 1000.0,
      magnesiumMg: 350.0,
      updatedAt: now,
      isDeleted: false
    },
    workouts: [
      {
        id: `sample_workout_1_${now}`,
        date: targetDate,
        rawText: '40 dakika tempolu kardiyo koşu ve 15 dakika esneme / core egzersizleri',
        caloriesBurned: 410,
        durationMinutes: 55,
        targetCalories: 350,
        targetDurationMinutes: 45,
        aiAnalysis: 'Mükemmel aerobik dayanıklılık seansı. Hedef nabız aralığında yağ yakımı optimize edildi.',
        updatedAt: now,
        isDeleted: false
      }
    ],
    weights: [
      {
        id: `sample_weight_1_${now}`,
        date: targetDate,
        weightKg: 76.4,
        targetWeightKg: 73.5,
        note: 'Sabah aç karnına akıllı tartı ölçümü',
        updatedAt: now,
        isDeleted: false
      }
    ]
  };

  return pushHealthData(token, samplePayload);
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

