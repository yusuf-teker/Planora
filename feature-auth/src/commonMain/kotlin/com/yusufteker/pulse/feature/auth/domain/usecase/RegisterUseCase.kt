package com.yusufteker.pulse.feature.auth.domain.usecase

import com.yusufteker.pulse.feature.auth.domain.repository.AuthRepository
import com.yusufteker.pulse.shared.api.AuthResponse
import com.yusufteker.pulse.shared.api.RegisterRequest

/**
 * Handles the business logic for registering a new user.
 */
class RegisterUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(request: RegisterRequest): Result<AuthResponse> {
        if (request.name.length < 3) {
            return Result.failure(IllegalArgumentException("Name must be at least 3 characters long."))
        }
        if (!request.email.contains("@")) {
            return Result.failure(IllegalArgumentException("Invalid email format."))
        }
        if (request.password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }
        
        return authRepository.register(request)
    }
}
