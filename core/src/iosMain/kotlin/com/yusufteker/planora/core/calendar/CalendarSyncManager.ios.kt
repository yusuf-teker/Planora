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
 * to the user's default calendar and import events across all account calendars.
 */
@OptIn(ExperimentalForeignApi::class)
actual class CalendarSyncManager : CalendarService {

    actual override fun addToSystemCalendar(
        title: String,
        description: String?,
        location: String?,
        startTimeEpochMillis: Long,
        endTimeEpochMillis: Long?
    ): Boolean {
        return try {
            val store = EKEventStore()
            val endMillis = endTimeEpochMillis ?: (startTimeEpochMillis + 3600000L)
            val status = EKEventStore.authorizationStatusForEntityType(EKEntityType.EKEntityTypeEvent)

            val performSave = {
                val event = EKEvent.eventWithEventStore(store)
                event.title = title
                event.notes = description
                event.location = location
                event.startDate = NSDate.dateWithTimeIntervalSince1970(startTimeEpochMillis / 1000.0)
                event.endDate = NSDate.dateWithTimeIntervalSince1970(endMillis / 1000.0)

                val allCalendars = (store.calendarsForEntityType(EKEntityType.EKEntityTypeEvent) as? List<*>)
                    ?.filterIsInstance<platform.EventKit.EKCalendar>()
                    ?: emptyList()

                event.calendar = store.defaultCalendarForNewEvents
                    ?: allCalendars.firstOrNull { it.allowsContentModifications }

                if (event.calendar != null) {
                    store.saveEvent(event, span = platform.EventKit.EKSpan.EKSpanThisEvent, commit = true, error = null)
                }
            }

            // 3L = FullAccess, 4L = WriteOnly (iOS 17+)
            if (status == 3L || status == 4L) {
                performSave()
                true
            } else {
                if (store.respondsToSelector(platform.Foundation.NSSelectorFromString("requestWriteOnlyAccessToEventsWithCompletion:"))) {
                    store.requestWriteOnlyAccessToEventsWithCompletion { granted, _ ->
                        if (granted) performSave()
                    }
                } else {
                    store.requestAccessToEntityType(EKEntityType.EKEntityTypeEvent) { granted, _ ->
                        if (granted) performSave()
                    }
                }
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    actual override fun hasCalendarReadPermission(): Boolean {
        val status = EKEventStore.authorizationStatusForEntityType(EKEntityType.EKEntityTypeEvent)
        // 3L = EKAuthorizationStatusFullAccess (iOS 17+) / EKAuthorizationStatusAuthorized (iOS <17)
        return status == 3L
    }

    actual override suspend fun fetchCalendarEvents(
        startEpochMillis: Long,
        endEpochMillis: Long
    ): List<CalendarImportItem> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        val status = EKEventStore.authorizationStatusForEntityType(EKEntityType.EKEntityTypeEvent)
        println("[CalendarSyncManager] Fetching events. Authorization status: $status (FullAccess=3)")
        if (status != 3L) {
            println("[CalendarSyncManager] Full calendar read permission not granted (status=$status). Returning empty list.")
            return@withContext emptyList()
        }

        // Always instantiate a fresh EKEventStore after authorization is confirmed
        val store = EKEventStore()

        // 1. Explicitly fetch calendars to ensure all accounts (iCloud, Google, Exchange, Local) are loaded
        val allCalendars = (store.calendarsForEntityType(EKEntityType.EKEntityTypeEvent) as? List<*>)
            ?.filterIsInstance<platform.EventKit.EKCalendar>()
            ?: emptyList()

        println("[CalendarSyncManager] Found ${allCalendars.size} calendars in EventKit.")

        // Filter out birthday calendars and holiday spam, but KEEP iCloud, Exchange, Google, Local, and user calendars
        val filteredCalendars = allCalendars.filter { cal ->
            val calType = cal.type
            val calTitle = cal.title.lowercase().trim()
            val isBirthday = calType == platform.EventKit.EKCalendarType.EKCalendarTypeBirthday
            val isHolidaySpam = calTitle.startsWith("holidays in") || calTitle.startsWith("resmi tatiller") || calTitle == "birthdays" || calTitle == "doğum günleri"
            !isBirthday && !isHolidaySpam
        }

        val calendarsToQuery = if (filteredCalendars.isNotEmpty()) filteredCalendars else (allCalendars.ifEmpty { null })

        // 2. EventKit 4-year limit protection:
        // Querying intervals greater than 4 years causes EventKit predicate to silently fail and return 0 events.
        val maxIntervalSec = 3L * 365L * 86400L // 3 years max
        val queryEndMillis = if ((endEpochMillis - startEpochMillis) / 1000L > maxIntervalSec) {
            startEpochMillis + (maxIntervalSec * 1000L)
        } else {
            endEpochMillis
        }

        val startDate = NSDate.dateWithTimeIntervalSince1970(startEpochMillis / 1000.0)
        val endDate = NSDate.dateWithTimeIntervalSince1970(queryEndMillis / 1000.0)

        val predicate = store.predicateForEventsWithStartDate(startDate, endDate = endDate, calendars = calendarsToQuery)
        val rawEvents = store.eventsMatchingPredicate(predicate)
        val eventsList = mutableListOf<CalendarImportItem>()

        @Suppress("UNCHECKED_CAST")
        val ekEvents = (rawEvents as? List<*>)?.filterIsInstance<EKEvent>()
            ?: (rawEvents as? List<EKEvent>)
            ?: emptyList()

        println("[CalendarSyncManager] Found ${ekEvents.size} raw calendar events from EventKit.")

        for (ekEvent in ekEvents) {
            val cal = ekEvent.calendar
            val rawTitle = ekEvent.title
            val title = if (!rawTitle.isNullOrBlank()) rawTitle else "Untitled Event"
            val startMs = ekEvent.startDate?.let { (it.timeIntervalSince1970 * 1000.0).toLong() } ?: startEpochMillis
            val baseId = ekEvent.eventIdentifier ?: com.yusufteker.planora.core.utils.generateUUID()
            val eventId = "${baseId}_$startMs"
            val description = ekEvent.notes
            val location = ekEvent.location
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

        // 3. Fetch Apple Reminders (Tasks) with strict 4-second timeout to avoid UI hangs
        val reminderStatus = EKEventStore.authorizationStatusForEntityType(EKEntityType.EKEntityTypeReminder)
        if (reminderStatus == 3L) {
            try {
                val reminderPredicate = store.predicateForIncompleteRemindersWithDueDateStarting(
                    startDate,
                    ending = endDate,
                    calendars = null
                )
                val deferred = kotlinx.coroutines.CompletableDeferred<List<platform.EventKit.EKReminder>?>()
                store.fetchRemindersMatchingPredicate(reminderPredicate) { reminders ->
                    @Suppress("UNCHECKED_CAST")
                    deferred.complete(reminders as? List<platform.EventKit.EKReminder>)
                }
                val ekReminders = kotlinx.coroutines.withTimeoutOrNull(4000L) {
                    deferred.await()
                } ?: emptyList()

                for (rem in ekReminders) {
                    val rawTitle = rem.title
                    val title = if (!rawTitle.isNullOrBlank()) rawTitle else "Untitled Task"
                    val dueComponents = rem.dueDateComponents
                    val dueNsDate = dueComponents?.let { platform.Foundation.NSCalendar.currentCalendar.dateFromComponents(it) }
                    val dueMs = dueNsDate?.let { (it.timeIntervalSince1970 * 1000.0).toLong() } ?: startEpochMillis
                    val remId = rem.calendarItemIdentifier ?: com.yusufteker.planora.core.utils.generateUUID()
                    val eventId = "rem_${remId}_$dueMs"
                    val description = rem.notes
                    val calName = rem.calendar?.title ?: "Apple Reminders"

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
                println("[CalendarSyncManager] Error fetching reminders: ${e.message}")
            }
        }

        eventsList.distinctBy { it.id }.sortedBy { it.startTimeEpochMillis }
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
