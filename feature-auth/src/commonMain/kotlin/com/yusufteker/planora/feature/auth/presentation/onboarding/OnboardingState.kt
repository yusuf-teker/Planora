package com.yusufteker.planora.feature.auth.presentation.onboarding

import com.yusufteker.planora.core.base.UiState

/**
 * UI state for the Onboarding screen.
 */
data class OnboardingState(
    val currentPage: Int = 0,
    val totalPages: Int = 3
) : UiState
