package com.yusufteker.pulse.feature.home.presentation.home

import com.yusufteker.pulse.core.base.UiEvent

/**
 * UI events for the Home screen.
 */
sealed interface HomeEvent : UiEvent {
    data object ProfileClicked : HomeEvent
    data object SettingsClicked : HomeEvent
    data object RefreshRequested : HomeEvent
}
