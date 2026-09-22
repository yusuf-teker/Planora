package com.yusufteker.planora.feature.home.presentation.premium

import com.yusufteker.planora.core.base.UiEffect
import com.yusufteker.planora.shared.billing.PaymentMethod
import com.yusufteker.planora.shared.billing.SubscriptionPeriod
import com.yusufteker.planora.shared.billing.SubscriptionPlan

/**
 * Planora Premium ekranı UI Durumu.
 */
data class PremiumState(
    val isPremium: Boolean = false,
    val availablePlans: List<SubscriptionPlan> = listOf(
        SubscriptionPlan(
            id = "planora_premium_annual",
            name = "Yıllık",
            priceText = "799.99 ₺ / Yıl",
            priceAmount = 799.99,
            period = SubscriptionPeriod.ANNUAL,
            discountPercentage = 33,
            isPopular = true,
            description = "Aylık sadece 66.60 ₺ (%33 Tasarruf)"
        ),
        SubscriptionPlan(
            id = "planora_premium_monthly",
            name = "Aylık",
            priceText = "99.99 ₺ / Ay",
            priceAmount = 99.99,
            period = SubscriptionPeriod.MONTHLY,
            discountPercentage = 0,
            isPopular = false,
            description = "İstediğiniz zaman iptal edebilirsiniz"
        )
    ),
    val selectedPlan: SubscriptionPlan = availablePlans.first(),
    val isIosPlatform: Boolean = false,
    val selectedPaymentMethod: PaymentMethod = if (isIosPlatform) PaymentMethod.APPLE_PAY else PaymentMethod.GOOGLE_PLAY,
    val isSyncing: Boolean = false,
    val showPaymentSheet: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Kullanıcı arayüzünden gelen eylemler.
 */
sealed interface PremiumEvent {
    data class OnPlanSelected(val plan: SubscriptionPlan) : PremiumEvent
    data class OnPaymentMethodSelected(val method: PaymentMethod) : PremiumEvent
    data object OnStartPurchaseClicked : PremiumEvent
    data object OnDismissPaymentSheet : PremiumEvent
    data object OnBackClicked : PremiumEvent
    data object OnRefreshPremiumStatus : PremiumEvent
}

/**
 * Tek seferlik olaylar.
 */
sealed interface PremiumEffect : UiEffect {
    data object NavigateBack : PremiumEffect
    data class ShowSnackbar(val message: String) : PremiumEffect
}
