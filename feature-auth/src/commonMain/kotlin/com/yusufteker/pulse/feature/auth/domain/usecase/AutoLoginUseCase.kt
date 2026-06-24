package com.yusufteker.pulse.feature.auth.domain.usecase

import com.yusufteker.pulse.feature.auth.domain.repository.AuthRepository

/**
 * Checks if the user is already logged in (has valid tokens).
 * Used by Splash Screen to determine routing.
 */
class AutoLoginUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(): Boolean {
        return authRepository.hasValidSession()
    }
}
