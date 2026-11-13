import React from 'react';
import { Shield, ArrowLeft, Lock, HeartPulse, Mic, Sparkles, Trash2, Mail } from 'lucide-react';

export default function PrivacyPolicyView({ onBack }) {
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
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-xs font-bold mb-3">
              <Shield className="w-3.5 h-3.5" />
              Gizlilik Politikası
            </div>
            <h1 className="text-2xl sm:text-3xl font-black text-white tracking-tight">
              Planora & HealthTrack Gizlilik Politikası
            </h1>
            <p className="text-xs text-gray-400 mt-2">
              Son Güncelleme: 13 Kasım 2026 • Geliştirici: Yusuf Teker
            </p>
          </div>

          <section className="space-y-3 text-sm text-gray-300 leading-relaxed">
            <h2 className="text-base font-bold text-white flex items-center gap-2">
              <Lock className="w-4 h-4 text-emerald-400" />
              1. Toplanan Veriler ve Kullanım Amacı
            </h2>
            <p>
              <strong>Planora</strong> ve <strong>HealthTrack</strong> uygulamaları, hesap oluşturma, cihazlar arası senkronizasyon ve kişiselleştirilmiş planlama/sağlık takibi deneyimi sunmak amacıyla yalnızca gerekli verileri işler:
            </p>
            <ul className="list-disc pl-5 space-y-1.5 text-gray-400">
              <li>
                <strong className="text-gray-200">Hesap Bilgileri:</strong> Kullanıcı adı, e-posta adresi, ad-soyad ve şifrelenmiş (hash) parola bilgileri.
              </li>
              <li>
                <strong className="text-gray-200">Planora Verileri:</strong> Görevler, notlar, takvim etkinlikleri, odaklanma kayıtları, ortak plan odaları ve topluluk paylaşımları.
              </li>
              <li>
                <strong className="text-gray-200">HealthTrack Verileri:</strong> Beslenme/kalori kayıtları, makro ve mikro besin hedefleri, antrenman geçmişi, kilo ölçümleri ve doktor randevu notları.
              </li>
            </ul>
          </section>

          <section className="space-y-3 text-sm text-gray-300 leading-relaxed">
            <h2 className="text-base font-bold text-white flex items-center gap-2">
              <HeartPulse className="w-4 h-4 text-rose-400" />
              2. Health Connect ve Apple HealthKit Verileri
            </h2>
            <p>
              HealthTrack; açık rızanız doğrultusunda Android <strong>Health Connect</strong> veya iOS <strong>Apple HealthKit</strong> üzerinden kilo, vücut yağ oranı, kas kütlesi, adım sayısı, mesafe, egzersiz, yakılan kalori ve kalp atış hızı verilerini okuyabilir veya güncelleyebilir.
            </p>
            <div className="p-4 rounded-2xl bg-emerald-500/5 border border-emerald-500/20 text-xs text-emerald-200 space-y-1">
              <p className="font-bold text-emerald-400">Sınırlı Kullanım Taahhüdü (Limited Use)</p>
              <p>
                Health Connect ve Apple HealthKit üzerinden alınan hiçbir sağlık verisi üçüncü taraflara satılmaz, reklam/pazarlama amacıyla kullanılmaz veya veri simsarlarıyla paylaşılmaz. Veriler yalnızca uygulama içi sağlık ve beslenme analizlerinizi görüntülemeniz için kullanılır.
              </p>
            </div>
          </section>

          <section className="space-y-3 text-sm text-gray-300 leading-relaxed">
            <h2 className="text-base font-bold text-white flex items-center gap-2">
              <Mic className="w-4 h-4 text-cyan-400" />
              3. Kamera, Galeri, Mikrofon ve Takvim İzinleri
            </h2>
            <ul className="list-disc pl-5 space-y-1.5 text-gray-400">
              <li>
                <strong className="text-gray-200">Kamera ve Galeri:</strong> Yemek/besin etiketi fotoğraflarının analizi, profil fotoğrafı yükleme ve randevu belgesi ekleme işlemleri için yalnızca sizin seçiminizle kullanılır.
              </li>
              <li>
                <strong className="text-gray-200">Mikrofon ve Konuşma Tanıma:</strong> Sesli yemek veya görev kaydı oluşturmak istediğinizde anlık ses-metin dönüşümü için kullanılır; ham ses kayıtları sunucuda saklanmaz.
              </li>
              <li>
                <strong className="text-gray-200">Takvim ve Bildirimler:</strong> Planora görev hatırlatıcıları, odak bildirimleri ve HealthTrack randevu/ölçüm hatırlatıcıları için kullanılır.
              </li>
            </ul>
          </section>

          <section className="space-y-3 text-sm text-gray-300 leading-relaxed">
            <h2 className="text-base font-bold text-white flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-amber-400" />
              4. Yapay Zeka ve Üçüncü Taraf Servisler
            </h2>
            <p className="text-gray-400">
              Doğal dil ile girdiğiniz yemek/egzersiz metinleri veya planlama talepleri, besin/kalori tahmini ve akıllı öneriler üretmek amacıyla Google Gemini API üzerinden işlenebilir. Hata takibi ve anlık bildirim teslimatı için Google Firebase (Crashlytics &amp; Cloud Messaging) altyapısından faydalanılır. Tüm sunucu iletişimi HTTPS (TLS) ile şifrelenir.
            </p>
          </section>

          <section className="space-y-3 text-sm text-gray-300 leading-relaxed">
            <h2 className="text-base font-bold text-white flex items-center gap-2">
              <Trash2 className="w-4 h-4 text-rose-400" />
              5. Veri Saklama ve Hesap Silme Hakkı
            </h2>
            <p className="text-gray-400">
              Hesabınız aktif olduğu sürece verileriniz bulut veritabanında güvenli şekilde saklanır. Dilediğiniz zaman:
            </p>
            <ul className="list-disc pl-5 space-y-1 text-gray-400">
              <li><strong>Planora</strong> içinde: <em>Ayarlar &rarr; Hesabı Sil</em></li>
              <li><strong>HealthTrack</strong> içinde: <em>Profil &rarr; Hesabı Sil</em></li>
              <li>
                <strong>Web üzerinden:</strong>{' '}
                <a href="/delete-account" className="text-emerald-400 underline hover:text-emerald-300">
                  Hesap Silme Sayfası
                </a>
              </li>
            </ul>
            <p className="text-gray-400">
              adımlarını kullanarak hesabınızı ve tüm ilişkili Planora &amp; HealthTrack verilerinizi anında ve kalıcı olarak silebilirsiniz.
            </p>
          </section>

          <section className="space-y-2 text-sm text-gray-300 border-t border-white/10 pt-6">
            <h2 className="text-base font-bold text-white flex items-center gap-2">
              <Mail className="w-4 h-4 text-indigo-400" />
              6. İletişim
            </h2>
            <p className="text-gray-400">
              Gizlilik politikası veya verilerinizle ilgili her türlü soru ve talep için:{' '}
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
