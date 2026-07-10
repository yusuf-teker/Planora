package com.yusufteker.pulse.shared.api

import kotlinx.serialization.Serializable

/**
 * Represent the data sent by the client when attempting to login.
 */
@Serializable
data class AuthRequest(
    val identifier: String,
    val password: String
)

/**
 * Represent the data sent by the client when registering a new account.
 */
@Serializable
data class RegisterRequest(
    val name: String,
    val username: String,
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

/**
 * The response sent back from the server containing the user's profile information.
 */
@Serializable
data class UserProfileResponse(
    val id: Int,
    val name: String,
    val username: String,
    val email: String,
    val avatarId: String,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val postsCount: Int = 0,
    val isFollowedByMe: Boolean = false,
    val followRequestStatus: String? = null
)

/**
 * The response sent back from the server when requesting a list of follow requests.
 */
@Serializable
data class FollowRequestResponse(
    val id: Int,
    val requesterId: Int,
    val requesterName: String,
    val requesterUsername: String,
    val requesterAvatarId: String,
    val status: String
)

/**
 * The response sent back from the server when searching for users.
 */
@Serializable
data class SearchUsersResponse(
    val users: List<UserProfileResponse>
)
