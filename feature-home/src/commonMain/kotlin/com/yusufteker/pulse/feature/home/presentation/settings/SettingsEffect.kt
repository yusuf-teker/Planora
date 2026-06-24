package com.yusufteker.pulse.feature.home.presentation.settings

import com.yusufteker.pulse.core.base.UiEffect

sealed interface SettingsEffect : UiEffect {
    data object NavigateBack : SettingsEffect
    data object NavigateToLogin : SettingsEffect
}
