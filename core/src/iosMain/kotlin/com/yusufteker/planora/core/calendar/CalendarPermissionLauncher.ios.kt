package com.yusufteker.planora.core.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.EventKit.EKEntityType
import platform.EventKit.EKEventStore

/**
 * iOS actual implementation of [rememberCalendarPermissionLauncher].
 *
 * Utilizes EventKit's [EKEventStore.requestAccessToEntityType] to prompt the user
 * for calendar authorization and callbacks onto the main thread.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberCalendarPermissionLauncher(
    onResult: (Boolean) -> Unit
): CalendarPermissionLauncher {
    return remember {
        object : CalendarPermissionLauncher {
            override fun launch() {
                val status = EKEventStore.authorizationStatusForEntityType(EKEntityType.EKEntityTypeEvent)
                if (status == 3L || status == 4L) { // Authorized or FullAccess
                    onResult(true)
                } else {
                    val store = EKEventStore()
                    store.requestAccessToEntityType(EKEntityType.EKEntityTypeEvent) { granted, _ ->
                        CoroutineScope(Dispatchers.Main).launch {
                            onResult(granted)
                        }
                    }
                }
            }
        }
    }
}
