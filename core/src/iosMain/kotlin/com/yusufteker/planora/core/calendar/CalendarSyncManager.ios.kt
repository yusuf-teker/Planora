package com.yusufteker.planora.core.calendar

import kotlinx.cinterop.ExperimentalForeignApi
import platform.EventKit.EKEntityType
import platform.EventKit.EKEvent
import platform.EventKit.EKEventStore
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSince1970

/**
 * iOS implementation of [CalendarSyncManager].
 *
 * Interacts with Apple Calendar via EventKit framework to add events directly
 * to the user's default calendar.
 */
@OptIn(ExperimentalForeignApi::class)
actual class CalendarSyncManager {

    private val eventStore = EKEventStore()

    actual fun addToSystemCalendar(
        title: String,
        description: String?,
        location: String?,
        startTimeEpochMillis: Long,
        endTimeEpochMillis: Long?
    ): Boolean {
        return try {
            val endMillis = endTimeEpochMillis ?: (startTimeEpochMillis + 3600000L)
            
            eventStore.requestAccessToEntityType(EKEntityType.EKEntityTypeEvent) { granted, error ->
                if (granted && error == null) {
                    val event = EKEvent.eventWithEventStore(eventStore)
                    event.title = title
                    event.notes = description
                    event.location = location
                    event.startDate = NSDate.dateWithTimeIntervalSince1970(startTimeEpochMillis / 1000.0)
                    event.endDate = NSDate.dateWithTimeIntervalSince1970(endMillis / 1000.0)
                    event.calendar = eventStore.defaultCalendarForNewEvents

                    eventStore.saveEvent(event, span = platform.EventKit.EKSpan.EKSpanThisEvent, commit = true, error = null)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
