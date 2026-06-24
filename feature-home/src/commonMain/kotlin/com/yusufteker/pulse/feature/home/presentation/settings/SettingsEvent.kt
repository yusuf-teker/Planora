package com.yusufteker.pulse.feature.home.presentation.settings

import com.yusufteker.pulse.core.base.UiEvent
import com.yusufteker.pulse.core.preferences.ThemeColor

sealed interface SettingsEvent : UiEvent {
    data object BackClicked : SettingsEvent
    data class DarkModeToggled(val enabled: Boolean) : SettingsEvent
    data class ThemeColorSelected(val color: ThemeColor) : SettingsEvent
    data object LogoutClicked : SettingsEvent
}
