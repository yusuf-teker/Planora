import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import {
  User,
  Key,
  ArrowRight,
  AlertCircle,
  Heart,
  CheckSquare,
  Sparkles,
} from 'lucide-react';

export default function LoginView({ onNavigateLegal }) {
  const { login } = useAuth();
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

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
        msg =
          'Sunucuya ulaşılamadı. Uyku modundan uyanıyor olabilir (20-30 sn), lütfen tekrar deneyin.';
      }
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col justify-between bg-[#0B0F19] text-[#F8FAFC] relative overflow-hidden">
      {/* Subtle Ambient Glows */}
      <div className="absolute -top-24 -left-24 w-96 h-96 bg-emerald-500/10 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute -bottom-24 -right-24 w-96 h-96 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none" />

      <div className="flex-1 flex items-center justify-center px-4 sm:px-6 py-10 relative z-10">
        <div className="w-full max-w-5xl grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-12 items-center">
          {/* Left: Unified Ecosystem Intro */}
          <div className="lg:col-span-6 space-y-6 text-center lg:text-left">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/[0.04] border border-white/10 text-xs text-gray-300">
              <Sparkles className="w-3.5 h-3.5 text-emerald-400" />
              <span>Tek Hesap • Çift Ekosistem</span>
            </div>

            <div className="space-y-3">
              <h1 className="text-3xl sm:text-4xl lg:text-5xl font-extrabold tracking-tight text-white leading-tight">
                Sağlık ve Planlamanız{' '}
                <span className="bg-gradient-to-r from-emerald-400 via-teal-300 to-indigo-400 bg-clip-text text-transparent">
                  Tek Merkezde.
                </span>
              </h1>
              <p className="text-sm sm:text-base text-gray-400 max-w-lg mx-auto lg:mx-0 leading-relaxed">
                Mobil uygulamanızla anlık senkronize çalışan web portalı üzerinden öğünlerinizi AI ile analiz edin, görevlerinizi yönetin ve sağlık raporlarınızı indirin.
              </p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5 pt-2">
              <div className="glass-panel rounded-2xl p-4 border border-emerald-500/20 text-left">
                <div className="flex items-center gap-2.5 mb-2">
                  <div className="w-8 h-8 rounded-xl bg-emerald-500/15 flex items-center justify-center text-emerald-400">
                    <Heart className="w-4 h-4" />
                  </div>
                  <span className="font-bold text-sm text-white">HealthTrack</span>
                </div>
                <p className="text-xs text-gray-400 leading-relaxed">
                  AI destekli kalori & makro takibi, antrenman kaydı, kilo grafiği ve doktor raporu.
                </p>
              </div>

              <div className="glass-panel rounded-2xl p-4 border border-indigo-500/20 text-left">
                <div className="flex items-center gap-2.5 mb-2">
                  <div className="w-8 h-8 rounded-xl bg-indigo-500/15 flex items-center justify-center text-indigo-400">
                    <CheckSquare className="w-4 h-4" />
                  </div>
                  <span className="font-bold text-sm text-white">Planora</span>
                </div>
                <p className="text-xs text-gray-400 leading-relaxed">
                  Günlük görevler, etkinlikler, alışkanlıklar ve anlık tamamlanma takibi.
                </p>
              </div>
            </div>
          </div>

          {/* Right: Clean Login Card */}
          <div className="lg:col-span-6 w-full max-w-md mx-auto">
            <div className="glass-panel rounded-3xl p-7 sm:p-9 shadow-2xl border border-white/10">
              <div className="flex items-center justify-center gap-3 mb-6">
                <div className="w-11 h-11 rounded-full bg-gradient-to-tr from-emerald-500 to-teal-400 p-[1.5px]">
                  <div className="w-full h-full bg-[#0B0F19] rounded-full flex items-center justify-center font-black text-emerald-400 text-base">
                    H
                  </div>
                </div>
                <div className="w-11 h-11 rounded-full bg-gradient-to-tr from-indigo-500 to-violet-400 p-[1.5px]">
                  <div className="w-full h-full bg-[#0B0F19] rounded-full flex items-center justify-center font-black text-indigo-400 text-base">
                    P
                  </div>
                </div>
              </div>

              <div className="text-center mb-6">
                <h2 className="text-xl font-bold text-white">Hesabınıza Giriş Yapın</h2>
                <p className="text-xs text-gray-400 mt-1">
                  Planora veya HealthTrack bilgilerinizle devam edin
                </p>
              </div>

              {error && (
                <div className="mb-5 p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/25 text-rose-300 text-xs flex items-start gap-2.5">
                  <AlertCircle className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
                  <div className="flex-1 leading-relaxed">{error}</div>
                </div>
              )}

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
                      placeholder="ornek@planora.com"
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
                  className="w-full mt-2 py-3.5 px-4 rounded-xl font-semibold text-sm bg-gradient-to-r from-emerald-500 via-teal-500 to-indigo-600 text-white shadow-lg hover:opacity-95 active:scale-[0.99] transition-all flex items-center justify-center gap-2 disabled:opacity-50"
                >
                  {loading ? (
                    <>
                      <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                      <span>Giriş Yapılıyor...</span>
                    </>
                  ) : (
                    <>
                      <span>Giriş Yap</span>
                      <ArrowRight className="w-4 h-4" />
                    </>
                  )}
                </button>
              </form>
            </div>
          </div>
        </div>
      </div>

      {/* Minimal Legal Footer */}
      <footer className="relative z-10 border-t border-white/5 py-5 px-4 text-center">
        <div className="max-w-5xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-2 text-[11px] text-gray-500">
          <span>HealthTrack & Planora © 2026</span>
          <div className="flex items-center gap-4 text-gray-400">
            <a
              href="/privacy"
              onClick={(e) => {
                if (onNavigateLegal) {
                  e.preventDefault();
                  onNavigateLegal('/privacy');
                }
              }}
              className="hover:text-emerald-400 transition-colors"
            >
              Gizlilik Politikası
            </a>
            <a
              href="/terms"
              onClick={(e) => {
                if (onNavigateLegal) {
                  e.preventDefault();
                  onNavigateLegal('/terms');
                }
              }}
              className="hover:text-emerald-400 transition-colors"
            >
              Kullanım Koşulları
            </a>
            <a
              href="/delete-account"
              onClick={(e) => {
                if (onNavigateLegal) {
                  e.preventDefault();
                  onNavigateLegal('/delete-account');
                }
              }}
              className="hover:text-rose-400 transition-colors"
            >
              Hesap Silme
            </a>
          </div>
        </div>
      </footer>
    </div>
  );
}
