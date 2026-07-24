package com.yusufteker.planora.feature.home.presentation.settings

import com.yusufteker.planora.core.base.UiState
import com.yusufteker.planora.core.preferences.ThemeColor

data class SettingsState(
    val isDarkMode: Boolean = false,
    val themeColor: ThemeColor = ThemeColor.DEFAULT
) : UiState
