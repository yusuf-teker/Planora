import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { User, Key, ArrowRight, AlertCircle } from 'lucide-react';

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
        msg = 'Render sunucusuna ulaşılamadı. Sunucu uyku modundan uyanıyor olabilir (30-40 sn sürebilir), lütfen birazdan tekrar deneyin.';
      }
      setError(msg);
    } finally {
      setLoading(false);
    }
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
          <div className="flex flex-col items-center text-center mb-6">
            {/* Logolar yan yana ve aralıklı */}
            <div className="flex items-center gap-4 mb-5">
              {/* HealthTrack Pod */}
              <div className="w-14 h-14 rounded-full bg-gradient-to-tr from-emerald-600 to-teal-400 p-[2px] shadow-glow-health">
                <div className="w-full h-full bg-[#0B0F19] rounded-full flex items-center justify-center font-black text-emerald-400 text-xl tracking-tighter">
                  H
                </div>
              </div>
              {/* Planora Pod */}
              <div className="w-14 h-14 rounded-full bg-gradient-to-tr from-indigo-600 to-violet-400 p-[2px] shadow-glow-planora">
                <div className="w-full h-full bg-[#0B0F19] rounded-full flex items-center justify-center font-black text-indigo-400 text-xl tracking-tighter">
                  P
                </div>
              </div>
            </div>

            <h1 className="text-2xl font-extrabold tracking-tight text-white">
              HealthTrack & Planora
            </h1>
          </div>

          {/* Bilgilendirme Notu */}
          <div className="mb-6 p-3 rounded-2xl bg-white/[0.03] border border-white/5 text-gray-400 text-xs text-center leading-relaxed">
            Hem normal kullanıcılar (rapor ve verilerini görüntülemek için) hem de yöneticiler aynı giriş ekranını kullanır.
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
                  <span>Giriş Yap</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          <div className="mt-6 pt-5 border-t border-white/10 flex flex-wrap items-center justify-center gap-4 text-[11px] text-gray-400">
            <a
              href="/privacy"
              onClick={(e) => {
                if (onNavigateLegal) { e.preventDefault(); onNavigateLegal('/privacy'); }
              }}
              className="hover:text-emerald-400 transition-colors"
            >
              Gizlilik Politikası
            </a>
            <span className="text-gray-600">•</span>
            <a
              href="/terms"
              onClick={(e) => {
                if (onNavigateLegal) { e.preventDefault(); onNavigateLegal('/terms'); }
              }}
              className="hover:text-emerald-400 transition-colors"
            >
              Kullanım Koşulları
            </a>
            <span className="text-gray-600">•</span>
            <a
              href="/delete-account"
              onClick={(e) => {
                if (onNavigateLegal) { e.preventDefault(); onNavigateLegal('/delete-account'); }
              }}
              className="hover:text-rose-400 transition-colors"
            >
              Hesap Silme
            </a>
          </div>

        </div>

      </div>
    </div>
  );
}
