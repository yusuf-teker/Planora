package com.yusufteker.pulse.feature.home.presentation.home

import com.yusufteker.pulse.core.base.UiState

/**
 * UI state for the Home screen.
 */
data class HomeState(
    val isLoading: Boolean = false
) : UiState
