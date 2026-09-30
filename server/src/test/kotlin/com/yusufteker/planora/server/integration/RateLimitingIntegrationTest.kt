package com.yusufteker.planora.server.integration

import com.yusufteker.planora.server.plugins.EMAIL_OTP_RATE_LIMIT
import com.yusufteker.planora.server.plugins.configureRateLimiting
import com.yusufteker.planora.server.plugins.configureStatusPages
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Integration tests verifying token bucket rate limiting on sensitive server endpoints.
 *
 * Verifies that exceeding configured request thresholds triggers [HttpStatusCode.TooManyRequests] (429)
 * preventing brute-force and resource exhaustion attacks.
 */
class RateLimitingIntegrationTest {

    /**
     * Verifies that endpoints protected by [EMAIL_OTP_RATE_LIMIT] allow requests up to their limit (3),
     * and strictly block subsequent requests with 429 Too Many Requests.
     */
    @Test
    fun exceedingEmailOtpRateLimitShouldReturn429TooManyRequests() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            configureRateLimiting()
            configureStatusPages()
            routing {
                rateLimit(EMAIL_OTP_RATE_LIMIT) {
                    get("/test/otp-rate-limited") {
                        call.respondText("OTP Sent")
                    }
                }
            }
        }

        // İlk 3 istek limit dahilinde başarılı olmalı (limit = 3)
        for (i in 1..3) {
            val response = client.get("/test/otp-rate-limited")
            assertEquals(HttpStatusCode.OK, response.status, "Request #$i within limit should succeed")
        }

        // 4. istek limiti aştığı için 429 Too Many Requests dönmeli
        val blockedResponse = client.get("/test/otp-rate-limited")
        assertEquals(HttpStatusCode.TooManyRequests, blockedResponse.status, "4th request should be rate limited")
    }
}
