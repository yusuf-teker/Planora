package com.yusufteker.pulse.feature.auth.presentation.onboarding

import com.yusufteker.pulse.core.base.BaseViewModel
import com.yusufteker.pulse.core.preferences.SessionPreferences
import com.yusufteker.pulse.feature.auth.domain.usecase.AutoLoginUseCase

/**
 * ViewModel for the Onboarding screen.
 *
 * Manages onboarding page navigation and skip/get-started flows.
 */
class OnboardingViewModel(
    private val sessionPreferences: SessionPreferences,
    private val autoLoginUseCase: AutoLoginUseCase
) : BaseViewModel<OnboardingState, OnboardingEvent, OnboardingEffect>(
    initialState = OnboardingState()
) {
    companion object {
        const val TOTAL_PAGES = 4
    }

    init {
        launch {
            val lastSeen = sessionPreferences.getLastSeenOverviewIndex()
            if (lastSeen < TOTAL_PAGES) {
                setState { copy(currentPage = lastSeen, totalPages = TOTAL_PAGES) }
            } else {
                finishOnboarding()
            }
        }
    }

    override fun onEvent(event: OnboardingEvent) {
        when (event) {
            is OnboardingEvent.NextClicked -> {
                val nextPage = currentState.currentPage + 1
                if (nextPage < currentState.totalPages) {
                    setState { copy(currentPage = nextPage) }
                } else {
                    finishOnboarding()
                }
            }

            is OnboardingEvent.SkipClicked -> {
                finishOnboarding()
            }

            is OnboardingEvent.GetStartedClicked -> {
                finishOnboarding()
            }
        }
    }

    private fun finishOnboarding() {
        launch {
            sessionPreferences.setLastSeenOverviewIndex(TOTAL_PAGES)
            val isLoggedIn = autoLoginUseCase()
            if (isLoggedIn) {
                setEffect(OnboardingEffect.NavigateToHome)
            } else {
                setEffect(OnboardingEffect.NavigateToLogin)
            }
        }
    }
}
