package com.yusufteker.planora.feature.home.presentation.settings

import com.yusufteker.planora.core.base.UiEffect
import org.jetbrains.compose.resources.StringResource

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
    /** Triggers the platform share sheet with the CSV export (Premium-only). */
    data object ExportTasks : SettingsEffect
    /** Shows a snackbar notification with a localized message. */
    data class ShowSnackbar(val messageRes: StringResource) : SettingsEffect
}
