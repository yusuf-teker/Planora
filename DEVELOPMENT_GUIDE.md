# Pulse Uygulaması Geliştirici Kılavuzu (Development Guide)

Pulse, **Kotlin Multiplatform (KMP)** kullanılarak geliştirilen, Android ve iOS için ortak iş mantığına sahip olan ve "Offline-First" (Çevrimdışı Öncelikli) mimariyi benimseyen bir uygulamadır. Bu kılavuz, projeye yeni dahil olan veya sistemin nasıl çalıştığını anlamak isteyen geliştiriciler (ve yapay zeka ajanları) için hazırlanmıştır.

## 1. Mimari Genel Bakış (Architecture Overview)

Pulse, modern mobil uygulama standartlarına göre inşa edilmiştir:
- **UI:** Jetpack Compose (Compose Multiplatform)
- **Veritabanı:** SQLDelight (`core` modülü içinde)
- **Ağ (Network):** Ktor Client
- **Bağımlılık Enjeksiyonu (DI):** Koin
- **Asenkron İşlemler:** Kotlin Coroutines & Flow
- **Sunucu (Backend):** Ktor Server (Ayrı `server` modülü olarak aynı repoda barınır)

Uygulamanın ana felsefesi **Single Source of Truth (SSOT - Tek Gerçeklik Kaynağı)** prensibidir. Uygulamadaki tüm UI verileri KESİNLİKLE lokal veritabanından (SQLDelight) okunur. Uzak sunucudan gelen veriler doğrudan UI'a yansıtılmaz; önce veritabanına yazılır ve veritabanını dinleyen (observe eden) Flow'lar tetiklenerek UI güncellenir.

## 2. Offline-First (Çevrimdışı Öncelikli) Yaklaşımı

Uygulamanın en kritik özelliği internet yokken bile tam teşekküllü çalışabilmesidir. Bu mekanizma şu adımlarla işler:

1. **İyimser Güncelleme (Optimistic UI):** Kullanıcı bir görev eklediğinde, sildiğinde veya güncellediğinde, işlem ağ isteği beklenmeden anında yerel SQLite veritabanına yazılır.
2. **Geçici ID'ler (Local IDs):** İnternet yokken oluşturulan varlıklar (örneğin görevler) `local_1234abcd` şeklinde geçici bir kimlik alır ve `isSynced = 0` (senkronize edilmedi) olarak işaretlenir.
3. **Arka Plan Senkronizasyonu (Background Sync):** Her yazma işleminden sonra arka planda bir senkronizasyon Coroutine'i başlatılır (örn. `syncPendingChanges()`). Bu metot, `Mutex` ile korunur (aynı anda sadece bir kez çalışır). İnternet bağlantısı sağlandığında, `isSynced = 0` olan kayıtlar sunucuya POST/PUT işlemleri ile gönderilir.
4. **Gerçek ID Eşlemesi:** Sunucu, geçici ID ile gönderilen veriye veritabanında kalıcı bir UUID atar. Mobil cihaz bu yanıtı aldığında, geçici `local_xxx` ID'sini asıl UUID ile değiştirir ve ilişkili tabloları (alt görevler vb.) günceller.
5. **Çakışma Kontrolü (Conflict Resolution):** Eğer senkronizasyon devam ederken kullanıcı görevi yerelde tekrar güncellerse, `isSynced` bayrağı bilerek `0` bırakılır ki bir sonraki döngüde en güncel hali sunucuya gönderilsin.

## 3. Servisler ve Proje Yapısı

Uygulama temel modüllere ayrılmıştır:

- **`core` Modülü:** Uygulamanın kalbidir. Veritabanı yapılandırması (`PulsyDatabase.sq`), ağ (`HttpClientProvider`), yapılandırmalar (Preferences/DataStore) ve utility (yardımcı) sınıfları barındırır.
- **`shared` Modülü:** Sunucu (Backend) ve Mobil İstemci (Client) tarafından ortak paylaşılan modelleri içerir. API İstek/Cevap objeleri (DTO'lar), Enum'lar (`TaskStatus`, `TaskType`) burada yer alır.
- **`feature-auth` Modülü:** Kullanıcı girişi, kayıt ve şifre işlemleri.
- **`feature-home` Modülü:** Ana uygulama işlevleri, görevler, takvim yönetimi, plan odaları (Plan Rooms).
- **`server` Modülü:** Ktor ile yazılmış olan arka uç (backend) kodlarını içerir.

### Ağ İsteği (Network Request) ve Token Yönetimi

Ktor'un standart `Auth` plugin'indeki `loadTokens` (cache) mekanizması **KULLANILMAMAKTADIR**. Çünkü bu yapı, kullanıcı hesaptan çıkış yapıp başka bir hesaba girdiğinde eski kullanıcının token'ını hafızada tutarak veri sızmasına sebep olmaktadır.

Bunun yerine her istekte token `SessionPreferences` üzerinden anlık olarak okunur:

```kotlin
// core/src/commonMain/kotlin/com/yusufteker/pulse/core/network/HttpClientProvider.kt
client.requestPipeline.intercept(io.ktor.client.request.HttpRequestPipeline.State) {
    val requestBuilder = context
    val path = requestBuilder.url.buildString()
    
    // Auth istekleri ve Google API'leri hariç, token'ı DataStore'dan anlık olarak oku
    if (!path.contains("auth/") && !path.contains("googleapis.com")) {
        val token = sessionPreferences.getAccessToken()
        if (token != null) {
            requestBuilder.headers.remove(HttpHeaders.Authorization)
            requestBuilder.headers.append(HttpHeaders.Authorization, "Bearer ${'$'}token")
        }
    }
}
```

### Örnek İşlem Akışı: Görevleri Çekme (Fetch Tasks)

Aşağıdaki örnekte verilerin nasıl çekilip veritabanına yazıldığı (ve senkronize edilmemiş yerel değişikliklerin nasıl korunduğu) görülmektedir:

```kotlin
// PlanRepositoryImpl.kt içindeki fetchMyTasks metodu özeti

// 1. Ağ isteği (Ktor Client üzerinden) yapılır
val remoteTasks = planApi.getMyTasks(fromTime, toTime)

// 2. Tüm işlemler transaction içinde yapılarak veri bütünlüğü korunur
database.pulsyDatabaseQueries.transaction {
    val remoteTaskIds = remoteTasks.map { it.id }.toSet()
    val localTasks = database.pulsyDatabaseQueries.getAllTasks().executeAsList()

    // 3. Sunucuda artık olmayan ama lokalde isSynced = 1 (zaten senkronlanmış) olanları SİL
    // Bu, sunucudan silinen verilerin lokalden de düşmesini sağlar.
    localTasks.forEach { localTask ->
        if (!remoteTaskIds.contains(localTask.id) && localTask.isSynced == 1L) {
            database.pulsyDatabaseQueries.deleteTaskById(localTask.id)
        }
    }

    // 4. Sunucudan gelen verileri lokal DB'ye ekle/güncelle
    remoteTasks.forEach { remoteTask ->
        val existingTask = database.pulsyDatabaseQueries.getTaskById(remoteTask.id).executeAsOneOrNull()
        
        // KRİTİK: Eğer görev lokalde değiştirilmiş ve henüz sunucuya gönderilmemişse (isSynced=0),
        // sunucudan gelen eski veri ile lokaldeki değişikliği EZME!
        if (existingTask != null && existingTask.isSynced == 0L) {
            return@forEach 
        }
        
        // Değişiklik yoksa güvenle veritabanına kaydet
        database.pulsyDatabaseQueries.insertTaskFromDto(remoteTask, isSynced = 1L)
    }
}
```

## 4. Geliştirici Kuralları (Best Practices & Gotchas)

### 4.1. Veritabanı Değişiklikleri ve Temizlik (Data Leaks Önlemi)
Yeni bir tablo (Entity) oluşturduğunuzda KESİNLİKLE ilgili temizlik kodunu yazmalısınız. Aksi halde bir kullanıcı çıkış yapıp, yeni bir kullanıcı girdiğinde önceki kullanıcının verilerini görecektir!
1. `PulsyDatabase.sq` dosyasına tablonuzu ekleyin.
2. `PulsyDatabase.sq` dosyasına silme sorgusunu ekleyin: `deleteAllMyNewTable: DELETE FROM myNewTableEntity;`
3. `core/src/commonMain/kotlin/com/yusufteker/pulse/core/database/PulsyDatabaseExt.kt` dosyasındaki `PulsyDatabase.clearAll()` fonksiyonunun içine `deleteAllMyNewTable()` çağrısını ekleyin.

### 4.2. ViewModel ve Dependency Injection (Koin)
ViewModel sınıfları Koin modüllerinde `single` (singleton) olarak **TANIMLANMAMALIDIR**. Tüm ViewModeller `factory` veya `viewModel` (Compose için) olmalıdır. Eğer singleton yaparsanız, ekran değiştirildiğinde state sıfırlanmaz ve önceki veriler (veya önceki hesabın verileri) ekranda asılı kalır. Repository ve UseCase'ler `single` olabilir.

### 4.3. UI ve Boş Durumlar (Empty States)
Kullanıcı deneyimi bizim için önemlidir. Görev listesi, plan odaları listesi veya not listesi boş olduğunda asla beyaz, boş bir ekran göstermeyin. Kullanıcıya özel "Empty State" tasarımları ve grafikler sunun (Örn: "Henüz bir görevin yok, eklemek ister misin?").

### 4.4. Navigasyon Güvenliği
Kullanıcı `Logout` (Çıkış) işlemi yaptığında, `navController.navigate("login") { popUpTo(0) }` (veya denk gelen yapı) kullanılarak tüm "back-stack" temizlenmelidir. Cihazın "Geri" tuşuna basıldığında uygulamanın önceki şifreli alanlarına geri dönülmesi kesinlikle engellenmelidir.

### 4.5. Mapper Güncellemeleri
`TaskEntity` (veritabanı), `TaskDto` (API yanıtı) or `CreateTaskRequest` objelerinden birine yeni bir parametre (örn: `location`) eklerseniz, bu parametreyi **KESİNLİKLE** `feature-home/src/commonMain/kotlin/com/yusufteker/pulse/feature/home/data/mapper/TaskMapper.kt` dosyasındaki dönüştürme fonksiyonlarına dahil edin. Aksi takdirde, veriler okunurken veya yazılırken bu alanlar kaybolur!

### 4.6. Dil Desteği ve Yerelleştirme (Localization - TR & EN)
Pulse, çok dilli bir yapıya sahiptir. Compose Multiplatform'un kendi kaynak yönetim sistemi kullanılmaktadır.
- UI ekranlarındaki hiçbir metin, başlık, uyarı veya buton etiketi **sabit (hardcoded) String** olarak kod içine yazılmamalıdır.
- İngilizce (varsayılan) dil metinleri: `core/src/commonMain/composeResources/values/strings.xml` dosyasında yer almalıdır.
- Türkçe dil metinleri: `core/src/commonMain/composeResources/values-tr/strings.xml` dosyasında yer almalıdır.
- Compose kodunda bu metinler `stringResource(Res.string.action_skip)` şeklinde çağrılmalıdır.

### 4.7. Fonksiyonel Yorum Satırları ve KDoc Dokümantasyonu
- Yazılan tüm sınıflar, arayüzler (interface), repository implementasyonları ve özellikle yeni/değiştirilen fonksiyonlar için KDoc/dokümantasyon bloğu eklenmelidir.
- Dokümantasyon; fonksiyonun ne iş yaptığını, aldığı parametrelerin işlevini, döndürdüğü değeri ve barındırdığı özel iş mantıklarını açıkça belirtmelidir.
