package com.yusufteker.planora.server.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import kotlin.time.Duration.Companion.seconds

/**
 * Named rate limit identifiers for scoping different route sensitivities.
 */
val AUTH_RATE_LIMIT = RateLimitName("auth_limit")
val EMAIL_OTP_RATE_LIMIT = RateLimitName("email_otp_limit")
val AI_RATE_LIMIT = RateLimitName("ai_limit")
val GENERAL_RATE_LIMIT = RateLimitName("general_limit")

/**
 * Configures the Ktor [RateLimit] plugin.
 *
 * Implements token bucket rate limiting to prevent:
 * - Denial of Service (DoS) attacks on the server
 * - Brute-force credential stuffing on authentication endpoints
 * - SMTP spam and email quota exhaustion on OTP routes
 * - Excessive cost and compute exhaustion on AI generation endpoints
 */
fun Application.configureRateLimiting() {
    install(RateLimit) {
        // 1. General API traffic (120 requests per minute per IP)
        register(GENERAL_RATE_LIMIT) {
            rateLimiter(limit = 120, refillPeriod = 60.seconds)
        }

        // 2. Auth routes like login/register (15 requests per minute to prevent credential brute forcing)
        register(AUTH_RATE_LIMIT) {
            rateLimiter(limit = 15, refillPeriod = 60.seconds)
        }

        // 3. Email sending & OTP generation (3 requests per minute to prevent mail quota depletion and spam)
        register(EMAIL_OTP_RATE_LIMIT) {
            rateLimiter(limit = 3, refillPeriod = 60.seconds)
        }

        // 4. AI endpoints (10 requests per minute to protect API limits and backend resources)
        register(AI_RATE_LIMIT) {
            rateLimiter(limit = 10, refillPeriod = 60.seconds)
        }
    }
}
