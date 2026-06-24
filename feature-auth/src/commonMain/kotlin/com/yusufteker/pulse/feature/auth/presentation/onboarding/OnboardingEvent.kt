package com.yusufteker.pulse.feature.auth.presentation.onboarding

import com.yusufteker.pulse.core.base.UiEvent

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
