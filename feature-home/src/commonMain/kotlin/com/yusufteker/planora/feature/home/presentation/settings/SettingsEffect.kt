package com.yusufteker.planora.feature.home.presentation.settings

import com.yusufteker.planora.core.base.UiEffect

sealed interface SettingsEffect : UiEffect {
    data object NavigateBack : SettingsEffect
    data object NavigateToLogin : SettingsEffect
}
