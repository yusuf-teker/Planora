package com.yusufteker.pulse.feature.auth.domain.repository

import com.yusufteker.pulse.shared.api.AuthRequest
import com.yusufteker.pulse.shared.api.AuthResponse
import com.yusufteker.pulse.shared.api.RegisterRequest

/**
 * Domain Layer interface for Authentication.
 * This abstracts away the network layer from the ViewModels and UseCases.
 */
interface AuthRepository {
    /**
     * Attempts to login with email and password.
     */
    suspend fun login(request: AuthRequest): Result<AuthResponse>

    /**
     * Attempts to register a new user.
     */
    suspend fun register(request: RegisterRequest): Result<AuthResponse>

    /**
     * Checks if the user currently has saved tokens locally.
     */
    suspend fun hasValidSession(): Boolean

    /**
     * Clears local session tokens.
     */
    suspend fun logout()

    /**
     * Updates the user's profile on the server.
     */
    suspend fun updateProfile(name: String, avatarId: String): Result<Unit>

    /**
     * Fetches the user's profile from the server and updates local session.
     */
    suspend fun fetchMyProfile(): Result<Unit>
}
