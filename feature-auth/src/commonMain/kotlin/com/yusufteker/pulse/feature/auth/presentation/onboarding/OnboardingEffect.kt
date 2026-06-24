package com.yusufteker.pulse.feature.auth.presentation.onboarding

import com.yusufteker.pulse.core.base.UiEffect

/**
 * Side effects for the Onboarding screen.
 */
sealed interface OnboardingEffect : UiEffect {
    /** Navigate to the Login screen */
    data object NavigateToLogin : OnboardingEffect
}
