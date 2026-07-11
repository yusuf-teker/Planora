package com.yusufteker.pulse.feature.home.presentation.home

import com.yusufteker.pulse.core.base.UiEvent
import com.yusufteker.pulse.core.utils.TimelineViewOption

/**
 * UI events for the Home screen.
 */
sealed interface HomeEvent : UiEvent {
    data object ProfileClicked : HomeEvent
    data object SettingsClicked : HomeEvent
    data object RefreshRequested : HomeEvent
    data class SmartInputChanged(val text: String) : HomeEvent
    data object SubmitSmartInput : HomeEvent
    data object CreateTaskClicked : HomeEvent
    data object CreateEventClicked : HomeEvent
    data class TimelineItemClicked(val task: com.yusufteker.pulse.shared.api.TaskDto) : HomeEvent
    data class ViewOptionChanged(val option: TimelineViewOption) : HomeEvent
    data class FilterOptionChanged(val filterOptions: TimelineFilterOptions) : HomeEvent
    data class ToggleFilterSheet(val isVisible: Boolean) : HomeEvent
    data class ToggleTaskCompletion(val task: com.yusufteker.pulse.shared.api.TaskDto) : HomeEvent
    data class CalendarDateSelected(val date: kotlinx.datetime.LocalDate) : HomeEvent
    data class CalendarMonthChanged(val monthStart: kotlinx.datetime.LocalDate) : HomeEvent
    data class ToggleSharedUser(val userId: Int) : HomeEvent
    data object DismissSharedTaskDetail : HomeEvent
}
