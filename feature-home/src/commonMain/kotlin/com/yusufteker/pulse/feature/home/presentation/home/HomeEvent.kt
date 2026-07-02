package com.yusufteker.pulse.feature.home.presentation.home

import com.yusufteker.pulse.core.base.UiEvent

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
}
