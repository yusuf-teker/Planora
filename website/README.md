# HealthTrack & Planora Web Portalı (Admin & Kullanıcı Merkezi)

Bu web uygulaması, **HealthTrack** ve **Planora** ekosistemini tek bir çatı altında birleştiren; hem kullanıcıların kişisel sağlık raporlarını ve görevlerini inceleyebildiği hem de yöneticilerin (Admin) sistemi denetleyip FCM push bildirimleri gönderebildiği modern bir web platformudur.

---

## 🌟 Öne Çıkan Yetenekler

### 1. Ortak Kimlik Doğrulama (Unified Authentication)
* Kullanıcılar ve yöneticiler, mobil uygulamadaki mevcut Planora/HealthTrack e-posta/kullanıcı adı ve şifresi ile giriş yapar.
* Tek veritabanı (**Neon PostgreSQL**) ve ortak JWT imzalama sistemi kullanıldığı için ek bir kayıt/hesap gerektirmez.

### 2. Çalışma Alanı Seçici (Ecosystem Switcher)
Giriş yapıldıktan sonra kullanıcıyı modern bir karşılama ekranı karşılar:
* 🍏 **HealthTrack Portalı:**
  * Günlük kalori göstergesi, kalan kalori ve makro dağılımı (Protein / Karbonhidrat / Yağ).
  * Kaydedilen öğün günlüğü ve AI besin dökümleri.
  * Egzersizler, spor aktiviteleri ve yakılan toplam enerji (kcal).
  * Tartı / Kilo geçmişi ve önceki tartıya göre kilo değişim eğrisi.
  * Klinik ve doktor randevuları takvimi.
  * **Diyetisyen & Doktor Rapor İhracı:** Tek tıkla doktora sunulabilir temiz PDF yazdırma görünümü veya CSV dosya indirme imkanı.
* ⚡ **Planora Portalı:**
  * Günlük görevler, yapılacaklar listesi (To-Do) ve etkinlikler.
  * Durum filtreleme (Tümü, Bekleyenler, Tamamlananlar).
  * Verimlilik puanı ve tamamlanma oranı (%) telemetrisi.
  * HealthTrack üzerinden otomatik senkronize edilen doktor randevu rozetleri.

### 3. Süper Admin Kontrol Merkezi (Yalnızca ADMIN Rolüne Görünür)
Kullanıcının veritabanındaki rolü `role = 'ADMIN'` olduğunda üst menüde ve çalışma alanı seçicide özel **Admin Konsolu** belirir:
* 🚀 **Canlı FCM Push Bildirim İstasyonu:**
  * Eski `push-panel.html`'in yerini alan, telefon önizlemeli modern arayüz.
  * Tüm kullanıcılara genel duyuru (Broadcast) veya tekil kullanıcı arama ile doğrudan hedefe bildirim iletme.
  * Yazdıkça canlı güncellenen akıllı telefon bildirim kilit ekranı önizlemesi.
  * Hızlı bildirim şablonları (Beslenme hatırlatması, Planora görev motivasyonu, Premium tebrik mesajı).
* 👥 **Kullanıcı & Abonelik Yönetimi:**
  * Anlık arama (isim, kullanıcı adı, e-posta).
  * Kullanıcı rolleri değiştirme (`USER` ↔ `ADMIN` ↔ `NOTIFIER`).
  * Tek tıkla **30 Günlük / 90 Günlük / Süresiz (Ömür Boyu) Premium** tanımlama veya iptal etme.
  * Kullanıcının görevlerini inceleme modal penceresi.
  * Hesap silme (onay korumalı).
* 📊 **Sistem & Neon DB Telemetrisi:**
  * Toplam kullanıcı, görev, plan odası, topluluk postu, aktif FCM cihazı ve premium üye sayıları.
  * Render Ktor sunucusu ve Neon PostgreSQL canlılık/gecikme (ms) takibi.

---

## 🛠️ Yerel Geliştirme (Local Development)

```bash
cd web
npm install
npm run dev
```

Tarayıcınızda `http://localhost:5173` adresine giderek portala erişebilirsiniz. Geliştirme modunda Vite proxy (`vite.config.js`) sayesinde Render backend'ine (`https://pulse-7b4z.onrender.com`) CORS engeline takılmadan otomatik yönlendirilir.

---

## 🚀 Render.com Üzerinde Canlıya Alma (Static Site Dağıtımı)

Bu projeyi Render üzerinde **ücretsiz** bir **Static Site** olarak dakikalar içinde canlıya alabilirsiniz:

1. [Render.com Dashboard](https://dashboard.render.com)'a gidin.
2. **New +** -> **Static Site** seçeneğini tıklayın.
3. GitHub / Git repository'nizi seçin.
4. Ayarları şu şekilde yapılandırın:
   * **Name:** `planora-healthtrack-portal`
   * **Root Directory:** `web`
   * **Build Command:** `npm install && npm run build`
   * **Publish Directory:** `dist`
5. **Redirects/Rewrites** sekmesine gidin ve SPA yönlendirmesini ekleyin:
   * **Type:** `Rewrite`
   * **Source:** `/*`
   * **Destination:** `/index.html`
6. **Create Static Site** butonuna tıklayın.

*Not: Render Static Site servisleri tamamen ücretsizdir ve arka planda uyku moduna (spin-down) geçmez; 0 ms bekleme süresiyle dünya çapında CDN üzerinden anında açılır.*
