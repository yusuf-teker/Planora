package com.yusufteker.pulse.feature.home.domain.repository

/**
 * Domain Layer interface for Profile operations.
 */
interface ProfileRepository {
    /**
     * Updates the user's profile on the server.
     */
    suspend fun updateProfile(name: String, avatarId: String): Result<Unit>
}
