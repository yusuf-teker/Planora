package com.yusufteker.planora.feature.home.domain.sync

/**
 * 1. WORKMANAGER (Arka Plan İşlemleri) İÇİN HAZIRLIK
 * KMP (Kotlin Multiplatform) dünyasında, Android'in WorkManager'ı iOS'te doğrudan kullanılamaz.
 * Bu yüzden ortak kod (commonMain) kısmında sadece bir arayüz (interface) tanımlarız.
 * 
 * Daha sonra (İleriki aşamalarda) Android tarafında bu arayüzü implement eden bir sınıf yazıp
 * içine WorkManager kodlarını yerleştireceğiz. iOS tarafında ise BGTaskScheduler kullanacağız.
 * 
 * Şimdilik offline-first yapısını kurmak için arayüzümüzü hazırlıyoruz.
 */
interface PostSyncManager {
    /**
     * Yerel veritabanında "Gönderilmeyi Bekliyor" (isDraft = 0) olarak işaretlenmiş
     * tüm postları sırayla alıp sunucuya gönderme işlemini tetikler.
     */
    suspend fun syncPendingPosts()
}
