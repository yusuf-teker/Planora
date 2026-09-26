package com.yusufteker.planora.core.calendar

import androidx.compose.runtime.Composable

/**
 * Interface representing a platform-specific calendar permission requester.
 */
interface CalendarPermissionLauncher {
    /**
     * Launches the system permission request prompt or handles the authorization flow.
     */
    fun launch()
}

/**
 * Remembers a cross-platform launcher for requesting calendar read permissions.
 *
 * On Android, uses [androidx.activity.compose.rememberLauncherForActivityResult]
 * with [androidx.activity.result.contract.ActivityResultContracts.RequestPermission]
 * targeting `android.permission.READ_CALENDAR`.
 *
 * On iOS, uses EventKit's `EKEventStore.requestAccessToEntityType`.
 *
 * @param onResult Callback invoked with the permission outcome (true if granted, false otherwise).
 */
@Composable
expect fun rememberCalendarPermissionLauncher(
    onResult: (Boolean) -> Unit
): CalendarPermissionLauncher
