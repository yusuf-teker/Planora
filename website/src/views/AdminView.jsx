import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import * as api from '../api/client';
import { 
  ShieldCheck, 
  Send, 
  Users, 
  Database, 
  Crown, 
  Search, 
  RefreshCw, 
  CheckCircle2, 
  AlertCircle, 
  Trash2, 
  Key, 
  Smartphone, 
  Bell, 
  Sparkles, 
  Flame, 
  CheckSquare, 
  Heart,
  BarChart3,
  ExternalLink,
  ChevronRight,
  UserCheck,
  UserX,
  X
} from 'lucide-react';

export default function AdminView() {
  const { token, adminSecret, setAdminSecret, user } = useAuth();
  const [activeTab, setActiveTab] = useState('push'); // 'push', 'users', 'system'

  // Push State
  const [pushMode, setPushMode] = useState('BROADCAST'); // 'BROADCAST', 'SPECIFIC'
  const [pushTitle, setPushTitle] = useState('');
  const [pushBody, setPushBody] = useState('');
  const [pushUserQuery, setPushUserQuery] = useState('');
  const [selectedUser, setSelectedUser] = useState(null);
  const [pushTargets, setPushTargets] = useState([]);
  const [searchingTargets, setSearchingTargets] = useState(false);
  const [pushSending, setPushSending] = useState(false);
  const [pushResult, setPushResult] = useState(null);

  // Users Management State
  const [usersList, setUsersList] = useState([]);
  const [usersQuery, setUsersQuery] = useState('');
  const [loadingUsers, setLoadingUsers] = useState(false);
  const [selectedUserTasks, setSelectedUserTasks] = useState(null);
  const [loadingTasksModal, setLoadingTasksModal] = useState(false);
  const [userActionFeedback, setUserActionFeedback] = useState(null);

  // Stats State
  const [stats, setStats] = useState(null);
  const [loadingStats, setLoadingStats] = useState(false);
  const [dbStatus, setDbStatus] = useState(null);

  // Force Update / App Configs State
  const [appConfigs, setAppConfigs] = useState([]);
  const [loadingConfigs, setLoadingConfigs] = useState(false);
  const [savingConfigAppId, setSavingConfigAppId] = useState(null);
  const [configFeedback, setConfigFeedback] = useState(null);

  // Load Initial Data
  useEffect(() => {
    loadStats();
    loadUsers();
    loadAppConfigs();
  }, [token, adminSecret]);

  const loadAppConfigs = async () => {
    setLoadingConfigs(true);
    try {
      const data = await api.getAdminAppConfigs(token, adminSecret);
      setAppConfigs(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('App configs load error:', err);
    } finally {
      setLoadingConfigs(false);
    }
  };

  const handleAppConfigFieldChange = (appId, field, value) => {
    setAppConfigs((prev) =>
      prev.map((cfg) => (cfg.appId === appId ? { ...cfg, [field]: value } : cfg))
    );
  };

  const handleSaveAppConfig = async (cfg) => {
    setSavingConfigAppId(cfg.appId);
    setConfigFeedback(null);
    try {
      const updated = await api.updateAdminAppConfig(token, cfg.appId, cfg, adminSecret);
      setAppConfigs((prev) => prev.map((c) => (c.appId === cfg.appId ? updated : c)));
      setConfigFeedback({
        type: 'success',
        message: `${cfg.appId === 'healthtrack' ? 'HealthTrack' : 'Planora'} Force Update ayarları kaydedildi.`,
      });
    } catch (err) {
      setConfigFeedback({
        type: 'error',
        message: err.message || 'Versiyon ayarları kaydedilemedi.',
      });
    } finally {
      setSavingConfigAppId(null);
    }
  };

  const loadStats = async () => {
    setLoadingStats(true);
    try {
      const data = await api.getAdminStats(token, adminSecret);
      setStats(data);
      const db = await api.checkDbStatus(token);
      setDbStatus(db);
    } catch (err) {
      console.error('Stats error:', err);
    } finally {
      setLoadingStats(false);
    }
  };

  const loadUsers = async (query = '') => {
    setLoadingUsers(true);
    try {
      const data = await api.getAdminUsers(token, query, adminSecret);
      setUsersList(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Users load error:', err);
    } finally {
      setLoadingUsers(false);
    }
  };

  // Push user search
  const searchPushTargets = async (query) => {
    setPushUserQuery(query);
    if (!query || query.length < 2) {
      setPushTargets([]);
      return;
    }
    setSearchingTargets(true);
    try {
      const data = await api.getAdminPushTargets(token, query, adminSecret);
      setPushTargets(Array.isArray(data) ? data : []);
    } catch (e) {
      console.error(e);
    } finally {
      setSearchingTargets(false);
    }
  };

  // Push Sending
  const handleSendPush = async (e) => {
    e.preventDefault();
    if (!pushTitle.trim() || !pushBody.trim()) {
      alert('Lütfen başlık ve mesaj içeriği girin.');
      return;
    }

    if (pushMode === 'SPECIFIC' && !selectedUser) {
      alert('Lütfen bildirim göndermek için bir kullanıcı seçin.');
      return;
    }

    setPushSending(true);
    setPushResult(null);

    const payload = {
      title: pushTitle.trim(),
      body: pushBody.trim(),
      broadcastAll: pushMode === 'BROADCAST',
      userId: pushMode === 'SPECIFIC' ? selectedUser.id : null,
      data: {
        sender: 'Planora_HealthTrack_Portal_Admin',
        timestamp: Date.now().toString(),
      }
    };

    try {
      const res = await api.sendAdminPush(token, payload, adminSecret);
      setPushResult({ ok: res.success !== false, message: res.message || 'Bildirim iletildi!' });
      if (res.success !== false) {
        setPushTitle('');
        setPushBody('');
        setSelectedUser(null);
      }
    } catch (err) {
      setPushResult({ ok: false, message: err.message || 'Bildirim gönderilemedi.' });
    } finally {
      setPushSending(false);
    }
  };

  // Push Templates
  const applyTemplate = (title, body) => {
    setPushTitle(title);
    setPushBody(body);
  };

  // User Actions: Set Premium
  const handleSetPremium = async (targetUser, days, isPremium = true) => {
    try {
      const res = await api.setAdminUserPremium(token, {
        userId: targetUser.id,
        isPremium,
        days,
      }, adminSecret);
      setUserActionFeedback({ ok: true, message: res.message || 'Premium güncellendi.' });
      loadUsers(usersQuery);
      loadStats();
    } catch (err) {
      setUserActionFeedback({ ok: false, message: err.message });
    }
  };

  // User Actions: Update Role
  const handleUpdateRole = async (targetUser, newRole) => {
    try {
      await api.updateAdminUserRole(token, targetUser.id, newRole, adminSecret);
      setUserActionFeedback({ ok: true, message: `'${targetUser.username}' kullanıcısının rolü '${newRole}' yapıldı.` });
      loadUsers(usersQuery);
    } catch (err) {
      setUserActionFeedback({ ok: false, message: err.message });
    }
  };

  // User Actions: Delete User
  const handleDeleteUser = async (targetUser) => {
    if (!confirm(`DİKKAT: '${targetUser.username}' kullanıcısını ve tüm verilerini kalıcı olarak silmek istediğinizden emin misiniz?`)) {
      return;
    }

    try {
      const res = await api.deleteAdminUser(token, targetUser.id, adminSecret);
      setUserActionFeedback({ ok: true, message: res.message || 'Kullanıcı silindi.' });
      loadUsers(usersQuery);
      loadStats();
    } catch (err) {
      setUserActionFeedback({ ok: false, message: err.message });
    }
  };

  // User Actions: Inspect Tasks
  const handleInspectTasks = async (targetUser) => {
    setLoadingTasksModal(true);
    setSelectedUserTasks(null);
    try {
      const data = await api.getAdminUserTasks(token, targetUser.id, adminSecret);
      setSelectedUserTasks(data);
    } catch (err) {
      alert(`Görevler alınamadı: ${err.message}`);
    } finally {
      setLoadingTasksModal(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-white/5 pb-6">
        <div>
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-amber-500 to-rose-500 flex items-center justify-center text-white shadow-glow-gold">
              <ShieldCheck className="w-4 h-4" />
            </div>
            <h1 className="text-2xl font-extrabold text-white tracking-tight">
              Ekosistem Admin Kontrol İstasyonu
            </h1>
            <span className="text-[10px] font-bold uppercase px-2 py-0.5 rounded-full bg-rose-500/20 text-rose-300 border border-rose-500/30">
              TAM YETKİLİ
            </span>
          </div>
          <p className="text-xs text-gray-400 mt-1">
            FCM push bildirim konsolu, kullanıcı & premium yönetimi ve Neon DB telemetrisi
          </p>
        </div>

        <div className="flex items-center gap-3">
          {/* Secret Key Input if desired */}
          <div className="relative">
            <input 
              type="password"
              value={adminSecret}
              onChange={(e) => setAdminSecret(e.target.value)}
              placeholder="X-Admin-Secret (İsteğe bağlı)"
              className="bg-white/5 border border-white/10 rounded-xl px-3 py-1.5 text-xs text-white placeholder-gray-500 outline-none w-48 focus:border-amber-400"
            />
          </div>
        </div>
      </div>

      {/* Action Feedback Banner */}
      {userActionFeedback && (
        <div className={`p-4 rounded-2xl text-xs flex items-center justify-between ${
          userActionFeedback.ok ? 'bg-emerald-500/10 border border-emerald-500/20 text-emerald-300' : 'bg-rose-500/10 border border-rose-500/20 text-rose-300'
        }`}>
          <div className="flex items-center gap-2">
            {userActionFeedback.ok ? <CheckCircle2 className="w-4 h-4" /> : <AlertCircle className="w-4 h-4" />}
            <span>{userActionFeedback.message}</span>
          </div>
          <button onClick={() => setUserActionFeedback(null)} className="text-gray-400 hover:text-white">
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      )}

      {/* Tabs */}
      <div className="flex items-center gap-2 border-b border-white/10 pb-2">
        <button
          onClick={() => setActiveTab('push')}
          className={`px-4 py-2 rounded-xl text-xs font-semibold flex items-center gap-2 transition-all ${
            activeTab === 'push'
              ? 'bg-rose-500/20 text-rose-300 border border-rose-500/30 shadow-sm'
              : 'text-gray-400 hover:text-white hover:bg-white/5'
          }`}
        >
          <Send className="w-3.5 h-3.5" />
          <span>Push Bildirim İstasyonu (FCM)</span>
        </button>

        <button
          onClick={() => setActiveTab('users')}
          className={`px-4 py-2 rounded-xl text-xs font-semibold flex items-center gap-2 transition-all ${
            activeTab === 'users'
              ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30 shadow-sm'
              : 'text-gray-400 hover:text-white hover:bg-white/5'
          }`}
        >
          <Users className="w-3.5 h-3.5" />
          <span>Kullanıcı & Premium Yönetimi ({usersList.length})</span>
        </button>

        <button
          onClick={() => setActiveTab('system')}
          className={`px-4 py-2 rounded-xl text-xs font-semibold flex items-center gap-2 transition-all ${
            activeTab === 'system'
              ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 shadow-sm'
              : 'text-gray-400 hover:text-white hover:bg-white/5'
          }`}
        >
          <Database className="w-3.5 h-3.5" />
          <span>Sistem & Neon DB Telemetrisi</span>
        </button>

        <button
          onClick={() => setActiveTab('versions')}
          className={`px-4 py-2 rounded-xl text-xs font-semibold flex items-center gap-2 transition-all ${
            activeTab === 'versions'
              ? 'bg-indigo-500/20 text-indigo-300 border border-indigo-500/30 shadow-sm'
              : 'text-gray-400 hover:text-white hover:bg-white/5'
          }`}
        >
          <Smartphone className="w-3.5 h-3.5" />
          <span>Force Update & Versiyon</span>
        </button>
      </div>

      {/* TAB 1: PUSH NOTIFICATION CONSOLE */}
      {activeTab === 'push' && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          
          {/* Left Form: 7 cols */}
          <div className="lg:col-span-7 space-y-6">
            
            {/* Target Selector */}
            <div className="glass-panel rounded-3xl p-6 border border-white/10 space-y-5">
              <h3 className="text-xs font-bold uppercase tracking-wider text-rose-400 flex items-center gap-2">
                <Send className="w-3.5 h-3.5" />
                <span>1. Hedef Kitle Belirleme</span>
              </h3>

              <div className="grid grid-cols-2 gap-3">
                <button
                  type="button"
                  onClick={() => { setPushMode('BROADCAST'); setSelectedUser(null); }}
                  className={`p-3.5 rounded-2xl text-left border transition-all ${
                    pushMode === 'BROADCAST' 
                      ? 'bg-rose-500/15 border-rose-500/40 text-white shadow-glow-planora' 
                      : 'bg-white/[0.02] border-white/10 text-gray-400 hover:border-white/20'
                  }`}
                >
                  <span className="font-bold text-xs block text-white">📢 Genel Duyuru (Broadcast)</span>
                  <span className="text-[11px] text-gray-400 mt-0.5 block">Tüm kayıtlı cihazlara anlık iletilir</span>
                </button>

                <button
                  type="button"
                  onClick={() => setPushMode('SPECIFIC')}
                  className={`p-3.5 rounded-2xl text-left border transition-all ${
                    pushMode === 'SPECIFIC' 
                      ? 'bg-amber-500/15 border-amber-500/40 text-white shadow-glow-gold' 
                      : 'bg-white/[0.02] border-white/10 text-gray-400 hover:border-white/20'
                  }`}
                >
                  <span className="font-bold text-xs block text-white">🎯 Tekil Kullanıcı</span>
                  <span className="text-[11px] text-gray-400 mt-0.5 block">Kullanıcı adı veya e-posta ile hedefle</span>
                </button>
              </div>

              {/* Specific user search */}
              {pushMode === 'SPECIFIC' && (
                <div className="pt-2">
                  <label className="block text-[11px] font-semibold text-gray-400 mb-1.5 uppercase">
                    Kullanıcı Ara
                  </label>
                  <div className="relative">
                    <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
                    <input
                      type="text"
                      value={pushUserQuery}
                      onChange={(e) => searchPushTargets(e.target.value)}
                      placeholder="kullanıcı adı, e-posta veya isim yazın..."
                      className="w-full pl-9 pr-4 py-2.5 rounded-xl glass-input text-xs text-white"
                    />
                  </div>

                  {/* Search Results */}
                  {pushTargets.length > 0 && (
                    <div className="mt-2 p-2 rounded-xl bg-black/60 border border-white/10 max-h-40 overflow-y-auto space-y-1">
                      {pushTargets.map((target) => (
                        <div
                          key={target.id}
                          onClick={() => { setSelectedUser(target); setPushTargets([]); }}
                          className="p-2 rounded-lg hover:bg-white/10 cursor-pointer flex items-center justify-between text-xs text-white transition-colors"
                        >
                          <div>
                            <span className="font-semibold">{target.name || target.username}</span>
                            <span className="text-[10px] text-gray-400 block">{target.email}</span>
                          </div>
                          <span className="text-[10px] px-2 py-0.5 rounded bg-white/10 text-emerald-400">
                            {target.fcmDeviceCount} Cihaz
                          </span>
                        </div>
                      ))}
                    </div>
                  )}

                  {selectedUser && (
                    <div className="mt-2 p-2.5 rounded-xl bg-amber-500/10 border border-amber-500/20 text-xs text-amber-300 flex items-center justify-between">
                      <span>Seçilen Kullanıcı: <strong>@{selectedUser.username}</strong> ({selectedUser.email})</span>
                      <button onClick={() => setSelectedUser(null)} className="text-gray-400 hover:text-white">
                        <X className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  )}
                </div>
              )}

            </div>

            {/* Notification Content Form */}
            <form onSubmit={handleSendPush} className="glass-panel rounded-3xl p-6 border border-white/10 space-y-5">
              <h3 className="text-xs font-bold uppercase tracking-wider text-rose-400 flex items-center gap-2">
                <Bell className="w-3.5 h-3.5" />
                <span>2. Bildirim İçeriği & Başlık</span>
              </h3>

              {/* Ready Templates */}
              <div>
                <label className="block text-[11px] font-semibold text-gray-400 mb-2 uppercase">
                  Hızlı Şablonlar
                </label>
                <div className="flex flex-wrap gap-2">
                  <button
                    type="button"
                    onClick={() => applyTemplate('🥗 Öğününüzü Kaydettiniz mi?', 'Gününüzün besin değerlerini ve makrolarını HealthTrack ile saniyeler içinde analiz edin!')}
                    className="px-2.5 py-1 rounded-lg bg-white/5 hover:bg-white/10 border border-white/5 text-[11px] text-emerald-300 transition-all"
                  >
                    🍏 HealthTrack Öğün Hatırlatması
                  </button>
                  <button
                    type="button"
                    onClick={() => applyTemplate('⚡ Günün Planı Hazır!', 'Bugünkü görevlerinizi kontrol edin ve verimlilik zincirinizi kırmayın.')}
                    className="px-2.5 py-1 rounded-lg bg-white/5 hover:bg-white/10 border border-white/5 text-[11px] text-indigo-300 transition-all"
                  >
                    ⚡ Planora Görev Hatırlatması
                  </button>
                  <button
                    type="button"
                    onClick={() => applyTemplate('👑 Tebrikler: Premium Erişim Tanımlandı!', 'Hesabınıza sınırsız AI ve özel analiz özellikleri tanımlanmıştır. Keyfini çıkarın!')}
                    className="px-2.5 py-1 rounded-lg bg-white/5 hover:bg-white/10 border border-white/5 text-[11px] text-amber-300 transition-all"
                  >
                    👑 Premium Bildirimi
                  </button>
                </div>
              </div>

              <div>
                <div className="flex justify-between items-center mb-1">
                  <label className="text-[11px] font-semibold text-gray-400 uppercase">Başlık</label>
                  <span className="text-[10px] text-gray-500">{pushTitle.length}/50</span>
                </div>
                <input
                  type="text"
                  required
                  value={pushTitle}
                  onChange={(e) => setPushTitle(e.target.value)}
                  placeholder="Örn: 🥗 Sağlık Hedeflerinize Bir Adım Daha"
                  className="w-full px-3.5 py-2.5 rounded-xl glass-input text-xs text-white"
                />
              </div>

              <div>
                <div className="flex justify-between items-center mb-1">
                  <label className="text-[11px] font-semibold text-gray-400 uppercase">Mesaj Metni</label>
                  <span className="text-[10px] text-gray-500">{pushBody.length}/150</span>
                </div>
                <textarea
                  rows={3}
                  required
                  value={pushBody}
                  onChange={(e) => setPushBody(e.target.value)}
                  placeholder="Bildirim içeriğinizi buraya yazın..."
                  className="w-full px-3.5 py-2.5 rounded-xl glass-input text-xs text-white resize-none"
                />
              </div>

              {pushResult && (
                <div className={`p-3.5 rounded-xl text-xs flex items-center gap-2 ${
                  pushResult.ok ? 'bg-emerald-500/15 border border-emerald-500/30 text-emerald-300' : 'bg-rose-500/15 border border-rose-500/30 text-rose-300'
                }`}>
                  {pushResult.ok ? <CheckCircle2 className="w-4 h-4 shrink-0" /> : <AlertCircle className="w-4 h-4 shrink-0" />}
                  <span>{pushResult.message}</span>
                </div>
              )}

              <button
                type="submit"
                disabled={pushSending}
                className="w-full py-3 rounded-xl font-bold text-xs bg-gradient-to-r from-rose-500 via-pink-500 to-amber-500 text-white shadow-lg hover:shadow-glow-gold hover:opacity-95 transition-all flex items-center justify-center gap-2 disabled:opacity-50"
              >
                {pushSending ? (
                  <>
                    <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                    <span>FCM Servisine İletiliyor...</span>
                  </>
                ) : (
                  <>
                    <Send className="w-4 h-4" />
                    <span>{pushMode === 'BROADCAST' ? 'Tüm Cihazlara Anında Gönder (Broadcast)' : 'Hedef Kullanıcıya Gönder'}</span>
                  </>
                )}
              </button>
            </form>

          </div>

          {/* Right Phone Mockup: 5 cols */}
          <div className="lg:col-span-5 flex flex-col items-center justify-start">
            <div className="w-full max-w-xs text-center mb-3">
              <span className="text-[11px] font-bold uppercase text-gray-400 tracking-wider">
                Canlı Mobil Bildirim Önizlemesi
              </span>
            </div>

            {/* Smartphone Case */}
            <div className="w-full max-w-[300px] h-[550px] bg-[#121826] border-4 border-[#242D42] rounded-[42px] p-3 shadow-2xl relative flex flex-col justify-between overflow-hidden">
              {/* Dynamic Island / Notch */}
              <div className="w-24 h-4 bg-black rounded-full mx-auto mb-2" />

              {/* Lock Screen Clock */}
              <div className="text-center mt-6">
                <div className="text-4xl font-extralight text-white font-sans">
                  {new Date().toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' })}
                </div>
                <div className="text-[11px] text-gray-400 mt-1">
                  {new Date().toLocaleDateString('tr-TR', { weekday: 'long', day: 'numeric', month: 'long' })}
                </div>
              </div>

              {/* Notification Banner */}
              <div className="my-auto">
                <div className="glass-panel p-3.5 rounded-2xl border border-white/20 shadow-2xl bg-[#1E2738]/90 backdrop-blur-xl animate-fade-in">
                  <div className="flex items-center justify-between text-[10px] text-gray-400 mb-1.5">
                    <div className="flex items-center gap-1.5 font-bold text-white">
                      <div className="w-4 h-4 rounded bg-gradient-to-tr from-emerald-500 to-indigo-500 flex items-center justify-center text-[9px] text-white font-black">
                        H
                      </div>
                      <span>HealthTrack & Planora</span>
                    </div>
                    <span>şimdi</span>
                  </div>

                  <div className="font-bold text-xs text-white leading-snug">
                    {pushTitle || 'Örnek Bildirim Başlığı'}
                  </div>

                  <p className="text-[11px] text-gray-300 mt-0.5 leading-snug line-clamp-3">
                    {pushBody || 'Bildirim içeriği burada canlı olarak görüntülenecektir...'}
                  </p>
                </div>
              </div>

              {/* Bottom Home Indicator */}
              <div className="w-28 h-1 bg-white/20 rounded-full mx-auto mb-1" />
            </div>
          </div>

        </div>
      )}

      {/* TAB 2: USER & SUBSCRIPTION MANAGEMENT */}
      {activeTab === 'users' && (
        <div className="space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="relative w-full sm:w-80">
              <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-500" />
              <input
                type="text"
                value={usersQuery}
                onChange={(e) => { setUsersQuery(e.target.value); loadUsers(e.target.value); }}
                placeholder="İsim, kullanıcı adı veya e-posta ara..."
                className="w-full pl-10 pr-4 py-2.5 rounded-xl glass-input text-xs text-white"
              />
            </div>

            <button
              onClick={() => loadUsers(usersQuery)}
              disabled={loadingUsers}
              className="px-3.5 py-2 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-xs font-semibold text-gray-300 hover:text-white transition-all flex items-center gap-2 self-start sm:self-auto"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${loadingUsers ? 'animate-spin' : ''}`} />
              <span>Kullanıcıları Yenile</span>
            </button>
          </div>

          {/* Users Table */}
          <div className="glass-panel rounded-3xl border border-white/10 overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse text-xs">
                <thead>
                  <tr className="border-b border-white/10 bg-white/[0.02] text-gray-400 uppercase font-semibold">
                    <th className="py-3.5 px-4">Kullanıcı</th>
                    <th className="py-3.5 px-4">Rol</th>
                    <th className="py-3.5 px-4">Premium Durumu</th>
                    <th className="py-3.5 px-4">Görev & Cihaz</th>
                    <th className="py-3.5 px-4 text-right">Eylemler / Yetki</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5">
                  {usersList.map((u) => {
                    const isPrem = u.isPremium;
                    return (
                      <tr key={u.id} className="hover:bg-white/[0.02] transition-colors">
                        {/* User Identity */}
                        <td className="py-3.5 px-4">
                          <div className="flex items-center gap-2.5">
                            <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-indigo-500 to-purple-600 flex items-center justify-center font-bold text-white text-xs">
                              {(u.name || u.username || 'U')[0].toUpperCase()}
                            </div>
                            <div>
                              <div className="font-bold text-white text-sm">{u.name || u.username}</div>
                              <div className="text-[11px] text-gray-400">@{u.username} • {u.email}</div>
                            </div>
                          </div>
                        </td>

                        {/* Role */}
                        <td className="py-3.5 px-4">
                          <span className={`text-[10px] font-bold px-2 py-0.5 rounded uppercase ${
                            u.role === 'ADMIN' 
                              ? 'bg-rose-500/20 text-rose-300 border border-rose-500/30' 
                              : u.role === 'NOTIFIER' 
                              ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30'
                              : 'bg-white/10 text-gray-400'
                          }`}>
                            {u.role || 'USER'}
                          </span>
                        </td>

                        {/* Premium Status */}
                        <td className="py-3.5 px-4">
                          {isPrem ? (
                            <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-amber-500/15 border border-amber-500/30 text-amber-300 font-semibold text-[11px]">
                              <Crown className="w-3 h-3 text-amber-400" />
                              <span>{u.premiumUntil ? `${new Date(u.premiumUntil).toLocaleDateString('tr-TR')}` : 'Süresiz / Ömür Boyu'}</span>
                            </div>
                          ) : (
                            <span className="text-gray-500 text-[11px]">Ücretsiz Standart</span>
                          )}
                        </td>

                        {/* Stats */}
                        <td className="py-3.5 px-4 text-gray-300">
                          <div className="flex items-center gap-3">
                            <span title="Görev Sayısı">📋 {u.taskCount || 0}</span>
                            <span title="Kayıtlı FCM Cihaz">📱 {u.fcmDeviceCount || 0}</span>
                          </div>
                        </td>

                        {/* Actions */}
                        <td className="py-3.5 px-4 text-right">
                          <div className="flex items-center justify-end gap-1.5">
                            {/* Inspect Tasks */}
                            <button
                              onClick={() => handleInspectTasks(u)}
                              title="Görevlerini Görüntüle"
                              className="px-2 py-1 rounded-lg bg-white/5 hover:bg-white/10 border border-white/5 text-[11px] text-gray-300 hover:text-white transition-all"
                            >
                              Görevler
                            </button>

                            {/* Premium Quick Buttons */}
                            {!isPrem ? (
                              <button
                                onClick={() => handleSetPremium(u, 30, true)}
                                title="30 Günlük Premium Tanımla"
                                className="px-2 py-1 rounded-lg bg-amber-500/10 hover:bg-amber-500/20 border border-amber-500/20 text-[11px] text-amber-300 transition-all font-semibold"
                              >
                                +30G Premium
                              </button>
                            ) : (
                              <button
                                onClick={() => handleSetPremium(u, 0, false)}
                                title="Premium İptal Et"
                                className="px-2 py-1 rounded-lg bg-rose-500/10 hover:bg-rose-500/20 border border-rose-500/20 text-[11px] text-rose-300 transition-all"
                              >
                                İptal Et
                              </button>
                            )}

                            {/* Role Switcher */}
                            <button
                              onClick={() => handleUpdateRole(u, u.role === 'ADMIN' ? 'USER' : 'ADMIN')}
                              title={u.role === 'ADMIN' ? 'Admin Yetkisini Al' : 'Admin Yetkisi Ver'}
                              className={`p-1.5 rounded-lg border transition-all ${
                                u.role === 'ADMIN' 
                                  ? 'bg-rose-500/10 border-rose-500/30 text-rose-400 hover:bg-rose-500/20' 
                                  : 'bg-white/5 border-white/10 text-gray-400 hover:text-white'
                              }`}
                            >
                              <ShieldCheck className="w-3.5 h-3.5" />
                            </button>

                            {/* Delete User */}
                            <button
                              onClick={() => handleDeleteUser(u)}
                              title="Kullanıcıyı Kalıcı Sil"
                              className="p-1.5 rounded-lg bg-white/5 hover:bg-rose-500/20 border border-white/10 hover:border-rose-500/30 text-gray-400 hover:text-rose-400 transition-all"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>

          {/* User Tasks Inspection Modal */}
          {selectedUserTasks && (
            <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
              <div className="glass-panel w-full max-w-2xl rounded-3xl p-6 border border-white/20 shadow-2xl max-h-[85vh] flex flex-col justify-between">
                <div>
                  <div className="flex items-center justify-between border-b border-white/10 pb-4 mb-4">
                    <div>
                      <h3 className="text-base font-bold text-white">
                        @{selectedUserTasks.username} Kullanıcısının Görevleri
                      </h3>
                      <p className="text-xs text-gray-400">Toplam {selectedUserTasks.tasks?.length || 0} görev bulundu</p>
                    </div>
                    <button onClick={() => setSelectedUserTasks(null)} className="p-1 text-gray-400 hover:text-white">
                      <X className="w-5 h-5" />
                    </button>
                  </div>

                  <div className="overflow-y-auto max-h-[50vh] space-y-2 pr-1">
                    {selectedUserTasks.tasks?.length === 0 ? (
                      <p className="text-xs text-gray-500 text-center py-6">Kullanıcının kayıtlı görevi yok.</p>
                    ) : (
                      selectedUserTasks.tasks?.map((t) => (
                        <div key={t.id} className="p-3 rounded-xl bg-white/[0.02] border border-white/5 flex items-center justify-between text-xs">
                          <div>
                            <span className="font-bold text-white block">{t.title}</span>
                            {t.description && <span className="text-[11px] text-gray-400 line-clamp-1">{t.description}</span>}
                          </div>
                          <span className={`px-2 py-0.5 rounded font-bold uppercase text-[10px] ${
                            t.status === 'COMPLETED' ? 'bg-emerald-500/15 text-emerald-300' : 'bg-amber-500/15 text-amber-300'
                          }`}>
                            {t.status}
                          </span>
                        </div>
                      ))
                    )}
                  </div>
                </div>

                <div className="pt-4 border-t border-white/10 text-right">
                  <button
                    onClick={() => setSelectedUserTasks(null)}
                    className="px-4 py-2 rounded-xl bg-white/10 hover:bg-white/20 text-xs font-semibold text-white transition-colors"
                  >
                    Kapat
                  </button>
                </div>
              </div>
            </div>
          )}

        </div>
      )}

      {/* TAB 3: SYSTEM & NEON DB STATS */}
      {activeTab === 'system' && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
            
            <div className="glass-panel rounded-3xl p-6 border border-emerald-500/20">
              <span className="text-xs font-bold uppercase tracking-wider text-emerald-400 block mb-1">
                Toplam Kullanıcılar
              </span>
              <div className="text-3xl font-black text-white">{stats?.totalUsers ?? '--'}</div>
              <p className="text-[11px] text-gray-400 mt-2">Neon DB users tablosu kayıtları</p>
            </div>

            <div className="glass-panel rounded-3xl p-6 border border-amber-500/20">
              <span className="text-xs font-bold uppercase tracking-wider text-amber-400 block mb-1">
                Premium Kullanıcı Sayısı
              </span>
              <div className="text-3xl font-black text-white">{stats?.totalPremiumUsers ?? '--'}</div>
              <p className="text-[11px] text-gray-400 mt-2">Aktif lisanslı/ücretli üyeler</p>
            </div>

            <div className="glass-panel rounded-3xl p-6 border border-indigo-500/20">
              <span className="text-xs font-bold uppercase tracking-wider text-indigo-400 block mb-1">
                Aktif FCM Cihazları
              </span>
              <div className="text-3xl font-black text-white">{stats?.totalFcmDevices ?? '--'}</div>
              <p className="text-[11px] text-gray-400 mt-2">Push bildirim alabilen cihazlar</p>
            </div>

            <div className="glass-panel rounded-3xl p-6 border border-white/10">
              <span className="text-xs font-bold uppercase tracking-wider text-gray-400 block mb-1">
                Toplam Görevler (Tasks)
              </span>
              <div className="text-3xl font-black text-white">{stats?.totalTasks ?? '--'}</div>
              <p className="text-[11px] text-gray-400 mt-2">Planora görev tablosu</p>
            </div>

            <div className="glass-panel rounded-3xl p-6 border border-white/10">
              <span className="text-xs font-bold uppercase tracking-wider text-gray-400 block mb-1">
                Plan Odaları (Rooms)
              </span>
              <div className="text-3xl font-black text-white">{stats?.totalRooms ?? '--'}</div>
              <p className="text-[11px] text-gray-400 mt-2">Grup ve çalışma odaları</p>
            </div>

            <div className="glass-panel rounded-3xl p-6 border border-white/10">
              <span className="text-xs font-bold uppercase tracking-wider text-gray-400 block mb-1">
                Topluluk Gönderileri (Posts)
              </span>
              <div className="text-3xl font-black text-white">{stats?.totalPosts ?? '--'}</div>
              <p className="text-[11px] text-gray-400 mt-2">Sosyal akış paylaşımları</p>
            </div>

          </div>

          {/* Database & Render Health Box */}
          <div className="glass-panel rounded-3xl p-6 border border-white/10 space-y-4">
            <h3 className="text-sm font-bold text-white flex items-center gap-2">
              <Database className="w-4 h-4 text-emerald-400" />
              <span>Neon PostgreSQL & Render Altyapı Durumu</span>
            </h3>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
              <div className="p-4 rounded-2xl bg-white/[0.02] border border-white/5 space-y-1">
                <span className="text-gray-400">Veritabanı Sağlayıcısı:</span>
                <p className="font-bold text-white">Neon Serverless PostgreSQL (AWS Frankfurt / eu-central-1)</p>
                <p className="text-[11px] text-emerald-400">SSL Mode: Require | HikariCP Havuzu Aktif</p>
              </div>

              <div className="p-4 rounded-2xl bg-white/[0.02] border border-white/5 space-y-1">
                <span className="text-gray-400">Sunucu Hosting Servisi:</span>
                <p className="font-bold text-white">Render.com Web Service (Ktor JVM Engine)</p>
                <p className="text-[11px] text-indigo-400">Uptime Robot 5 dk aralıklı canlandırma aktif</p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* TAB 4: FORCE UPDATE & VERSION MANAGEMENT */}
      {activeTab === 'versions' && (
        <div className="space-y-6">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-lg font-bold text-white flex items-center gap-2">
                <Smartphone className="w-5 h-5 text-indigo-400" />
                <span>Force Update & Uzaktan Versiyon Yönetimi</span>
              </h2>
              <p className="text-xs text-gray-400 mt-1">
                Minimum desteklenen sürümü yükselterek eski sürümleri zorunlu güncellemeye yönlendirebilirsiniz.
              </p>
            </div>
            <button
              onClick={loadAppConfigs}
              disabled={loadingConfigs}
              className="px-3 py-2 rounded-xl bg-white/5 hover:bg-white/10 text-xs font-semibold text-gray-300 flex items-center gap-1.5 border border-white/10"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${loadingConfigs ? 'animate-spin' : ''}`} />
              <span>Yenile</span>
            </button>
          </div>

          {configFeedback && (
            <div
              className={`p-3.5 rounded-2xl border text-xs flex items-center gap-2 ${
                configFeedback.type === 'success'
                  ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-300'
                  : 'bg-rose-500/10 border-rose-500/30 text-rose-300'
              }`}
            >
              {configFeedback.type === 'success' ? (
                <CheckCircle2 className="w-4 h-4 shrink-0" />
              ) : (
                <AlertCircle className="w-4 h-4 shrink-0" />
              )}
              <span>{configFeedback.message}</span>
            </div>
          )}

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {appConfigs.map((cfg) => {
              const isHealth = cfg.appId === 'healthtrack';
              return (
                <div
                  key={cfg.appId}
                  className="glass-panel rounded-3xl p-6 border border-white/10 space-y-4"
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2.5">
                      <span
                        className={`px-2.5 py-1 rounded-lg text-xs font-bold ${
                          isHealth
                            ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                            : 'bg-indigo-500/20 text-indigo-300 border border-indigo-500/30'
                        }`}
                      >
                        {isHealth ? 'HealthTrack' : 'Planora'}
                      </span>
                      <span className="text-xs text-gray-500 font-mono">
                        /api/app-config?app={cfg.appId}
                      </span>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block text-[11px] font-semibold text-gray-400 mb-1">
                        Minimum Zorunlu Sürüm
                      </label>
                      <input
                        type="text"
                        value={cfg.minSupportedVersion || ''}
                        onChange={(e) =>
                          handleAppConfigFieldChange(cfg.appId, 'minSupportedVersion', e.target.value)
                        }
                        placeholder="1.0.0"
                        className="w-full px-3 py-2 rounded-xl bg-white/5 border border-white/10 text-xs text-white font-mono"
                      />
                    </div>
                    <div>
                      <label className="block text-[11px] font-semibold text-gray-400 mb-1">
                        En Güncel Sürüm (Opsiyonel Uyarı)
                      </label>
                      <input
                        type="text"
                        value={cfg.latestVersion || ''}
                        onChange={(e) =>
                          handleAppConfigFieldChange(cfg.appId, 'latestVersion', e.target.value)
                        }
                        placeholder="1.1.0"
                        className="w-full px-3 py-2 rounded-xl bg-white/5 border border-white/10 text-xs text-white font-mono"
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <div>
                      <label className="block text-[11px] font-semibold text-gray-400 mb-1">
                        Android Store URL
                      </label>
                      <input
                        type="text"
                        value={cfg.storeUrlAndroid || ''}
                        onChange={(e) =>
                          handleAppConfigFieldChange(cfg.appId, 'storeUrlAndroid', e.target.value)
                        }
                        placeholder="https://play.google.com/store/apps/details?id=..."
                        className="w-full px-3 py-2 rounded-xl bg-white/5 border border-white/10 text-xs text-white"
                      />
                    </div>
                    <div>
                      <label className="block text-[11px] font-semibold text-gray-400 mb-1">
                        iOS App Store URL
                      </label>
                      <input
                        type="text"
                        value={cfg.storeUrlIos || ''}
                        onChange={(e) =>
                          handleAppConfigFieldChange(cfg.appId, 'storeUrlIos', e.target.value)
                        }
                        placeholder="https://apps.apple.com/app/..."
                        className="w-full px-3 py-2 rounded-xl bg-white/5 border border-white/10 text-xs text-white"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-[11px] font-semibold text-gray-400 mb-1">
                      Zorunlu Güncelleme Başlığı (TR)
                    </label>
                    <input
                      type="text"
                      value={cfg.forceUpdateTitleTr || ''}
                      onChange={(e) =>
                        handleAppConfigFieldChange(cfg.appId, 'forceUpdateTitleTr', e.target.value)
                      }
                      className="w-full px-3 py-2 rounded-xl bg-white/5 border border-white/10 text-xs text-white"
                    />
                  </div>

                  <div>
                    <label className="block text-[11px] font-semibold text-gray-400 mb-1">
                      Zorunlu Güncelleme Mesajı (TR)
                    </label>
                    <textarea
                      rows={2}
                      value={cfg.forceUpdateMessageTr || ''}
                      onChange={(e) =>
                        handleAppConfigFieldChange(cfg.appId, 'forceUpdateMessageTr', e.target.value)
                      }
                      className="w-full px-3 py-2 rounded-xl bg-white/5 border border-white/10 text-xs text-white"
                    />
                  </div>

                  <button
                    onClick={() => handleSaveAppConfig(cfg)}
                    disabled={savingConfigAppId === cfg.appId}
                    className="w-full py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white text-xs font-bold transition-colors"
                  >
                    {savingConfigAppId === cfg.appId ? 'Kaydediliyor...' : 'Ayarları Kaydet'}
                  </button>
                </div>
              );
            })}
          </div>
        </div>
      )}

    </div>
  );
}

