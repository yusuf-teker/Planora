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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
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

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun getPlansForApp(appId: String, offer: String? = null): List<SubscriptionPlan> {
        val normalized = appId.lowercase().trim()
        val isHealth = normalized == "healthtrack" || normalized == "health"
        val prefix = if (isHealth) "healthtrack" else "planora"
        val isSpecialOffer = !offer.isNullOrBlank()

        val annualPlan = if (isSpecialOffer) {
            SubscriptionPlan(
                id = "${prefix}_premium_annual_offer",
                name = "Özel Yıllık Fırsat",
                priceText = "249.99 ₺ / Yıl",
                priceAmount = 249.99,
                period = SubscriptionPeriod.ANNUAL,
                discountPercentage = 48,
                isPopular = true,
                isLimitedTimeOffer = true,
                originalPriceText = "299.99 ₺",
                offerBadgeText = "🔥 SINIRLI SÜRELİ FIRSAT",
                storeOfferId = "limited_annual_offer",
                description = "Aylık sadece 20.80 ₺ (%48 Tasarruf)"
            )
        } else {
            SubscriptionPlan(
                id = "${prefix}_premium_annual",
                name = "Yıllık",
                priceText = "299.99 ₺ / Yıl",
                priceAmount = 299.99,
                period = SubscriptionPeriod.ANNUAL,
                discountPercentage = 37,
                isPopular = true,
                isLimitedTimeOffer = false,
                originalPriceText = null,
                offerBadgeText = null,
                storeOfferId = null,
                description = "Aylık sadece 25.00 ₺ (%37 Tasarruf)"
            )
        }

        val monthlyPlan = SubscriptionPlan(
            id = "${prefix}_premium_monthly",
            name = "Aylık",
            priceText = "39.99 ₺ / Ay",
            priceAmount = 39.99,
            period = SubscriptionPeriod.MONTHLY,
            discountPercentage = 0,
            isPopular = false,
            isLimitedTimeOffer = false,
            storeOfferId = null,
            description = "İstediğiniz zaman iptal edebilirsiniz"
        )

        return listOf(annualPlan, monthlyPlan)
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

        val cleanToken = request.purchaseToken?.trim()?.takeIf { it.isNotBlank() }
        val cleanTxId = request.transactionId?.trim()?.takeIf { it.isNotBlank() }
        if (cleanToken == null && cleanTxId == null && request.receiptData.isNullOrBlank()) {
            return VerifyPurchaseResponse(
                success = false,
                isPremium = false,
                planId = request.planId,
                provider = provider,
                status = "INVALID_RECEIPT",
                message = "Satın alma makbuzu veya işlem kimliği eksik.",
                billingConfigured = true
            )
        }

        // Prevent cross-account replay of the same purchaseToken or transactionId
        val alreadyClaimedByOtherUser = dbQuery {
            SubscriptionEntity.find {
                SubscriptionsTable.userId neq userId
            }.any { existing ->
                (cleanToken != null && existing.purchaseToken == cleanToken) ||
                    (cleanTxId != null && existing.transactionId == cleanTxId)
            }
        }
        if (alreadyClaimedByOtherUser) {
            return VerifyPurchaseResponse(
                success = false,
                isPremium = false,
                planId = request.planId,
                provider = provider,
                status = "TOKEN_ALREADY_CLAIMED",
                message = "Bu satın alma makbuzu zaten başka bir hesapla ilişkilendirilmiş.",
                billingConfigured = true
            )
        }

        val verificationResult = verifyWithStoreProvider(userId, provider, request)
        if (!verificationResult.isValid) {
            return VerifyPurchaseResponse(
                success = false,
                isPremium = false,
                planId = request.planId,
                provider = provider,
                status = "VERIFICATION_FAILED",
                message = verificationResult.reason ?: "Mağaza makbuz doğrulaması başarısız oldu.",
                billingConfigured = true
            )
        }

        val now = Instant.now()
        val daysToAdd = if (request.planId.contains("annual", ignoreCase = true)) 365L else 30L
        var resolvedExpiresAt = now.plus(daysToAdd, ChronoUnit.DAYS)

        dbQuery {
            val user = UserEntity.findById(userId) ?: return@dbQuery
            val baseTime = user.premiumUntil?.takeIf { it.isAfter(now) } ?: now
            resolvedExpiresAt = verificationResult.verifiedExpiresAt ?: baseTime.plus(daysToAdd, ChronoUnit.DAYS)
            user.isPremium = true
            user.premiumUntil = resolvedExpiresAt

            val existingSameToken = SubscriptionEntity.find {
                SubscriptionsTable.userId eq user.id
            }.firstOrNull { sub ->
                (cleanToken != null && sub.purchaseToken == cleanToken) ||
                    (cleanTxId != null && sub.transactionId == cleanTxId)
            }

            if (existingSameToken != null) {
                existingSameToken.status = "ACTIVE"
                existingSameToken.expiresAt = resolvedExpiresAt
                existingSameToken.updatedAt = now
            } else {
                SubscriptionEntity.new {
                    this.user = user
                    this.appId = request.appId.lowercase().trim()
                    this.planId = request.planId
                    this.provider = provider
                    this.purchaseToken = cleanToken
                    this.transactionId = cleanTxId
                    this.status = "ACTIVE"
                    this.autoRenew = true
                    this.startedAt = now
                    this.expiresAt = resolvedExpiresAt
                    this.rawPayload = request.receiptData
                    this.createdAt = now
                    this.updatedAt = now
                }
            }
        }

        return VerifyPurchaseResponse(
            success = true,
            isPremium = true,
            premiumUntil = resolvedExpiresAt.toString(),
            planId = request.planId,
            provider = provider,
            status = "ACTIVE",
            message = "Abonelik doğrulandı ve Premium aktifleştirildi.",
            billingConfigured = true
        )
    }

    private data class StoreVerificationResult(
        val isValid: Boolean,
        val verifiedExpiresAt: Instant? = null,
        val reason: String? = null
    )

    private fun verifyWithStoreProvider(
        userId: Int,
        provider: String,
        request: VerifyPurchaseRequest
    ): StoreVerificationResult {
        return when (provider) {
            "REVENUECAT" -> verifyWithRevenueCat(userId)
            "GOOGLE_PLAY", "APPLE_APP_STORE", "APPLE_PAY" -> {
                // Fail closed until native Google Play Developer API / Apple App Store Server API verification client is wired
                StoreVerificationResult(
                    isValid = false,
                    reason = "$provider canlı makbuz doğrulama istemcisi henüz yapılandırılmadı; sahte aktivasyon engellendi."
                )
            }
            else -> StoreVerificationResult(isValid = false, reason = "Desteklenmeyen ödeme sağlayıcısı: $provider")
        }
    }

    private fun verifyWithRevenueCat(userId: Int): StoreVerificationResult {
        val secretKey = System.getenv("REVENUECAT_SECRET_KEY")?.trim().orEmpty()
        if (secretKey.isBlank()) return StoreVerificationResult(false, reason = "RevenueCat anahtarı eksik.")
        return try {
            val httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.revenuecat.com/v1/subscribers/$userId"))
                .header("Authorization", "Bearer $secretKey")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build()
            val response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) {
                return StoreVerificationResult(false, reason = "RevenueCat HTTP ${response.statusCode()}")
            }
            val root = json.parseToJsonElement(response.body()).jsonObject
            val entitlements = root["subscriber"]?.jsonObject?.get("entitlements")?.jsonObject
            if (entitlements.isNullOrEmpty()) {
                return StoreVerificationResult(false, reason = "Aktif RevenueCat yetkisi bulunamadı.")
            }
            val now = Instant.now()
            val latestExpiry = entitlements.values.mapNotNull { entry ->
                val expStr = entry.jsonObject["expires_date"]?.jsonPrimitive?.content
                runCatching { expStr?.let { Instant.parse(it) } }.getOrNull()
            }.maxOrNull()

            if (latestExpiry == null || latestExpiry.isAfter(now)) {
                StoreVerificationResult(isValid = true, verifiedExpiresAt = latestExpiry)
            } else {
                StoreVerificationResult(false, reason = "RevenueCat aboneliğinin süresi dolmuş.")
            }
        } catch (e: Exception) {
            StoreVerificationResult(false, reason = "RevenueCat doğrulama hatası: ${e.message}")
        }
    }

    suspend fun handleWebhook(provider: String, payload: String) {
        val normalizedProvider = provider.uppercase().trim()
        runCatching {
            val root = json.parseToJsonElement(payload).jsonObject
            val eventObj = root["event"]?.jsonObject ?: root
            val eventType = eventObj["type"]?.jsonPrimitive?.content?.uppercase().orEmpty()
            val appUserId = eventObj["app_user_id"]?.jsonPrimitive?.content?.toIntOrNull()
            val transactionId = eventObj["transaction_id"]?.jsonPrimitive?.content
                ?: eventObj["original_transaction_id"]?.jsonPrimitive?.content

            val isRevokedOrExpired = eventType.contains("EXPIR") ||
                eventType.contains("REVOK") ||
                eventType.contains("REFUND") ||
                eventType.contains("CANCEL")

            if (isRevokedOrExpired && (appUserId != null || !transactionId.isNullOrBlank())) {
                val now = Instant.now()
                dbQuery {
                    val targetSub = if (!transactionId.isNullOrBlank()) {
                        SubscriptionEntity.find { SubscriptionsTable.transactionId eq transactionId }.firstOrNull()
                    } else null

                    val targetUser = targetSub?.user ?: appUserId?.let { UserEntity.findById(it) }
                    if (targetSub != null && (eventType.contains("EXPIR") || eventType.contains("REVOK") || eventType.contains("REFUND"))) {
                        targetSub.status = if (eventType.contains("REFUND") || eventType.contains("REVOK")) "REVOKED" else "EXPIRED"
                        targetSub.autoRenew = false
                        targetSub.updatedAt = now
                    } else if (targetSub != null && eventType.contains("CANCEL")) {
                        targetSub.autoRenew = false
                        targetSub.updatedAt = now
                    }
                    if (targetUser != null && (eventType.contains("EXPIR") || eventType.contains("REVOK") || eventType.contains("REFUND"))) {
                        targetUser.isPremium = false
                        targetUser.premiumUntil = now
                    }
                }
            }
            println("[SubscriptionService] Processed webhook provider=$normalizedProvider eventType=$eventType")
        }.onFailure { err ->
            println("[SubscriptionService] Webhook parse error for $normalizedProvider: ${err.message}")
        }
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

