package com.yusufteker.pulse.server.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.yusufteker.pulse.server.AppConfig
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

/**
 * Configures the Ktor Authentication plugin to automatically validate incoming JWTs.
 */
fun Application.configureSecurity() {
    val secret = AppConfig.jwtSecret
    val issuer = AppConfig.jwtIssuer

    install(Authentication) {
        jwt("auth-jwt") {
            realm = "Pulse Server"
            verifier(
                JWT
                    .require(Algorithm.HMAC256(secret))
                    .withAudience("pulse-client")
                    .withIssuer(issuer)
                    .build()
            )
            validate { credential ->
                // Check if the JWT contains the required "userId" claim
                if (credential.payload.getClaim("userId").asInt() != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null // Token is invalid or missing claims
                }
            }
        }
    }
}
