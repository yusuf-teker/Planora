package com.yusufteker.pulse.server.security

import com.yusufteker.pulse.server.AppConfig
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date
import java.security.SecureRandom
import java.util.Base64

/**
 * Responsible for generating Access Tokens (JWT) and Refresh Tokens.
 */
object TokenService {
    private val secret = AppConfig.jwtSecret
    private val issuer = AppConfig.jwtIssuer
    
    // Access token valid for 15 minutes
    private const val ACCESS_TOKEN_EXPIRATION = 1000L * 60 * 15
    // Refresh token valid for 30 days
    const val REFRESH_TOKEN_EXPIRATION = 1000L * 60 * 60 * 24 * 30

    // SecureRandom instance for generating cryptographically strong refresh tokens
    private val secureRandom = SecureRandom()

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
        // Generate 32 random bytes (256 bits) and return a URL-safe base64 token without padding.
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
