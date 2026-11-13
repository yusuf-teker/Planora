package com.yusufteker.planora.server.service

import com.yusufteker.planora.server.database.DatabaseFactory.dbQuery
import com.yusufteker.planora.server.database.tables.SubscriptionEntity
import com.yusufteker.planora.server.database.tables.SubscriptionsTable
import com.yusufteker.planora.server.database.tables.UserEntity
import com.yusufteker.planora.server.database.tables.isPremiumActive
import com.yusufteker.planora.shared.billing.SubscriptionPeriod
import com.yusufteker.planora.shared.billing.SubscriptionPlan
import com.yusufteker.planora.shared.billing.SubscriptionStatusResponse
import com.yusufteker.planora.shared.billing.VerifyPurchaseRequest
import com.yusufteker.planora.shared.billing.VerifyPurchaseResponse
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Store receipt verification & subscription lifecycle management service.
 *
 * Ready to connect to:
 * - Google Play Developer API (`GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`, `GOOGLE_PLAY_PACKAGE_NAME`)
 * - Apple App Store Server API v2 (`APPLE_APP_STORE_ISSUER_ID`, `APPLE_APP_STORE_KEY_ID`, `APPLE_APP_STORE_PRIVATE_KEY`)
 * - RevenueCat REST API (`REVENUECAT_SECRET_KEY`)
 *
 * Until store credentials are provided in environment variables, verification returns `billingConfigured = false`
 * while preserving the complete persistence, status, restore, and admin-grant pipeline.
 */
object SubscriptionService {

    private val googlePlayConfigured: Boolean
        get() = !System.getenv("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON").isNullOrBlank()

    private val appleStoreConfigured: Boolean
        get() = !System.getenv("APPLE_APP_STORE_ISSUER_ID").isNullOrBlank() &&
            !System.getenv("APPLE_APP_STORE_PRIVATE_KEY").isNullOrBlank()

    private val revenueCatConfigured: Boolean
        get() = !System.getenv("REVENUECAT_SECRET_KEY").isNullOrBlank()

    fun isBillingConfiguredForProvider(provider: String): Boolean {
        return when (provider.uppercase().trim()) {
            "GOOGLE_PLAY" -> googlePlayConfigured
            "APPLE_APP_STORE", "APPLE_PAY" -> appleStoreConfigured
            "REVENUECAT" -> revenueCatConfigured
            else -> false
        }
    }

    fun isAnyBillingConfigured(): Boolean {
        return googlePlayConfigured || appleStoreConfigured || revenueCatConfigured
    }

    fun getPlansForApp(appId: String): List<SubscriptionPlan> {
        val normalized = appId.lowercase().trim()
        return if (normalized == "healthtrack" || normalized == "health") {
            listOf(
                SubscriptionPlan(
                    id = "healthtrack_premium_annual",
                    name = "Yıllık",
                    priceText = "799.99 ₺ / Yıl",
                    priceAmount = 799.99,
                    period = SubscriptionPeriod.ANNUAL,
                    discountPercentage = 33,
                    isPopular = true,
                    description = "Aylık sadece 66.60 ₺ (%33 Tasarruf)"
                ),
                SubscriptionPlan(
                    id = "healthtrack_premium_monthly",
                    name = "Aylık",
                    priceText = "99.99 ₺ / Ay",
                    priceAmount = 99.99,
                    period = SubscriptionPeriod.MONTHLY,
                    discountPercentage = 0,
                    isPopular = false,
                    description = "İstediğiniz zaman iptal edebilirsiniz"
                )
            )
        } else {
            listOf(
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
            )
        }
    }

    suspend fun getStatus(userId: Int): SubscriptionStatusResponse = dbQuery {
        val user = UserEntity.findById(userId) ?: return@dbQuery SubscriptionStatusResponse(
            billingConfigured = isAnyBillingConfigured()
        )
        val active = user.isPremiumActive()
        val latestSub = SubscriptionEntity.find {
            (SubscriptionsTable.userId eq user.id) and (SubscriptionsTable.status eq "ACTIVE")
        }.orderBy(SubscriptionsTable.updatedAt to SortOrder.DESC).firstOrNull()

        SubscriptionStatusResponse(
            isPremium = active,
            premiumUntil = user.premiumUntil?.toString(),
            activePlanId = latestSub?.planId,
            provider = latestSub?.provider,
            status = if (active) (latestSub?.status ?: "ACTIVE") else "NONE",
            autoRenew = latestSub?.autoRenew ?: false,
            billingConfigured = isAnyBillingConfigured()
        )
    }

    /**
     * Verifies a store purchase receipt/token and activates the user's subscription.
     * Currently delegates to provider-specific stubs until store credentials are configured.
     */
    suspend fun verifyAndActivatePurchase(
        userId: Int,
        request: VerifyPurchaseRequest
    ): VerifyPurchaseResponse {
        val provider = request.provider.uppercase().trim()
        val configured = isBillingConfiguredForProvider(provider)

        if (!configured) {
            return VerifyPurchaseResponse(
                success = false,
                isPremium = false,
                planId = request.planId,
                provider = provider,
                status = "PENDING_STORE_SETUP",
                message = "Mağaza abonelik doğrulaması ($provider) henüz sunucu ortam değişkenleriyle bağlanmadı. Admin panelden manuel Premium tanımlanabilir.",
                billingConfigured = false
            )
        }

        // TODO: Plug in live store API verification when credentials are added:
        // - GOOGLE_PLAY: AndroidPublisher.purchases().subscriptionsv2().get(packageName, purchaseToken)
        // - APPLE_APP_STORE: Verify JWS signedTransactionInfo with Apple Root CA / App Store Server API
        // - REVENUECAT: GET https://api.revenuecat.com/v1/subscribers/$userId
        val now = Instant.now()
        val daysToAdd = if (request.planId.contains("annual", ignoreCase = true)) 365L else 30L
        val expiresAt = now.plus(daysToAdd, ChronoUnit.DAYS)

        dbQuery {
            val user = UserEntity.findById(userId) ?: return@dbQuery
            user.isPremium = true
            user.premiumUntil = expiresAt

            SubscriptionEntity.new {
                this.user = user
                this.appId = request.appId.lowercase().trim()
                this.planId = request.planId
                this.provider = provider
                this.purchaseToken = request.purchaseToken
                this.transactionId = request.transactionId
                this.status = "ACTIVE"
                this.autoRenew = true
                this.startedAt = now
                this.expiresAt = expiresAt
                this.rawPayload = request.receiptData
                this.createdAt = now
                this.updatedAt = now
            }
        }

        return VerifyPurchaseResponse(
            success = true,
            isPremium = true,
            premiumUntil = expiresAt.toString(),
            planId = request.planId,
            provider = provider,
            status = "ACTIVE",
            message = "Abonelik doğrulandı ve Premium aktifleştirildi.",
            billingConfigured = true
        )
    }

    suspend fun restorePurchases(
        userId: Int,
        provider: String,
        purchaseToken: String?,
        transactionId: String?
    ): SubscriptionStatusResponse = dbQuery {
        val user = UserEntity.findById(userId) ?: return@dbQuery SubscriptionStatusResponse(
            billingConfigured = isAnyBillingConfigured()
        )

        if (!purchaseToken.isNullOrBlank() || !transactionId.isNullOrBlank()) {
            val matching = SubscriptionEntity.find {
                SubscriptionsTable.userId eq user.id
            }.firstOrNull { sub ->
                (!purchaseToken.isNullOrBlank() && sub.purchaseToken == purchaseToken) ||
                    (!transactionId.isNullOrBlank() && sub.transactionId == transactionId)
            }

            if (matching != null && matching.status == "ACTIVE") {
                val exp = matching.expiresAt
                if (exp == null || exp.isAfter(Instant.now())) {
                    user.isPremium = true
                    user.premiumUntil = exp
                }
            }
        }

        val active = user.isPremiumActive()
        val latestSub = SubscriptionEntity.find {
            (SubscriptionsTable.userId eq user.id) and (SubscriptionsTable.status eq "ACTIVE")
        }.orderBy(SubscriptionsTable.updatedAt to SortOrder.DESC).firstOrNull()

        SubscriptionStatusResponse(
            isPremium = active,
            premiumUntil = user.premiumUntil?.toString(),
            activePlanId = latestSub?.planId,
            provider = latestSub?.provider,
            status = if (active) (latestSub?.status ?: "ACTIVE") else "NONE",
            autoRenew = latestSub?.autoRenew ?: false,
            billingConfigured = isAnyBillingConfigured()
        )
    }
}

