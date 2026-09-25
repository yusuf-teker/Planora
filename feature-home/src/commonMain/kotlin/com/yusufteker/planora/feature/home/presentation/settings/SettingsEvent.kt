package com.yusufteker.planora.feature.home.presentation.settings

import com.yusufteker.planora.core.base.UiEvent
import com.yusufteker.planora.core.preferences.ThemeColor

sealed interface SettingsEvent : UiEvent {
    data object BackClicked : SettingsEvent
    data class DarkModeToggled(val enabled: Boolean) : SettingsEvent
    data class ThemeTabSelected(val target: ThemeColorTarget) : SettingsEvent
    data class ThemeColorSelected(val color: ThemeColor) : SettingsEvent
    data object LogoutClicked : SettingsEvent
    data object TrashClicked : SettingsEvent
    data object AnalyticsClicked : SettingsEvent
    data object PlanComparisonClicked : SettingsEvent
    data object DeleteAccountClicked : SettingsEvent
    data object DeleteAccountConfirmed : SettingsEvent
    data object DeleteAccountDismissed : SettingsEvent
    /** Triggered when the user taps "Export Tasks" in Settings. */
    data object ExportTasksClicked : SettingsEvent
    data class SetExportSheetVisible(val visible: Boolean) : SettingsEvent
    data class ExportTasksWithFormat(val format: com.yusufteker.planora.core.export.ExportFormat) : SettingsEvent
}

