package com.yusufteker.pulse.feature.home.presentation.home

import com.yusufteker.pulse.core.base.UiState

import com.yusufteker.pulse.shared.api.TaskDto

/**
 * UI state for the Home (Dashboard) screen.
 */
data class HomeState(
    val isLoading: Boolean = false,
    val smartInputText: String = "",
    val allFetchedTasks: List<TaskDto> = emptyList(),
    val upcomingTasks: List<TaskDto> = emptyList(),
    val error: String? = null,
    val viewOption: TimelineViewOption = TimelineViewOption.RELATIVE,
    val filterOptions: TimelineFilterOptions = TimelineFilterOptions(),
    val isFilterSheetVisible: Boolean = false
) : UiState

data class TimelineFilterOptions(
    val showOnlyNextRecurring: Boolean = true,
    val showCompleted: Boolean = true
)

enum class TimelineViewOption {
    DATE, RELATIVE
}
