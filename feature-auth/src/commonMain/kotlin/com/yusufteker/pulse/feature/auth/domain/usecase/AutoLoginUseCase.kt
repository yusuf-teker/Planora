package com.yusufteker.pulse.feature.auth.domain.usecase

import com.yusufteker.pulse.feature.auth.domain.repository.AuthRepository

import com.yusufteker.pulse.core.preferences.SessionPreferences

/**
 * Checks if the user is already logged in (has valid tokens).
 * Used by Splash Screen to determine routing.
 */
class AutoLoginUseCase(
    private val authRepository: AuthRepository,
    private val sessionPreferences: SessionPreferences
) {
    suspend operator fun invoke(): Boolean {
        // If the app was just installed, clear secure settings (Keychain)
        // because iOS Keychain persists across app deletions!
        if (sessionPreferences.isFirstRun()) {
            sessionPreferences.clearSession()
            sessionPreferences.markAppAsRun()
            return false
        }

        val hasSession = authRepository.hasValidSession()
        if (hasSession) {
            // Fetch the latest profile from the server to keep the local DataStore up-to-date
            authRepository.fetchMyProfile()
        }
        return hasSession
    }
}
