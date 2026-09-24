package com.yusufteker.planora.feature.home.presentation.settings

import com.yusufteker.planora.core.base.UiState
import com.yusufteker.planora.core.preferences.ThemeColor

/**
 * Target slot for theme color customization (Primary or Accent/Secondary).
 */
enum class ThemeColorTarget {
    PRIMARY,
    SECONDARY
}

data class SettingsState(
    val isDarkMode: Boolean = false,
    val themeColor: ThemeColor = ThemeColor.DEFAULT,
    val secondaryThemeColor: ThemeColor = ThemeColor.GREEN,
    val activeThemeTab: ThemeColorTarget = ThemeColorTarget.PRIMARY,
    val isPremium: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val errorMessage: String? = null
) : UiState

