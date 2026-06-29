package com.yusufteker.pulse.feature.home.domain.repository

import com.yusufteker.pulse.shared.api.UserProfileResponse

/**
 * Domain Layer interface for Profile operations.
 */
interface ProfileRepository {
    /**
     * Updates the user's profile on the server.
     */
    suspend fun updateProfile(name: String, avatarId: String): Result<Unit>

    suspend fun getProfile(userId: String): Result<UserProfileResponse>

    suspend fun toggleFollow(userId: Int): Result<Unit>
}
