package com.yusufteker.pulse.feature.home.presentation.home

import com.yusufteker.pulse.core.base.UiState
import com.yusufteker.pulse.core.utils.TimelineViewOption

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
    val isFilterSheetVisible: Boolean = false,
    val selectedCalendarDate: kotlinx.datetime.LocalDate? = null,
    val visibleCalendarMonth: kotlinx.datetime.LocalDate? = null,
    val isPreferencesLoading: Boolean = false,
    val hasLoadedTasks: Boolean = false,
    val accessibleUsers: List<AccessibleUser> = emptyList(),
    val selectedSharedUserIds: Set<Int> = emptySet(),
    val sharedTasksByUser: Map<Int, List<TaskDto>> = emptyMap(),
    val selectedSharedTask: TaskDto? = null
) : UiState

data class AccessibleUser(
    val userId: Int,
    val name: String,
    val username: String,
    val avatarId: String,
    val color: String
)

data class TimelineFilterOptions(
    val showOnlyNextRecurring: Boolean = true,
    val showCompleted: Boolean = true
)

