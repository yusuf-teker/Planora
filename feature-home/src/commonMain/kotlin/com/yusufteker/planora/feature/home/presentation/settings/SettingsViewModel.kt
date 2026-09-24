package com.yusufteker.planora.feature.home.presentation.settings

import com.yusufteker.planora.core.base.BaseViewModel
import com.yusufteker.planora.core.preferences.SessionPreferences
import com.yusufteker.planora.core.preferences.ThemePreferences
import com.yusufteker.planora.core.preferences.isPremiumTheme
import com.yusufteker.planora.feature.home.domain.repository.PlanRepository
import kotlinx.coroutines.flow.first

import com.yusufteker.planora.core.database.PlanoraDatabase
import com.yusufteker.planora.core.database.clearAll

/**
 * ViewModel for the Settings screen.
 *
 * Manages theme preferences (dark mode, theme color), premium status,
 * logout, and account deletion flows.
 */
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
            setState { copy(isDarkMode = isDark) }
        }
        launch {
            themePreferences.themeColor.collect { color ->
                setState { copy(themeColor = color) }
            }
        }
        launch {
            themePreferences.secondaryThemeColor.collect { secondary ->
                setState { copy(secondaryThemeColor = secondary) }
            }
        }
        // Observe premium status reactively
        launch {
            sessionPreferences.isPremiumFlow.collect { isPrem ->
                setState { copy(isPremium = isPrem) }
            }
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

            is SettingsEvent.ThemeTabSelected -> {
                setState { copy(activeThemeTab = event.target) }
            }

            is SettingsEvent.ThemeColorSelected -> {
                if (event.color.isPremiumTheme() && !state.value.isPremium) {
                    setEffect(SettingsEffect.NavigateToPremium)
                    return
                }

                if (state.value.activeThemeTab == ThemeColorTarget.PRIMARY) {
                    setState { copy(themeColor = event.color) }
                    launch {
                        themePreferences.setThemeColor(event.color)
                    }
                } else {
                    setState { copy(secondaryThemeColor = event.color) }
                    launch {
                        themePreferences.setSecondaryThemeColor(event.color)
                    }
                }
            }

            is SettingsEvent.LogoutClicked -> {
                launch {
                    database.planoraDatabaseQueries.clearAll()
                    sessionPreferences.clearSession()
                    setEffect(SettingsEffect.NavigateToLogin)
                }
            }

            is SettingsEvent.TrashClicked -> {
                setEffect(SettingsEffect.NavigateToTrash)
            }

            is SettingsEvent.AnalyticsClicked -> {
                setEffect(SettingsEffect.NavigateToAnalytics)
            }

            is SettingsEvent.PlanComparisonClicked -> {
                setEffect(SettingsEffect.NavigateToPlanComparison)
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

