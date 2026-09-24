package com.yusufteker.planora.feature.home.presentation.settings

import com.yusufteker.planora.core.base.UiEffect

sealed interface SettingsEffect : UiEffect {
    data object NavigateBack : SettingsEffect
    data object NavigateToLogin : SettingsEffect
    /** Navigates to the Premium paywall when a locked feature is tapped. */
    data object NavigateToPremium : SettingsEffect
    /** Navigates to the Recycle Bin (Trash) screen. */
    data object NavigateToTrash : SettingsEffect
    /** Navigates to the Productivity & Insights (Analytics) screen. */
    data object NavigateToAnalytics : SettingsEffect
    /** Navigates to the Plan Comparison screen. */
    data object NavigateToPlanComparison : SettingsEffect
}
