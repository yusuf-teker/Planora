import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * =========================================================================================
 * BASELINE PROFILE MODÜLÜ YAPILANDIRMASI
 * =========================================================================================
 * 
 * Bu modül, uygulamanın performansını ölçen ve "Baseline Profile" (ön derleme haritası)
 * üreten özel bir Android test modülüdür (com.android.test).
 * 
 * Normal bir uygulama modülü değildir; cihaz üzerinde bağımsız bir test koşucusu
 * olarak çalışır ve :androidApp modülünü açıp analiz eder.
 */
plugins {
    // 1) androidTest: Bu modülün cihazda çalışacak bir test paketi olduğunu belirtir.
    alias(libs.plugins.androidTest)

    // 2) androidBaselineProfile: Macrobenchmark testini koşturup çıkan profili
    //    otomatik olarak :androidApp modülüne yerleştiren resmi Gradle eklentisi.
    alias(libs.plugins.androidBaselineProfile)

    // NOT: AGP 9.0+ sürümünde Kotlin desteği dahili olarak geldiğinden
    // 'org.jetbrains.kotlin.android' eklentisine gerek yoktur.
}

android {
    namespace = "com.yusufteker.planora.baselineprofile"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()

        // Testleri koşacak olan AndroidX test çalıştırıcısı
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Emülatörde veya düşük pilde test çalıştırırken hata vermesini engelle
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR,LOW-BATTERY"
    }

    // Hedef proje: Baseline profilinin hangi uygulamayı analiz edeceğini belirtir.
    // Bu sayede testler çalışırken otomatik olarak :androidApp yüklenir ve test edilir.
    targetProjectPath = ":androidApp"

    // Baseline Profile ayarları
    baselineProfile {
        // Otomatik olarak bilgisayara bağlı fiziksel cihazı ya da açık olan emülatörü kullanır
        useConnectedDevices = true
    }
}

dependencies {
    // 1) JUnit ve AndroidX Test Runner: Test senaryolarını koşmak için temel altyapı
    implementation(libs.junit)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.runner)

    // 2) UI Automator: Cihaz ekranındaki butonlara tıklama, home tuşuna basma,
    //    ekranın açılmasını bekleme gibi sistem seviyesinde arayüz kontrollerini sağlar.
    implementation(libs.androidx.test.uiautomator)

    // 3) Macrobenchmark: Google'ın uygulama açılışı ve performansını ölçen,
    //    BaselineProfileRule sınıfını sağlayan resmi benchmark kütüphanesi.
    implementation(libs.androidx.benchmark.macro)
}
