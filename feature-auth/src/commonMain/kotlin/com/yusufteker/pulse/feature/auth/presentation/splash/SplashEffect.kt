package com.yusufteker.pulse.feature.auth.presentation.splash

import com.yusufteker.pulse.core.base.UiEffect

/**
 * Side effects for the Splash screen.
 */
sealed interface SplashEffect : UiEffect {
    /** Navigate to the Onboarding screen */
    data object NavigateToOnboarding : SplashEffect

    /** Navigate to the Home screen (if user is already logged in) */
    data object NavigateToHome : SplashEffect

    /** Navigate to the Login screen */
    data object NavigateToLogin : SplashEffect
}
