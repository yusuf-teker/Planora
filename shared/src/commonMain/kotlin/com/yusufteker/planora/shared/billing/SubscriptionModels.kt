package com.yusufteker.planora.shared.billing

import kotlinx.serialization.Serializable

/**
 * Planora abonelik periyotları.
 */
@Serializable
enum class SubscriptionPeriod {
    MONTHLY,
    ANNUAL
}

/**
 * Desteklenen ödeme yöntemleri.
 * iOS için StoreKit / Apple Pay, Android için Google Play Billing ve Kredi Kartı.
 */
@Serializable
enum class PaymentMethod {
    GOOGLE_PLAY,
    CREDIT_CARD,
    APPLE_PAY
}

/**
 * Abonelik planı modeli.
 *
 * @property id Planın benzersiz tanımlayıcısı (örn: "planora_premium_monthly", "planora_premium_annual")
 * @property name Planın görünen adı
 * @property priceText Biçimlendirilmiş fiyat metni (örn: "99.99 ₺ / Ay", "799.99 ₺ / Yıl")
 * @property priceAmount Sayısal fiyat değeri
 * @property currency Para birimi simgesi
 * @property period Aylık veya Yıllık periyot
 * @property discountPercentage Varsa indirim oranı (örn: 33)
 * @property isPopular En popüler/önerilen plan etiketi
 * @property description Kısa açıklama
 */
@Serializable
data class SubscriptionPlan(
    val id: String,
    val name: String,
    val priceText: String,
    val priceAmount: Double,
    val currency: String = "₺",
    val period: SubscriptionPeriod,
    val discountPercentage: Int = 0,
    val isPopular: Boolean = false,
    val description: String = ""
)

/**
 * Satın alma işlemi sonucu.
 *
 * @property isSuccess İşlem başarılı mı
 * @property transactionId İşlem / Dekont ID'si (simülasyon veya mağaza)
 * @property planId Satın alınan plan ID'si
 * @property paymentMethod Kullanılan ödeme yöntemi
 * @property errorMessage Hata durumunda hata mesajı
 * @property isSimulation Bu işlemin demo/mock modunda yapılıp yapılmadığı
 */
@Serializable
data class PurchaseResult(
    val isSuccess: Boolean,
    val transactionId: String? = null,
    val planId: String? = null,
    val paymentMethod: PaymentMethod? = null,
    val errorMessage: String? = null,
    val isSimulation: Boolean = true
)

/**
 * Planora Premium avantaj maddesi modeli.
 */
@Serializable
data class PremiumFeatureItem(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String
)

/**
 * Mağaza satın alma doğrulama isteği (Google Play Billing, Apple StoreKit, RevenueCat).
 */
@Serializable
data class VerifyPurchaseRequest(
    val appId: String = "planora",
    val planId: String,
    val provider: String, // GOOGLE_PLAY, APPLE_APP_STORE, REVENUECAT
    val purchaseToken: String? = null,
    val transactionId: String? = null,
    val receiptData: String? = null
)

/**
 * Mağaza satın alma doğrulama yanıtı.
 */
@Serializable
data class VerifyPurchaseResponse(
    val success: Boolean,
    val isPremium: Boolean = false,
    val premiumUntil: String? = null,
    val planId: String? = null,
    val provider: String? = null,
    val status: String? = null,
    val message: String = "",
    val billingConfigured: Boolean = false
)

/**
 * Kullanıcının güncel abonelik durumu yanıtı.
 */
@Serializable
data class SubscriptionStatusResponse(
    val isPremium: Boolean = false,
    val premiumUntil: String? = null,
    val activePlanId: String? = null,
    val provider: String? = null,
    val status: String = "NONE",
    val autoRenew: Boolean = false,
    val billingConfigured: Boolean = false
)

/**
 * Satın alımları geri yükleme isteği.
 */
@Serializable
data class RestorePurchaseRequest(
    val appId: String = "planora",
    val provider: String,
    val purchaseToken: String? = null,
    val transactionId: String? = null
)
