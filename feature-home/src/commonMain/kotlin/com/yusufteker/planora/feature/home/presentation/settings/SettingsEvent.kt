package com.yusufteker.planora.feature.home.presentation.settings

import com.yusufteker.planora.core.base.UiEvent
import com.yusufteker.planora.core.preferences.ThemeColor

sealed interface SettingsEvent : UiEvent {
    data object BackClicked : SettingsEvent
    data class DarkModeToggled(val enabled: Boolean) : SettingsEvent
    data class ThemeColorSelected(val color: ThemeColor) : SettingsEvent
    data object LogoutClicked : SettingsEvent
    data object DeleteAccountClicked : SettingsEvent
    data object DeleteAccountConfirmed : SettingsEvent
    data object DeleteAccountDismissed : SettingsEvent
}

