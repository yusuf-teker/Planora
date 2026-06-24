package com.yusufteker.pulse.feature.home.presentation.settings

import com.yusufteker.pulse.core.base.UiEvent

sealed interface SettingsEvent : UiEvent {
    data object BackClicked : SettingsEvent
    data class DarkModeToggled(val enabled: Boolean) : SettingsEvent
    data object LogoutClicked : SettingsEvent
}
