package com.yusufteker.planora.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * =========================================================================================
 * BASELINE PROFILE GENERATOR (Planora Başlangıç ve Akıcılık Profilleyicisi)
 * =========================================================================================
 *
 * 🎯 BU SINIF NEDİR VE NEDEN VAR?
 * -----------------------------------------------------------------------------------------
 * Normalde Android uygulamaları ilk açıldığında kodları "Just-In-Time" (JIT) ile anlık olarak
 * derler. Jetpack Compose & Compose Multiplatform mimarisinde ilk açılışta Koin DI, SQLDelight DB,
 * yüzlerce Compose bileşeni ve tema motoru aynı anda belleğe yüklenir.
 * Bu durum uygulamanın ilk 1-2 saniyesinde mikro donmalara (frame drop / jank) neden olur.
 *
 * Bu sınıf, Google'ın Macrobenchmark kütüphanesini kullanarak uygulamayı gerçek bir cihazda
 * ya da emülatörde çalıştırır. Uygulama açılırken hangi fonksiyonların, sınıfların ve Compose
 * ağaçlarının çalıştığını adım adım kaydeder ve bir "ön derleme haritası" (baseline-prof.txt) üretir.
 *
 * 🚀 SONUÇ:
 * Üretilen bu profil dosyası APK/AAB içine gömülür. Kullanıcı uygulamayı indirdiği anda
 * Android Runtime (ART) bu kritik kodları önceden derler (Ahead-Of-Time / AOT).
 * Böylece kullanıcı uygulamayı açtığında hiçbir takılma yaşamadan yağ gibi akan bir deneyim elde eder!
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    /**
     * BaselineProfileRule:
     * Android Benchmark kütüphanesinin profil toplamak için sağladığı resmi JUnit kuralı.
     * Cihazdaki test ortamını hazırlar, profili kaydeder ve dosya olarak dışa aktarır.
     */
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    /**
     * 1. ADIM: Cold Start (İlk Açılış) ve Ana Ekran Çizimi Profili
     * -------------------------------------------------------------------------------------
     * Bu test senaryosu uygulamanın sıfırdan açılışını (Splash -> MainActivity -> Ana Ekran) simüle eder.
     *
     * Nasıl Çalıştırılır?
     * Terminalden şu komut verilir (Cihaz veya Emülatör açık olmalı):
     * `./gradlew :baselineprofile:generateBaselineProfile`
     * veya Android Studio'da bu test fonksiyonunun solundaki yeşil "Run" ikonuna tıklanabilir.
     */
    @Test
    fun generateBaselineProfile() = baselineProfileRule.collect(
        // Hedef Android paket adı (:androidApp içinde tanımlı olan applicationId)
        packageName = "com.yusufteker.planora",

        // Açılış anındaki ilk 100-200ms için kritik olan Startup Profile'ı da dahil et
        includeInStartupProfile = true,

        // Profil üretim fonksiyonu (Burada kullanıcının en sık yaptığı hareketleri canlandırıyoruz)
        profileBlock = {
            // 1. Cihazın ana ekranına dön (Uygulamanın RAM'de kalan eski bir oturumu varsa temizlensin)
            pressHome()

            // 2. Planora uygulamasını sıfırdan (Cold Start) başlat ve ilk ekran açılana kadar bekle
            startActivityAndWait()

            // 3. UI boşta kalana (Compose ilk çizimi, Koin ve DB sorguları bitene) kadar bekle
            device.waitForIdle()

            // 4. KRİTİK KULLANICI AKIŞI (CUJ): Ana Ekran Liste/Takvim Kaydırma (Scroll Jank Önleme)
            // Compose'da en çok takılma ekran ilk kaydırıldığında LazyColumn/Grid öğeleri render edilirken olur.
            // Cihaz ekran koordinatlarını hesaplayarak yumuşak bir kaydırma (swipe) hareketi yapıyoruz:
            val displayWidth = device.displayWidth
            val displayHeight = device.displayHeight

            // Aşağı doğru kaydır (Listeyi aşağı sürükleyip yeni saatleri/görevleri göster)
            device.swipe(
                displayWidth / 2,
                (displayHeight * 0.70).toInt(),
                displayWidth / 2,
                (displayHeight * 0.30).toInt(),
                20 // Kaydırma hızı (adım sayısı)
            )
            device.waitForIdle()

            // Yukarı doğru geri kaydır (Tekrar ilk pozisyona dön)
            device.swipe(
                displayWidth / 2,
                (displayHeight * 0.30).toInt(),
                displayWidth / 2,
                (displayHeight * 0.70).toInt(),
                20
            )
            device.waitForIdle()
        }
    )
}
