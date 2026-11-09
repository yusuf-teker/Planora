package com.yusufteker.planora.core.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.EventKit.EKEntityType
import platform.EventKit.EKEventStore
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

/**
 * iOS actual implementation of [rememberCalendarPermissionLauncher].
 *
 * Prompts the user for calendar authorization (Full Access on iOS 17+, Authorized on iOS <17).
 * If permission was previously denied, restricted, or limited to Write-Only, opens the iOS Settings page.
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

                // 3L = Authorized (<iOS 17) or FullAccess (iOS 17+)
                if (status == 3L) {
                    requestRemindersIfNeeded()
                    onResult(true)
                } else if (status == 1L || status == 2L || status == 4L) {
                    // 1L = Restricted, 2L = Denied, 4L = WriteOnly (Add Events Only)
                    // The iOS system prompt will not re-appear; open Settings so user can enable Full Access.
                    val settingsUrl = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
                    if (settingsUrl != null && UIApplication.sharedApplication.canOpenURL(settingsUrl)) {
                        UIApplication.sharedApplication.openURL(settingsUrl)
                    }
                    CoroutineScope(Dispatchers.Main).launch {
                        onResult(false)
                    }
                } else {
                    // Status is 0L (Not Determined) - Request Full Access
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
