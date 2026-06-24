package com.yusufteker.pulse.server.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date
import java.util.UUID

/**
 * Responsible for generating Access Tokens (JWT) and Refresh Tokens.
 */
object TokenService {
    // Read the secret and issuer directly from System properties (populated from .env)
    private val secret = System.getProperty("JWT_SECRET") ?: System.getenv("JWT_SECRET") ?: "secret"
    private val issuer = System.getProperty("JWT_ISSUER") ?: System.getenv("JWT_ISSUER") ?: "pulse"
    
    // Access token valid for 15 minutes
    private const val ACCESS_TOKEN_EXPIRATION = 1000L * 60 * 15
    // Refresh token valid for 30 days
    const val REFRESH_TOKEN_EXPIRATION = 1000L * 60 * 60 * 24 * 30

    /**
     * Generates a signed JWT access token for the given user.
     * The token includes the userId as a claim so the server can identify the user.
     */
    fun generateAccessToken(userId: Int, email: String): String {
        return JWT.create()
            .withAudience("pulse-client")
            .withIssuer(issuer)
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withExpiresAt(Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION))
            .sign(Algorithm.HMAC256(secret))
    }

    /**
     * Generates a secure, random string to be used as a Refresh Token.
     * This token will be saved in the database.
     */
    fun generateRefreshToken(): String {
        return UUID.randomUUID().toString()
    }
}
