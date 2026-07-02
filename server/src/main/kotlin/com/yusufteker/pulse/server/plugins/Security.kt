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
        jwt("auth-jwt") { // Use "auth-jwt" as the name for this authentication provider
            realm = "Pulsy Server"
            verifier( // Configure the JWT verifier with the secret, issuer, and audience
                JWT

                    //JWT.create() JWT ilk oluştruludgudune içine USER ID EMAIL VE SECRET Bilgisi koyuluyor
                    //    .withClaim("userId", 1)
                    //    .withClaim("email", "yusuf@gmail.com")
                    //    .sign(Algorithm.HMAC256(secret))
                    //   "accessToken": "eyJ...eyJ...abc123" gibi birşeyi clienta döner
                    // client her istek attığında
                    //GET /profile
                    //Authorization:
                    //Bearer eyJ...eyJ...abc123 gönderir

                    .require(Algorithm.HMAC256(secret)) //  HMAC256 algorithm
                    .withAudience("pulse-client") // Bu tokenın hangi client için geçerli olduğunu belirler. Örneğin, birden fazla client varsa (web, mobile), her biri için farklı audience belirlenebilir.
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
