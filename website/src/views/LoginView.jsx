import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { getBaseUrl, setCustomBaseUrl } from '../api/client';
import { ShieldCheck, Heart, CheckSquare, Key, User, ArrowRight, Server, AlertCircle, Sparkles } from 'lucide-react';

export default function LoginView() {
  const { login } = useAuth();
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [showServerConfig, setShowServerConfig] = useState(false);
  const [customUrl, setCustomUrl] = useState(getBaseUrl());

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!identifier.trim() || !password) {
      setError('Lütfen kullanıcı adı / e-posta ve şifrenizi girin.');
      return;
    }

    setError('');
    setLoading(true);

    try {
      await login(identifier.trim(), password);
    } catch (err) {
      console.error('Login error:', err);
      let msg = err.message || 'Giriş yapılamadı.';
      if (msg.includes('Failed to fetch') || msg.includes('NetworkError')) {
        msg = 'Render sunucusuna ulaşılamadı. Sunucu uyku modundan uyanıyor olabilir (30-40 sn sürebilir), lütfen birazdan tekrar deneyin.';
      }
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleSaveCustomUrl = (e) => {
    e.preventDefault();
    setCustomBaseUrl(customUrl);
    alert('Sunucu adresi güncellendi.');
    setShowServerConfig(false);
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4 relative overflow-hidden bg-[#0B0F19]">
      {/* Background Decorative Ambient Glows */}
      <div className="absolute top-1/4 -left-20 w-96 h-96 bg-emerald-500/10 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-1/4 -right-20 w-96 h-96 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none" />

      <div className="w-full max-w-md relative z-10">
        
        {/* Main Glass Card */}
        <div className="glass-panel rounded-3xl p-8 sm:p-10 shadow-2xl border border-white/10 relative">
          
          {/* Brand Header */}
          <div className="flex flex-col items-center text-center mb-8">
            <div className="flex items-center -space-x-3 mb-5">
              {/* HealthTrack Pod */}
              <div className="w-14 h-14 rounded-2xl bg-gradient-to-tr from-emerald-600 to-teal-400 p-[1.5px] shadow-glow-health">
                <div className="w-full h-full bg-[#0B0F19] rounded-[14px] flex items-center justify-center font-black text-emerald-400 text-xl tracking-tighter">
                  H
                </div>
              </div>
              {/* Planora Pod */}
              <div className="w-14 h-14 rounded-2xl bg-gradient-to-tr from-indigo-600 to-violet-400 p-[1.5px] shadow-glow-planora">
                <div className="w-full h-full bg-[#0B0F19] rounded-[14px] flex items-center justify-center font-black text-indigo-400 text-xl tracking-tighter">
                  P
                </div>
              </div>
            </div>

            <h1 className="text-2xl font-extrabold tracking-tight text-white">
              HealthTrack <span className="text-gray-500">&</span> Planora
            </h1>
            <p className="text-xs text-gray-400 mt-1 max-w-xs">
              Merkezi Sağlık Raporları, Görevler ve Sistem Yönetim Portalı
            </p>
          </div>

          {/* Error Banner */}
          {error && (
            <div className="mb-6 p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-300 text-xs flex items-start gap-2.5">
              <AlertCircle className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
              <div className="flex-1 leading-relaxed">{error}</div>
            </div>
          )}

          {/* Login Form */}
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-[11px] font-semibold uppercase tracking-wider text-gray-400 mb-1.5">
                Kullanıcı Adı veya E-posta
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-gray-500">
                  <User className="w-4 h-4" />
                </div>
                <input
                  type="text"
                  required
                  value={identifier}
                  onChange={(e) => setIdentifier(e.target.value)}
                  placeholder="ornek@planora.com veya kullanici"
                  className="w-full pl-10 pr-4 py-3 rounded-xl glass-input text-sm text-white placeholder-gray-600 transition-all"
                />
              </div>
            </div>

            <div>
              <label className="block text-[11px] font-semibold uppercase tracking-wider text-gray-400 mb-1.5">
                Şifre
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-gray-500">
                  <Key className="w-4 h-4" />
                </div>
                <input
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••••••"
                  className="w-full pl-10 pr-4 py-3 rounded-xl glass-input text-sm text-white placeholder-gray-600 transition-all"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full mt-2 py-3.5 px-4 rounded-xl font-semibold text-sm bg-gradient-to-r from-emerald-500 via-teal-500 to-indigo-600 text-white shadow-lg hover:shadow-glow-health hover:opacity-95 active:scale-[0.99] transition-all flex items-center justify-center gap-2 disabled:opacity-50"
            >
              {loading ? (
                <>
                  <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  <span>Giriş Yapılıyor...</span>
                </>
              ) : (
                <>
                  <span>Portala Giriş Yap</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          {/* Ecosystem note */}
          <div className="mt-6 pt-5 border-t border-white/5 text-center">
            <p className="text-[11px] text-gray-400">
              💡 Hem normal kullanıcılar (rapor ve verilerini görüntülemek için) hem de yöneticiler aynı giriş ekranını kullanır.
            </p>
          </div>

          {/* Advanced Server Configuration Toggle */}
          <div className="mt-4 text-center">
            <button
              type="button"
              onClick={() => setShowServerConfig(!showServerConfig)}
              className="text-[10px] text-gray-500 hover:text-gray-300 transition-colors inline-flex items-center gap-1"
            >
              <Server className="w-3 h-3" />
              <span>{showServerConfig ? 'Sunucu Ayarlarını Gizle' : 'Özel Sunucu / API Bağlantısı'}</span>
            </button>

            {showServerConfig && (
              <form onSubmit={handleSaveCustomUrl} className="mt-3 text-left p-3 rounded-xl bg-black/40 border border-white/5 space-y-2">
                <label className="block text-[10px] text-gray-400 uppercase font-semibold">
                  Backend API URL
                </label>
                <input
                  type="text"
                  value={customUrl}
                  onChange={(e) => setCustomUrl(e.target.value)}
                  placeholder="https://pulse-7b4z.onrender.com"
                  className="w-full px-2.5 py-1.5 rounded-lg bg-white/5 border border-white/10 text-xs text-white"
                />
                <button
                  type="submit"
                  className="w-full py-1.5 rounded-lg bg-white/10 hover:bg-white/20 text-xs font-medium text-white transition-colors"
                >
                  Kaydet & Yenile
                </button>
              </form>
            )}
          </div>

        </div>

        {/* Footer info */}
        <p className="text-center text-[11px] text-gray-500 mt-6">
          Render Cloud & Neon PostgreSQL ile 7/24 Senkronize
        </p>

      </div>
    </div>
  );
}
