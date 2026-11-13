import React, { useState } from 'react';
import { Trash2, ArrowLeft, AlertTriangle, CheckCircle2, Lock, User } from 'lucide-react';
import { login, deleteAccount } from '../api/client';

export default function DeleteAccountView({ onBack }) {
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [confirmed, setConfirmed] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!identifier.trim() || !password) {
      setError('Lütfen kullanıcı adı/e-posta ve şifrenizi girin.');
      return;
    }
    if (!confirmed) {
      setError('Devam etmek için kalıcı silme onay kutusunu işaretlemelisiniz.');
      return;
    }

    setLoading(true);
    try {
      const authData = await login(identifier.trim(), password);
      const token = authData?.token || authData?.accessToken;
      if (!token) {
        throw new Error('Kimlik doğrulama jetonu alınamadı.');
      }
      await deleteAccount(token);
      try {
        localStorage.removeItem('ht_token');
        localStorage.removeItem('ht_user');
      } catch (_) {}
      setSuccess(true);
    } catch (err) {
      setError(err.message || 'Hesap silme işlemi sırasında bir hata oluştu.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#0B0F19] text-[#F8FAFC] py-10 px-4 sm:px-6">
      <div className="max-w-xl mx-auto space-y-6">
        <button
          onClick={onBack}
          className="inline-flex items-center gap-2 text-xs font-semibold text-emerald-400 hover:text-emerald-300 bg-emerald-500/10 border border-emerald-500/20 px-3.5 py-2 rounded-xl transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          Portala Dön
        </button>

        <div className="glass-panel rounded-3xl p-6 sm:p-8 border border-white/10 space-y-6">
          <div className="border-b border-white/10 pb-5">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs font-bold mb-3">
              <Trash2 className="w-3.5 h-3.5" />
              Hesap ve Veri Silme
            </div>
            <h1 className="text-2xl font-black text-white tracking-tight">
              Planora & HealthTrack Hesabını Sil
            </h1>
            <p className="text-xs text-gray-400 mt-1.5">
              Planora ve HealthTrack aynı ortak hesabı kullanır. Hesabınızı sildiğinizde her iki uygulamadaki verileriniz de kalıcı olarak silinir.
            </p>
          </div>

          {success ? (
            <div className="p-5 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 space-y-3">
              <div className="flex items-center gap-2 text-emerald-400 font-bold text-sm">
                <CheckCircle2 className="w-5 h-5" />
                Hesabınız Kalıcı Olarak Silindi
              </div>
              <p className="text-xs text-gray-300 leading-relaxed">
                Hesabınız ve ilişkili tüm Planora &amp; HealthTrack verileriniz sistemlerimizden tamamen kaldırılmıştır. Bizi tercih ettiğiniz için teşekkür ederiz.
              </p>
            </div>
          ) : (
            <>
              <div className="p-4 rounded-2xl bg-rose-500/10 border border-rose-500/20 text-xs text-rose-200 space-y-2">
                <div className="flex items-center gap-2 font-bold text-rose-400">
                  <AlertTriangle className="w-4 h-4 shrink-0" />
                  Kalıcı Olarak Silinecek Veriler:
                </div>
                <ul className="list-disc pl-5 space-y-1 text-rose-200/80">
                  <li>Profil bilgileriniz, oturum jetonlarınız ve bildirim kayıtlarınız</li>
                  <li>HealthTrack beslenme, antrenman, kilo, hedef ve doktor randevu kayıtlarınız</li>
                  <li>Planora görevleriniz, notlarınız, paylaşımlarınız ve oda üyelikleriniz</li>
                </ul>
                <p className="text-[11px] text-rose-300/80 pt-1">
                  İşlem anında gerçekleştirilir ve geri alınamaz. Uygulama içinden silmek için: Planora &rarr; <strong>Ayarlar &rarr; Hesabı Sil</strong> veya HealthTrack &rarr; <strong>Profil &rarr; Hesabı Sil</strong>.
                </p>
              </div>

              {error && (
                <div className="p-3.5 rounded-xl bg-rose-500/15 border border-rose-500/30 text-xs text-rose-300">
                  {error}
                </div>
              )}

              <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-gray-300 mb-1.5">
                    Kullanıcı Adı veya E-posta
                  </label>
                  <div className="relative">
                    <User className="w-4 h-4 text-gray-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
                    <input
                      type="text"
                      value={identifier}
                      onChange={(e) => setIdentifier(e.target.value)}
                      placeholder="kullaniciadi veya ornek@mail.com"
                      className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-sm text-white placeholder-gray-500 focus:outline-none focus:border-rose-500/50"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-gray-300 mb-1.5">
                    Şifre
                  </label>
                  <div className="relative">
                    <Lock className="w-4 h-4 text-gray-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
                    <input
                      type="password"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      placeholder="••••••••"
                      className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-sm text-white placeholder-gray-500 focus:outline-none focus:border-rose-500/50"
                    />
                  </div>
                </div>

                <label className="flex items-start gap-2.5 text-xs text-gray-300 cursor-pointer select-none pt-1">
                  <input
                    type="checkbox"
                    checked={confirmed}
                    onChange={(e) => setConfirmed(e.target.checked)}
                    className="mt-0.5 rounded border-white/20 bg-white/5 text-rose-500 focus:ring-rose-500"
                  />
                  <span>
                    Hesabımın ve tüm Planora &amp; HealthTrack verilerimin geri döndürülemez şekilde silineceğini onaylıyorum.
                  </span>
                </label>

                <button
                  type="submit"
                  disabled={loading}
                  className="w-full py-3 rounded-xl bg-rose-600 hover:bg-rose-500 disabled:opacity-50 text-white text-sm font-bold transition-colors flex items-center justify-center gap-2"
                >
                  <Trash2 className="w-4 h-4" />
                  {loading ? 'Hesap Siliniyor...' : 'Hesabımı Kalıcı Olarak Sil'}
                </button>
              </form>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
