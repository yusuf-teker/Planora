package com.yusufteker.planora.core.calendar

import androidx.compose.runtime.Composable

/**
 * Platform launcher for authenticating and fetching tasks from Google Tasks.
 */
interface GoogleTasksLauncher {
    /**
     * Initiates the Google Sign-In and Google Tasks authorization flow.
     */
    fun launch()
}

/**
 * Creates and remembers a [GoogleTasksLauncher] that handles the OAuth flow and returns
 * either a list of imported [CalendarImportItem] tasks or an error message.
 *
 * @param onResult Callback invoked with the list of retrieved tasks (or null) and an optional error message.
 */
@Composable
expect fun rememberGoogleTasksLauncher(
    onResult: (List<CalendarImportItem>?, String?) -> Unit
): GoogleTasksLauncher
