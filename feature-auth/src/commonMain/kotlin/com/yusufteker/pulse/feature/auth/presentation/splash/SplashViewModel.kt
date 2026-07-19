package com.yusufteker.pulse.feature.auth.presentation.splash

import com.yusufteker.pulse.core.base.BaseViewModel
import com.yusufteker.pulse.feature.auth.domain.usecase.AutoLoginUseCase

import com.yusufteker.pulse.core.preferences.SessionPreferences
import com.yusufteker.pulse.feature.auth.presentation.onboarding.OnboardingViewModel

/**
 * ViewModel for the Splash screen.
 *
 * Handles the splash flow:
 * 1. Show splash animation
 * 2. Check if there are new overview screens
 * 3. Check authentication state
 * 4. Navigate to Onboarding or Home or Login
 */
class SplashViewModel(
    private val autoLoginUseCase: AutoLoginUseCase,
    private val sessionPreferences: SessionPreferences
) : BaseViewModel<SplashState, SplashEvent, SplashEffect>(
    initialState = SplashState()
) {
    init {
        checkSession()
    }

    private fun checkSession() {
        launch {
            val isLoggedIn = autoLoginUseCase()
            val lastSeen = sessionPreferences.getLastSeenOverviewIndex()
            
            // Assuming we have TOTAL_PAGES defined in OnboardingViewModel or we just use 3 for now.
            // OnboardingViewModel companion object can hold the total count.
            val totalPages = OnboardingViewModel.TOTAL_PAGES
            
            if (lastSeen < totalPages) {
                setEffect(SplashEffect.NavigateToOnboarding)
            } else if (isLoggedIn) {
                setEffect(SplashEffect.NavigateToHome)
            } else {
                setEffect(SplashEffect.NavigateToLogin)
            }
        }
    }

    override fun onEvent(event: SplashEvent) {
        // No longer waiting for animation
    }
}
