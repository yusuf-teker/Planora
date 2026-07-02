package com.yusufteker.pulse.feature.home.presentation.home

import com.yusufteker.pulse.core.base.UiState

import com.yusufteker.pulse.shared.api.TaskDto

/**
 * UI state for the Home (Dashboard) screen.
 */
data class HomeState(
    val isLoading: Boolean = false,
    val smartInputText: String = "",
    val upcomingTasks: List<TaskDto> = emptyList(),
    val error: String? = null
) : UiState
