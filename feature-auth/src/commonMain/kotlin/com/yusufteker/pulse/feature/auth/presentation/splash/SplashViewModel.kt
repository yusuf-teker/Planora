package com.yusufteker.pulse.feature.auth.presentation.splash

import com.yusufteker.pulse.core.base.BaseViewModel
import com.yusufteker.pulse.feature.auth.domain.usecase.AutoLoginUseCase

/**
 * ViewModel for the Splash screen.
 *
 * Handles the splash flow:
 * 1. Show splash animation
 * 2. Check authentication state (future)
 * 3. Navigate to Onboarding or Home
 */
class SplashViewModel(
    private val autoLoginUseCase: AutoLoginUseCase
) : BaseViewModel<SplashState, SplashEvent, SplashEffect>(
    initialState = SplashState()
) {
    init {
        checkSession()
    }

    private fun checkSession() {
        launch {
            val isLoggedIn = autoLoginUseCase()
            if (isLoggedIn) {
                setEffect(SplashEffect.NavigateToHome)
            } else {
                setEffect(SplashEffect.NavigateToOnboarding)
            }
        }
    }

    override fun onEvent(event: SplashEvent) {
        // No longer waiting for animation
    }
}
