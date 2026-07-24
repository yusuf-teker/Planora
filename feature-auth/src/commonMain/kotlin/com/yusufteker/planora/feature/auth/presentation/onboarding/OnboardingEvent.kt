package com.yusufteker.planora.feature.auth.presentation.onboarding

import com.yusufteker.planora.core.base.UiEvent

/**
 * UI events for the Onboarding screen.
 */
sealed interface OnboardingEvent : UiEvent {
    /** User tapped the next button */
    data object NextClicked : OnboardingEvent

    /** User tapped the skip button */
    data object SkipClicked : OnboardingEvent

    /** User tapped the get started button on the last page */
    data object GetStartedClicked : OnboardingEvent
}
