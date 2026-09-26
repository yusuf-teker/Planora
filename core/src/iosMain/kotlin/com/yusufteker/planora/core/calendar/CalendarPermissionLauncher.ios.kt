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
                val store = EKEventStore()

                val requestRemindersIfNeeded = {
                    val remStatus = EKEventStore.authorizationStatusForEntityType(EKEntityType.EKEntityTypeReminder)
                    if (remStatus == 0L) { // Not determined
                        if (store.respondsToSelector(platform.Foundation.NSSelectorFromString("requestFullAccessToRemindersWithCompletion:"))) {
                            store.requestFullAccessToRemindersWithCompletion { _, _ -> }
                        } else {
                            store.requestAccessToEntityType(EKEntityType.EKEntityTypeReminder) { _, _ -> }
                        }
                    }
                }

                if (status == 3L) { // Authorized (iOS <17) or FullAccess (iOS 17+)
                    requestRemindersIfNeeded()
                    onResult(true)
                } else {
                    if (store.respondsToSelector(platform.Foundation.NSSelectorFromString("requestFullAccessToEventsWithCompletion:"))) {
                        store.requestFullAccessToEventsWithCompletion { granted, _ ->
                            if (granted) requestRemindersIfNeeded()
                            CoroutineScope(Dispatchers.Main).launch {
                                onResult(granted)
                            }
                        }
                    } else {
                        store.requestAccessToEntityType(EKEntityType.EKEntityTypeEvent) { granted, _ ->
                            if (granted) requestRemindersIfNeeded()
                            CoroutineScope(Dispatchers.Main).launch {
                                onResult(granted)
                            }
                        }
                    }
                }
            }
        }
    }
}
