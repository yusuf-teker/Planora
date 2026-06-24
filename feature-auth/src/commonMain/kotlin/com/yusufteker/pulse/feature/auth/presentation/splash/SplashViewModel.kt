package com.yusufteker.pulse.feature.auth.presentation.splash

import com.yusufteker.pulse.core.base.BaseViewModel

/**
 * ViewModel for the Splash screen.
 *
 * Handles the splash flow:
 * 1. Show splash animation
 * 2. Check authentication state (future)
 * 3. Navigate to Onboarding or Home
 */
class SplashViewModel : BaseViewModel<SplashState, SplashEvent, SplashEffect>(
    initialState = SplashState()
) {

    override fun onEvent(event: SplashEvent) {
        when (event) {
            is SplashEvent.AnimationCompleted -> {
                // TODO: Check auth state and navigate accordingly
                // For now, always navigate to Onboarding
                setEffect(SplashEffect.NavigateToOnboarding)
            }
        }
    }
}
