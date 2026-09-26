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
        return status == 3L // EKAuthorizationStatusFullAccess / EKAuthorizationStatusAuthorized
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
            val calTitle = cal?.title?.lowercase()?.trim() ?: ""

            // Exclude Apple Holidays subscription feeds, birthdays, and holiday calendars
            val type = cal?.type
            if (type == platform.EventKit.EKCalendarType.EKCalendarTypeBirthday ||
                type == platform.EventKit.EKCalendarType.EKCalendarTypeSubscription
            ) {
                continue
            }

            val systemCalendarNames = listOf(
                "holidays", "holidays in", "türkiye'deki resmi tatiller", "türkiye'deki tatiller",
                "resmi tatiller", "tatiller", "dini bayramlar", "bayramlar", "birthdays", "doğum günleri"
            )
            if (systemCalendarNames.any { calTitle.startsWith(it) || calTitle == it }) {
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

        // Fetch Apple Reminders (Tasks) if Reminders permission is granted
        val reminderStatus = EKEventStore.authorizationStatusForEntityType(EKEntityType.EKEntityTypeReminder)
        if (reminderStatus == 3L) {
            try {
                val reminderPredicate = eventStore.predicateForIncompleteRemindersWithDueDateStarting(
                    startDate,
                    ending = endDate,
                    calendars = null
                )
                val deferred = kotlinx.coroutines.CompletableDeferred<List<platform.EventKit.EKReminder>?>()
                eventStore.fetchRemindersMatchingPredicate(reminderPredicate) { reminders ->
                    @Suppress("UNCHECKED_CAST")
                    deferred.complete(reminders as? List<platform.EventKit.EKReminder>)
                }
                val ekReminders = deferred.await() ?: emptyList()

                for (rem in ekReminders) {
                    val rawTitle = rem.title
                    val title = if (!rawTitle.isNullOrBlank()) rawTitle else "Untitled Task"
                    val eventId = rem.calendarItemIdentifier ?: com.yusufteker.planora.core.utils.generateUUID()
                    val description = rem.notes
                    val calName = rem.calendar?.title ?: "Apple Reminders"
                    val dueComponents = rem.dueDateComponents
                    val dueNsDate = dueComponents?.let { platform.Foundation.NSCalendar.currentCalendar.dateFromComponents(it) }
                    val dueMs = dueNsDate?.let { (it.timeIntervalSince1970 * 1000.0).toLong() } ?: startEpochMillis

                    eventsList.add(
                        CalendarImportItem(
                            id = eventId,
                            title = title,
                            description = description,
                            location = null,
                            startTimeEpochMillis = dueMs,
                            endTimeEpochMillis = dueMs + 3600000L,
                            isAllDay = false,
                            calendarName = calName,
                            accountName = "Apple Reminders",
                            targetType = com.yusufteker.planora.shared.api.TaskType.TASK,
                            priority = com.yusufteker.planora.shared.api.TaskPriority.MEDIUM,
                            isSelected = true
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
