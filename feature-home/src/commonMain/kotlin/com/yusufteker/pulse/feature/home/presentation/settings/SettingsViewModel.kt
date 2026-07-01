package com.yusufteker.pulse.feature.home.presentation.settings

import com.yusufteker.pulse.core.base.BaseViewModel
import com.yusufteker.pulse.core.preferences.SessionPreferences
import com.yusufteker.pulse.core.preferences.ThemePreferences
import kotlinx.coroutines.flow.first

import com.yusufteker.pulse.core.database.PulseDatabase

class SettingsViewModel(
    private val themePreferences: ThemePreferences,
    private val sessionPreferences: SessionPreferences,
    private val database: PulseDatabase
) : BaseViewModel<SettingsState, SettingsEvent, SettingsEffect>(
    initialState = SettingsState()
) {

    init {
        launch {
            val isDark = themePreferences.isDarkMode.first() ?: false
            val color = themePreferences.themeColor.first()
            setState { copy(isDarkMode = isDark, themeColor = color) }
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

            is SettingsEvent.ThemeColorSelected -> {
                setState { copy(themeColor = event.color) }
                launch {
                    themePreferences.setThemeColor(event.color)
                }
            }

            is SettingsEvent.LogoutClicked -> {
                launch {
                    database.pulseDatabaseQueries.clearAll()
                    sessionPreferences.clearSession()
                    setEffect(SettingsEffect.NavigateToLogin)
                }
            }
        }
    }
}
