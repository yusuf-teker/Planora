import React from 'react';
import { FileText, ArrowLeft, AlertTriangle, CheckCircle2 } from 'lucide-react';

export default function TermsOfServiceView({ onBack }) {
  return (
    <div className="min-h-screen bg-[#0B0F19] text-[#F8FAFC] py-10 px-4 sm:px-6">
      <div className="max-w-3xl mx-auto space-y-6">
        <button
          onClick={onBack}
          className="inline-flex items-center gap-2 text-xs font-semibold text-emerald-400 hover:text-emerald-300 bg-emerald-500/10 border border-emerald-500/20 px-3.5 py-2 rounded-xl transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          Portala Dön
        </button>

        <div className="glass-panel rounded-3xl p-6 sm:p-10 border border-white/10 space-y-8">
          <div className="border-b border-white/10 pb-6">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 border border-indigo-500/20 text-indigo-400 text-xs font-bold mb-3">
              <FileText className="w-3.5 h-3.5" />
              Kullanım Koşulları
            </div>
            <h1 className="text-2xl sm:text-3xl font-black text-white tracking-tight">
              Planora & HealthTrack Kullanım Koşulları
            </h1>
            <p className="text-xs text-gray-400 mt-2">
              Son Güncelleme: 13 Kasım 2026 • Geliştirici: Yusuf Teker
            </p>
          </div>

          <section className="space-y-3 text-sm text-gray-300 leading-relaxed">
            <h2 className="text-base font-bold text-white flex items-center gap-2">
              <AlertTriangle className="w-4 h-4 text-amber-400" />
              1. Tıbbi Sorumluluk Reddi (HealthTrack)
            </h2>
            <div className="p-4 rounded-2xl bg-amber-500/10 border border-amber-500/20 text-xs text-amber-200 space-y-1.5">
              <p className="font-bold text-amber-400">Önemli Uyarı: Tıbbi Tavsiye Değildir</p>
              <p>
                HealthTrack yalnızca kişisel beslenme, egzersiz, kilo ve randevu takibi amacıyla sunulan bir yaşam tarzı ve biyo-telemetri uygulamasıdır. Uygulama içindeki kalori, makro/mikro besin hedefleri, VKİ (BMI) hesaplamaları ve yapay zeka tabanlı analizler tıbbi tavsiye, teşhis veya tedavi yerine geçmez. Sağlıkla ilgili kararlarınızda mutlaka bir hekim veya uzman diyetisyene danışınız.
              </p>
            </div>
          </section>

          <section className="space-y-3 text-sm text-gray-300 leading-relaxed">
            <h2 className="text-base font-bold text-white flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-400" />
              2. Hesap Kullanımı ve Sorumluluk
            </h2>
            <ul className="list-disc pl-5 space-y-1.5 text-gray-400">
              <li>Hesap bilgilerinizin gizliliğini korumak sizin sorumluluğunuzdadır.</li>
              <li>Planora topluluk akışı ve ortak plan odalarında hukuka aykırı, taciz edici veya spam içerik paylaşılamaz.</li>
              <li>Yapay zeka kotasını aşmaya yönelik otomatik veya kötüye kullanım girişimlerinde hesap erişimi kısıtlanabilir.</li>
            </ul>
          </section>

          <section className="space-y-3 text-sm text-gray-300 leading-relaxed">
            <h2 className="text-base font-bold text-white">3. Hesap Sonlandırma ve Veri Silme</h2>
            <p className="text-gray-400">
              Kullanıcılar diledikleri zaman uygulama ayarlarından veya{' '}
              <a href="/delete-account" className="text-emerald-400 underline hover:text-emerald-300">
                Hesap Silme Sayfası
              </a>{' '}
              üzerinden hesaplarını kalıcı olarak silebilirler. Silinen hesaplar ve veriler geri getirilemez.
            </p>
          </section>

          <section className="space-y-2 text-sm text-gray-300 border-t border-white/10 pt-6">
            <h2 className="text-base font-bold text-white">4. İletişim</h2>
            <p className="text-gray-400">
              Sorularınız için:{' '}
              <a href="mailto:yusufteker.dev@gmail.com" className="text-emerald-400 underline">
                yusufteker.dev@gmail.com
              </a>
            </p>
          </section>
        </div>
      </div>
    </div>
  );
}
