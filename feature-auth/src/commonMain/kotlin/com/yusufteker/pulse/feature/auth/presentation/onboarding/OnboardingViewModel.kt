package com.yusufteker.pulse.feature.auth.presentation.onboarding

import com.yusufteker.pulse.core.base.BaseViewModel

/**
 * ViewModel for the Onboarding screen.
 *
 * Manages onboarding page navigation and skip/get-started flows.
 */
class OnboardingViewModel : BaseViewModel<OnboardingState, OnboardingEvent, OnboardingEffect>(
    initialState = OnboardingState()
) {

    override fun onEvent(event: OnboardingEvent) {
        when (event) {
            is OnboardingEvent.NextClicked -> {
                val nextPage = currentState.currentPage + 1
                if (nextPage < currentState.totalPages) {
                    setState { copy(currentPage = nextPage) }
                } else {
                    setEffect(OnboardingEffect.NavigateToLogin)
                }
            }

            is OnboardingEvent.SkipClicked -> {
                setEffect(OnboardingEffect.NavigateToLogin)
            }

            is OnboardingEvent.GetStartedClicked -> {
                setEffect(OnboardingEffect.NavigateToLogin)
            }
        }
    }
}
