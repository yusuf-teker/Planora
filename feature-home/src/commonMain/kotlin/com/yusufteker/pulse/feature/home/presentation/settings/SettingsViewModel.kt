package com.yusufteker.pulse.feature.home.presentation.settings

import com.yusufteker.pulse.core.base.BaseViewModel
import com.yusufteker.pulse.core.preferences.ThemePreferences
import kotlinx.coroutines.flow.first

class SettingsViewModel(
    private val themePreferences: ThemePreferences
) : BaseViewModel<SettingsState, SettingsEvent, SettingsEffect>(
    initialState = SettingsState()
) {

    init {
        launch {
            val isDark = themePreferences.isDarkMode.first() ?: false
            setState { copy(isDarkMode = isDark) }
        }
    }

    override fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.BackClicked -> {
                setEffect(SettingsEffect.NavigateBack)
            }

            is SettingsEvent.DarkModeToggled -> {
                setState { copy(isDarkMode = event.enabled) }
                launch {
                    themePreferences.setDarkMode(event.enabled)
                }
            }

            is SettingsEvent.LogoutClicked -> {
                // TODO: Clear session and navigate to login
                setEffect(SettingsEffect.NavigateToLogin)
            }
        }
    }
}
