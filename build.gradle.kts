plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.buildkonfig) apply false

    // Baseline Profile & Macrobenchmark:
    // 1) androidBaselineProfile: Uygulamanın en kritik kod yollarını (açılış, splash, ana sayfa render vb.)
    //    otomatik tespit edip derleme öncesi profil dosyası (baseline-prof.txt) oluşturan eklenti.
    alias(libs.plugins.androidBaselineProfile) apply false

    // 2) androidTest: Benchmark ve test senaryolarını bağımsız bir modülde (:baselineprofile)
    //    çalıştırabilmek için Android Gradle Plugin (AGP) tarafından sağlanan test modülü eklentisi.
    alias(libs.plugins.androidTest) apply false
}