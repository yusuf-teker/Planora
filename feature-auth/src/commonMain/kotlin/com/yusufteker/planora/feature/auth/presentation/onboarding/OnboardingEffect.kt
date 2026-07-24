package com.yusufteker.planora.feature.auth.presentation.onboarding

import com.yusufteker.planora.core.base.UiEffect

/**
 * Side effects for the Onboarding screen.
 */
sealed interface OnboardingEffect : UiEffect {
    /** Navigate to the Login screen */
    data object NavigateToLogin : OnboardingEffect

    /** Navigate to the Home screen */
    data object NavigateToHome : OnboardingEffect
}
