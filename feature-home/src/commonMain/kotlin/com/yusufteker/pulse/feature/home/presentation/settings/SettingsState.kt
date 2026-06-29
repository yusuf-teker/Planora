package com.yusufteker.pulse.feature.home.presentation.settings

import com.yusufteker.pulse.core.base.UiState
import com.yusufteker.pulse.core.preferences.ThemeColor

data class SettingsState(
    val isDarkMode: Boolean = false,
    val themeColor: ThemeColor = ThemeColor.DEFAULT
) : UiState
