package com.yusufteker.pulse.shared.api

import kotlinx.serialization.Serializable

/**
 * Represent the data sent by the client when attempting to login.
 */
@Serializable
data class AuthRequest(
    val email: String,
    val password: String
)

/**
 * Represent the data sent by the client when registering a new account.
 */
@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)

/**
 * The response sent back from the server containing tokens upon successful auth.
 * 
 * - accessToken: Short-lived token sent in the Authorization header.
 * - refreshToken: Long-lived token used to get a new accessToken when it expires.
 */
@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val userId: Int,
    val name: String,
    val avatarId: String
)

/**
 * The data sent to the server to update the user's profile.
 */
@Serializable
data class UpdateProfileRequest(
    val name: String,
    val avatarId: String
)

/**
 * The data sent to the server to request a new access token using a refresh token.
 */
@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)
