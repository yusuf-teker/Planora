package com.yusufteker.planora.server.routes

import com.yusufteker.planora.server.service.SubscriptionService
import com.yusufteker.planora.shared.billing.RestorePurchaseRequest
import com.yusufteker.planora.shared.billing.VerifyPurchaseRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/**
 * Subscription, receipt verification, restore, and store webhook routes.
 */
fun Route.subscriptionRoutes() {
    route("/api/subscriptions") {
        /**
         * GET /api/subscriptions/plans?app=planora|healthtrack
         * Public endpoint returning available subscription plans and store product IDs.
         */
        get("/plans") {
            val app = call.request.queryParameters["app"] ?: "planora"
            call.respond(HttpStatusCode.OK, SubscriptionService.getPlansForApp(app))
        }

        /**
         * POST /api/subscriptions/webhook/{provider}
         * Webhook receiver for Google Play RTDN, Apple App Store Server Notifications V2, and RevenueCat.
         * Validates `X-Webhook-Secret` against `SUBSCRIPTION_WEBHOOK_SECRET` when configured.
         */
        post("/webhook/{provider}") {
            val expectedSecret = System.getenv("SUBSCRIPTION_WEBHOOK_SECRET")
            if (!expectedSecret.isNullOrBlank()) {
                val provided = call.request.headers["X-Webhook-Secret"]
                if (provided != expectedSecret) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid webhook secret"))
                    return@post
                }
            }
            val provider = call.parameters["provider"] ?: "unknown"
            val payload = call.receiveText()
            println("Subscription webhook received for provider=$provider, payloadLength=${payload.length}")
            call.respond(HttpStatusCode.OK, mapOf("received" to true, "provider" to provider))
        }

        authenticate("auth-jwt") {
            /**
             * GET /api/subscriptions/status
             * Returns the authenticated user's active subscription and premium status.
             */
            get("/status") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
                    return@get
                }
                call.respond(HttpStatusCode.OK, SubscriptionService.getStatus(userId))
            }

            /**
             * POST /api/subscriptions/verify
             * Verifies a store purchase token / receipt and activates premium.
             */
            post("/verify") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
                    return@post
                }
                val request = call.receive<VerifyPurchaseRequest>()
                val result = SubscriptionService.verifyAndActivatePurchase(userId, request)
                val statusCode = if (result.success) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
                call.respond(statusCode, result)
            }

            /**
             * POST /api/subscriptions/restore
             * Restores previous purchases for the authenticated user.
             */
            post("/restore") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
                    return@post
                }
                val request = call.receive<RestorePurchaseRequest>()
                val status = SubscriptionService.restorePurchases(
                    userId = userId,
                    provider = request.provider,
                    purchaseToken = request.purchaseToken,
                    transactionId = request.transactionId
                )
                call.respond(HttpStatusCode.OK, status)
            }
        }
    }
}

