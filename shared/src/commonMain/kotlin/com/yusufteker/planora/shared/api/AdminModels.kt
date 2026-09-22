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
