# Pulse Uygulaması Geliştirme Kuralları (Best Practices & Gotchas)

Bu belge, Pulse projesine yeni bir özellik (ekran, viewmodel, tablo vb.) eklenirken dikkat edilmesi gereken kritik kuralları içerir. Bu kurallara uyulmaması veri sızıntılarına veya state yönetim hatalarına yol açabilir.

## 1. Veritabanı (SQLDelight) ve Tablo İşlemleri

- **Yeni Tablo Eklendiğinde**: `PulsyDatabase.sq` dosyasına yeni bir tablo (Entity) eklerseniz, KESİNLİKLE aynı dosyaya o tablonun tüm verilerini silen tekli bir sorgu (Örn: `deleteAllMessages: DELETE FROM messageEntity;`) eklemelisiniz.
- **Temizlik (ClearAll) Güncellemesi**: Oluşturduğunuz yeni silme sorgusunu, `core/src/commonMain/kotlin/com/yusufteker/pulse/core/database/PulsyDatabaseExt.kt` içerisindeki `clearAll()` extension fonksiyonuna (transaction bloğu içine) **mutlaka** eklemelisiniz. 
  - *Neden?* Kullanıcı çıkış yapıp başka bir hesapla giriş yaptığında, lokal SQLite veritabanının %100 temizlendiğinden emin olmak için `clearAll()` fonksiyonu çağrılır. Buraya eklenmeyen tablolar, yeni hesaba veri sızdırır!

## 2. Ktor ve Network (HTTP) İşlemleri

- **Token Yönetimi**: Ktor'un `Auth` plugin'ine güvenip token cache (bellek) üzerinden işlem yapmayın (`loadTokens` kullanmayın). `HttpClient` Koin'de Singleton (tekil) olduğu için, uygulama kapanmadan farklı hesaba geçilirse Ktor eski kullanıcının tokenını hafızada tutar.
- **Interceptor Kullanımı**: Her isteğe yetki (Authorization) başlığı eklemek için, `HttpClientProvider.kt` içindeki `requestPipeline.intercept` yapısı kullanılmalıdır. Bu yapı DataStore'dan (SessionPreferences) tokenı anlık olarak okur, böylece hesap değiştirildiğinde her zaman en güncel token kullanılır.

## 3. ViewModel ve State Yönetimi

- **Koin Bağımlılıkları (Injection)**: Repositories (örn: `FeedRepository`, `AuthRepository`) `single` (Singleton) olarak tanımlanmalıdır. Ancak, UI State tutan `ViewModel` sınıfları mutlaka `factory` veya `viewModel` olarak tanımlanmalıdır ki her ekrana girildiğinde yeni baştan temiz bir şekilde üretilsinler.
- **Flow/StateFlow**: UI'da dinlenen (`collect`) veriler StateFlow ise ve Repository'den geliyorsa, veritabanı boşaldığında bu Flow'ların otomatik olarak boş liste fırlatacağından emin olmalısınız (SQLDelight bunu `notifyQueries` ile kendi yapar, bu yüzden SQLDelight'ta manuel `driver.execute` yerine otomatik üretilen fonksiyonları kullanın).

## 4. Yeni Ekran (Screen) Eklerken

- **Empty State (Boş Ekran) Tasarımı**: Listeleme yapılan her ekran (Home, Plan Rooms, Notes vb.) için KESİNLİKLE veri olmadığında gösterilecek bir "Boş Ekran (Empty State)" tasarımı konulmalıdır ("Henüz bir plan odası yok", "Henüz bir görev yok" gibi).
- **Navigation (Yönlendirme)**: Kullanıcı çıkış (Logout) yaptığında back-stack (geri dönüş yığını) tamamen temizlenmeli (`popUpTo`) ve `login` ekranına yönlendirilmelidir ki geri tuşuyla yetkisiz sayfalara dönülememesin.
