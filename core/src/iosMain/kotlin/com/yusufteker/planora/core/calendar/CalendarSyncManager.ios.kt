package com.yusufteker.planora.core.calendar

import kotlinx.cinterop.ExperimentalForeignApi
import platform.EventKit.EKEntityType
import platform.EventKit.EKEvent
import platform.EventKit.EKEventStore
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970

/**
 * iOS implementation of [CalendarSyncManager].
 *
 * Interacts with Apple Calendar via EventKit framework to add events directly
 * to the user's default calendar.
 */
@OptIn(ExperimentalForeignApi::class)
actual class CalendarSyncManager : CalendarService {

    private val eventStore = EKEventStore()

    actual override fun addToSystemCalendar(
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

    actual override fun hasCalendarReadPermission(): Boolean {
        val status = EKEventStore.authorizationStatusForEntityType(EKEntityType.EKEntityTypeEvent)
        return status == 3L || status == 4L
    }

    actual override suspend fun fetchCalendarEvents(
        startEpochMillis: Long,
        endEpochMillis: Long
    ): List<CalendarImportItem> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        if (!hasCalendarReadPermission()) {
            return@withContext emptyList()
        }

        val startDate = NSDate.dateWithTimeIntervalSince1970(startEpochMillis / 1000.0)
        val endDate = NSDate.dateWithTimeIntervalSince1970(endEpochMillis / 1000.0)
        val predicate = eventStore.predicateForEventsWithStartDate(startDate, endDate = endDate, calendars = null)
        val rawEvents = eventStore.eventsMatchingPredicate(predicate)
        val eventsList = mutableListOf<CalendarImportItem>()

        @Suppress("UNCHECKED_CAST")
        val ekEvents = rawEvents as? List<EKEvent> ?: emptyList()

        for (ekEvent in ekEvents) {
            val cal = ekEvent.calendar
            val calTitle = cal?.title?.lowercase() ?: ""
            val sourceTitle = cal?.source?.title?.lowercase() ?: ""

            // Exclude Apple Holidays subscription feeds, birthdays, and holiday calendars
            val type = cal?.type
            if (type == platform.EventKit.EKCalendarType.EKCalendarTypeBirthday ||
                type == platform.EventKit.EKCalendarType.EKCalendarTypeSubscription
            ) {
                continue
            }

            val systemKeywords = listOf(
                "holiday", "tatil", "tatilleri", "bayram", "resmi tatil",
                "birthday", "doğum günü", "dogum gunu", "contacts", "rehber"
            )
            if (systemKeywords.any { calTitle.contains(it) || sourceTitle.contains(it) }) {
                continue
            }

            val rawTitle = ekEvent.title
            val title = if (!rawTitle.isNullOrBlank()) rawTitle else "Untitled Event"
            val eventId = ekEvent.eventIdentifier ?: com.yusufteker.planora.core.utils.generateUUID()
            val description = ekEvent.notes
            val location = ekEvent.location
            val startMs = ekEvent.startDate?.let { (it.timeIntervalSince1970 * 1000.0).toLong() } ?: startEpochMillis
            val endMs = ekEvent.endDate?.let { (it.timeIntervalSince1970 * 1000.0).toLong() }
            val isAllDay = ekEvent.allDay
            val calName = cal?.title
            val accName = cal?.source?.title
            val targetType = detectTargetType(title, calName)

            eventsList.add(
                CalendarImportItem(
                    id = eventId,
                    title = title,
                    description = description,
                    location = location,
                    startTimeEpochMillis = startMs,
                    endTimeEpochMillis = endMs,
                    isAllDay = isAllDay,
                    calendarName = calName,
                    accountName = accName,
                    targetType = targetType,
                    priority = com.yusufteker.planora.shared.api.TaskPriority.MEDIUM,
                    isSelected = true
                )
            )
        }

        eventsList.sortedBy { it.startTimeEpochMillis }
    }

    private fun detectTargetType(
        title: String,
        calendarName: String?
    ): com.yusufteker.planora.shared.api.TaskType {
        val t = title.lowercase().trim()
        val c = calendarName?.lowercase()?.trim() ?: ""

        val taskKeywords = listOf("task", "tasks", "görev", "görevler", "todo", "to-do", "yapılacak", "reminder", "reminders", "hatırlatıcı")
        if (taskKeywords.any { c.contains(it) }) {
            return com.yusufteker.planora.shared.api.TaskType.TASK
        }

        if (t.startsWith("[ ]") || t.startsWith("[x]") || t.startsWith("todo:") || t.startsWith("görev:") || t.startsWith("task:")) {
            return com.yusufteker.planora.shared.api.TaskType.TASK
        }

        return com.yusufteker.planora.shared.api.TaskType.EVENT
    }
}
