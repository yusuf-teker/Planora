package com.yusufteker.planora.shared.api

import kotlinx.serialization.Serializable

/**
 * Admin tarafından bir kullanıcıya Premium atama veya kaldırma isteği.
 *
 * @property email Hedef kullanıcının e-posta adresi (opsiyonel)
 * @property userId Hedef kullanıcının sayısal ID'si (opsiyonel)
 * @property isPremium Premium aktif mi pasif mi yapılacağı
 * @property days Kaç gün süreyle verileceği (null ise süresiz/kalıcı)
 */
@Serializable
data class SetPremiumRequest(
    val email: String? = null,
    val userId: Int? = null,
    val isPremium: Boolean = true,
    val days: Int? = null
)

/**
 * Admin tarafından gerçekleştirilen Premium işlemi sonucu.
 */
@Serializable
data class SetPremiumResponse(
    val success: Boolean,
    val message: String,
    val userId: Int,
    val email: String,
    val isPremium: Boolean,
    val premiumUntil: String? = null
)

/**
 * Admin tarafından belirli bir kullanıcıya veya tüm kullanıcılara push bildirimi gönderme isteği.
 *
 * @property userId Hedef kullanıcının sayısal ID'si (opsiyonel)
 * @property username Hedef kullanıcının kullanıcı adı (opsiyonel)
 * @property email Hedef kullanıcının e-posta adresi (opsiyonel)
 * @property broadcastAll Tüm aktif FCM token sahibi kullanıcılara gönderilsin mi
 * @property title Bildirim başlığı
 * @property body Bildirim metni
 * @property data İstemciye iletilecek opsiyonel ekstra anahtar-değer haritası
 */
@Serializable
data class AdminSendPushRequest(
    val userId: Int? = null,
    val username: String? = null,
    val email: String? = null,
    val broadcastAll: Boolean = false,
    val title: String,
    val body: String,
    val data: Map<String, String>? = null
)

/**
 * Admin push bildirimi gönderim sonucu.
 *
 * @property success İşlem başarılı mı
 * @property message İşlem detay mesajı
 * @property recipientCount Bildirimin ulaştırıldığı kullanıcı sayısı
 */
@Serializable
data class AdminSendPushResponse(
    val success: Boolean,
    val message: String,
    val recipientCount: Int = 0
)
