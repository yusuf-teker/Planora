package com.yusufteker.planora.feature.home.presentation.settings

import com.yusufteker.planora.core.base.BaseViewModel
import com.yusufteker.planora.core.preferences.SessionPreferences
import com.yusufteker.planora.core.preferences.ThemePreferences
import com.yusufteker.planora.feature.home.domain.repository.PlanRepository
import kotlinx.coroutines.flow.first

import com.yusufteker.planora.core.database.PlanoraDatabase
import com.yusufteker.planora.core.database.clearAll

class SettingsViewModel(
    private val themePreferences: ThemePreferences,
    private val sessionPreferences: SessionPreferences,
    private val database: PlanoraDatabase,
    private val planRepository: PlanRepository
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
                    database.planoraDatabaseQueries.clearAll()
                    sessionPreferences.clearSession()
                    setEffect(SettingsEffect.NavigateToLogin)
                }
            }

            is SettingsEvent.DeleteAccountClicked -> {
                setState { copy(showDeleteConfirmDialog = true) }
            }

            is SettingsEvent.DeleteAccountDismissed -> {
                setState { copy(showDeleteConfirmDialog = false) }
            }

            is SettingsEvent.DeleteAccountConfirmed -> {
                setState { copy(showDeleteConfirmDialog = false, isDeletingAccount = true) }
                launch {
                    val result = planRepository.deleteAccount()
                    if (result.isSuccess) {
                        setEffect(SettingsEffect.NavigateToLogin)
                    } else {
                        setState { copy(isDeletingAccount = false, errorMessage = result.exceptionOrNull()?.message) }
                    }
                }
            }
        }
    }
}


