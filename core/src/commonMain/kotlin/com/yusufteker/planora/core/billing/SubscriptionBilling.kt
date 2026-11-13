package com.yusufteker.planora.core.billing

import com.yusufteker.planora.shared.billing.PurchaseResult
import com.yusufteker.planora.shared.billing.RestorePurchaseRequest
import com.yusufteker.planora.shared.billing.SubscriptionPlan
import com.yusufteker.planora.shared.billing.SubscriptionStatusResponse
import com.yusufteker.planora.shared.billing.VerifyPurchaseRequest
import com.yusufteker.planora.shared.billing.VerifyPurchaseResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Platform store billing bridge (Google Play Billing on Android, StoreKit 2 on iOS).
 * Implement and bind via Koin when store product IDs and merchant accounts are ready.
 */
interface StoreBillingClient {
    val isStoreConnected: Boolean
    suspend fun purchase(plan: SubscriptionPlan): PurchaseResult
    suspend fun restore(): PurchaseResult
}

/**
 * Default placeholder implementation until Google Play Billing / StoreKit 2 SDK is wired.
 */
class PendingStoreBillingClient : StoreBillingClient {
    override val isStoreConnected: Boolean = false

    override suspend fun purchase(plan: SubscriptionPlan): PurchaseResult {
        return PurchaseResult(
            isSuccess = false,
            planId = plan.id,
            errorMessage = "Mağaza abonelik SDK'sı henüz bağlanmadı.",
            isSimulation = true
        )
    }

    override suspend fun restore(): PurchaseResult {
        return PurchaseResult(
            isSuccess = false,
            errorMessage = "Mağaza abonelik SDK'sı henüz bağlanmadı.",
            isSimulation = true
        )
    }
}

/**
 * Remote API client for Planora subscription plans, receipt verification, and status sync.
 */
class SubscriptionApi(private val httpClient: HttpClient) {
    suspend fun fetchPlans(appId: String = "planora"): Result<List<SubscriptionPlan>> = runCatching {
        httpClient.get("api/subscriptions/plans?app=$appId").body()
    }

    suspend fun fetchStatus(): Result<SubscriptionStatusResponse> = runCatching {
        httpClient.get("api/subscriptions/status").body()
    }

    suspend fun verifyPurchase(request: VerifyPurchaseRequest): Result<VerifyPurchaseResponse> = runCatching {
        httpClient.post("api/subscriptions/verify") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun restorePurchases(request: RestorePurchaseRequest): Result<SubscriptionStatusResponse> = runCatching {
        httpClient.post("api/subscriptions/restore") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}

