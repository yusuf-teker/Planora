package com.yusufteker.pulse.feature.auth.domain.usecase

import com.yusufteker.pulse.feature.auth.domain.repository.AuthRepository
import com.yusufteker.pulse.shared.api.AuthRequest
import com.yusufteker.pulse.shared.api.AuthResponse

/**
 * Handles the business logic for user login.
 */
class LoginUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(request: AuthRequest): Result<AuthResponse> {
        // ViewModel'den gelen verilerin kurallara uygunluğunu Repository'ye gitmeden test ediyoruz.
        if (request.identifier.isBlank() || request.password.isBlank()) {
            return Result.failure(IllegalArgumentException("Identifier and password cannot be empty."))
        }
        
        return authRepository.login(request)
    }
}
