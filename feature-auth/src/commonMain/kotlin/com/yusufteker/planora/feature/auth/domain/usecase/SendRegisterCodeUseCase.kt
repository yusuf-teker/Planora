package com.yusufteker.planora.feature.auth.domain.usecase

import com.yusufteker.planora.feature.auth.domain.repository.AuthRepository

/**
 * Use case to request sending a 6-digit email verification OTP code for new user registration.
 */
class SendRegisterCodeUseCase(
    private val authRepository: AuthRepository
) {
    /**
     * Sends registration verification code request to server.
     *
     * @param email Target user email address.
     * @param username Target user username.
     * @return [Result] wrapping [Unit] if successful or error on failure.
     */
    suspend operator fun invoke(email: String, username: String): Result<Unit> {
        return authRepository.sendRegisterCode(email, username)
    }
}
