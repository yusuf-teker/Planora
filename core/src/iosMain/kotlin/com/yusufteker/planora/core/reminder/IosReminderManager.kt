package com.yusufteker.planora.core.reminder

import com.yusufteker.planora.core.preferences.SessionPreferences
import com.yusufteker.planora.core.utils.RecurringTaskEvaluator
import com.yusufteker.planora.core.utils.getCurrentTimeMs
import com.yusufteker.planora.shared.api.ItemDetails
import com.yusufteker.planora.shared.api.RecurrenceRule
import com.yusufteker.planora.shared.api.TaskDto
import com.yusufteker.planora.shared.api.TaskStatus
import com.yusufteker.planora.shared.api.TaskType
import com.yusufteker.planora.shared.api.TaskVisibility
import io.github.aakira.napier.Napier
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import platform.Foundation.NSDateComponents
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.preferredLanguages
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationAction
import platform.UserNotifications.UNNotificationActionOptionDestructive
import platform.UserNotifications.UNNotificationActionOptionNone
import platform.UserNotifications.UNNotificationCategory
import platform.UserNotifications.UNNotificationCategoryOptionCustomDismissAction
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

/**
 * iOS implementation of [ReminderManager] using UNUserNotificationCenter.
 *
 * Guarantees that:
 * 1. Notifications are NEVER scheduled in the past or immediately triggered with fallback intervals.
 * 2. Events that have already started or ended are strictly excluded.
 * 3. In shared rooms, alarms are only scheduled for assigned members (or creator / all when unassigned).
 * 4. Daily digests do not repeat stale event entries on subsequent days (repeats = false with exact date).
 * 5. Completed tasks and folders are filtered out.
 */
@OptIn(ExperimentalForeignApi::class)
class IosReminderManager(
    private val sessionPreferences: SessionPreferences
) : ReminderManager {

    private val center = UNUserNotificationCenter.currentNotificationCenter()

    companion object {
        private const val TAG = "ReminderManager"

        private const val ACTION_SNOOZE = "PLANORA_ACTION_SNOOZE"
        private const val ACTION_DISMISS = "PLANORA_ACTION_DISMISS"

        fun generateIdentifier(taskId: String, reminderMinutes: Int): String {
            return "$taskId:$reminderMinutes"
        }
    }

    init {
        registerCategories()
        requestAuthorization()
    }

    private fun registerCategories() {
        val isTr = isTurkishLocale()
        val snoozeTitle = if (isTr) "10 Dk Ertele" else "Snooze 10m"
        val dismissTitle = if (isTr) "Kapat" else "Dismiss"

        val snoozeAction = UNNotificationAction.actionWithIdentifier(
            identifier = ACTION_SNOOZE,
            title = snoozeTitle,
            options = UNNotificationActionOptionNone
        )
        val dismissAction = UNNotificationAction.actionWithIdentifier(
            identifier = ACTION_DISMISS,
            title = dismissTitle,
            options = UNNotificationActionOptionDestructive
        )

        val taskCategory = UNNotificationCategory.categoryWithIdentifier(
            identifier = "PLANORA_TASK_CATEGORY",
            actions = listOf(snoozeAction, dismissAction),
            intentIdentifiers = emptyList<Any?>(),
            options = UNNotificationCategoryOptionCustomDismissAction
        )
        val eventCategory = UNNotificationCategory.categoryWithIdentifier(
            identifier = "PLANORA_EVENT_CATEGORY",
            actions = listOf(snoozeAction, dismissAction),
            intentIdentifiers = emptyList<Any?>(),
            options = UNNotificationCategoryOptionCustomDismissAction
        )
        val generalCategory = UNNotificationCategory.categoryWithIdentifier(
            identifier = "PLANORA_GENERAL_CATEGORY",
            actions = listOf(snoozeAction, dismissAction),
            intentIdentifiers = emptyList<Any?>(),
            options = UNNotificationCategoryOptionCustomDismissAction
        )
        center.setNotificationCategories(setOf(taskCategory, eventCategory, generalCategory))
    }

    private fun requestAuthorization() {
        center.requestAuthorizationWithOptions(
            options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        ) { granted, error ->
            if (error != null) {
                Napier.e("Notification auth error: ${error.localizedDescription}", tag = TAG)
            } else {
                Napier.d("Notification auth granted=$granted", tag = TAG)
            }
        }
    }

    override fun scheduleAllReminders(tasks: List<TaskDto>) {
        cancelAllReminders()

        val now = getCurrentTimeMs()
        val currentUserId = sessionPreferences.cachedUserIdInt
        val candidates = mutableListOf<ReminderCandidate>()

        Napier.d("Evaluating ${tasks.size} tasks for reminder scheduling (currentUserId=$currentUserId)", tag = TAG)

        tasks.forEach { task ->
            // 1. Skip completed tasks and folders
            if (task.status == TaskStatus.COMPLETED || task.type == TaskType.FOLDER) {
                return@forEach
            }

            // 2. Room duty / assignment check:
            // In shared rooms, alarms only belong to assigned users (creator or participant).
            // If participants list is empty, all room members share the event/task.
            if (task.visibility == TaskVisibility.ROOM_SHARED || task.sharedRoomIds.isNotEmpty()) {
                if (currentUserId != null) {
                    val isAssigned = task.creatorId == currentUserId ||
                            task.participants.any { it.userId == currentUserId } ||
                            task.participants.isEmpty()
                    if (!isAssigned) {
                        return@forEach
                    }
                }
            }

            // 3. Past check for Events:
            // Crucial requirement: NEVER schedule reminders for events that have already started or ended.
            if (task.type == TaskType.EVENT) {
                val eventEndTime = task.endTime ?: task.startTime
                if (eventEndTime <= now || task.startTime <= now) {
                    return@forEach
                }
            }

            if (task.reminders.isEmpty()) {
                return@forEach
            }

            // 4. Handle recurring vs single tasks
            val ruleStr = task.recurrenceRule
            if (task.isRecurring && ruleStr != null) {
                val rule = try {
                    Json.decodeFromString<RecurrenceRule>(ruleStr)
                } catch (e: Exception) {
                    null
                }

                if (rule != null) {
                    // Generate next occurrences in the future (next 14 days)
                    val occurrences = RecurringTaskEvaluator.generateOccurrences(
                        startTimeMs = task.startTime,
                        rule = rule,
                        rangeStartMs = now,
                        rangeEndMs = now + 14 * 24 * 3600 * 1000L
                    )

                    occurrences.forEach { occurrenceMs ->
                        if (task.type == TaskType.EVENT && occurrenceMs <= now) {
                            return@forEach
                        }
                        task.reminders.forEach { reminderMinutes ->
                            val triggerTime = occurrenceMs - (reminderMinutes * 60_000L)
                            if (triggerTime > now + 1_000L) {
                                candidates.add(
                                    ReminderCandidate(
                                        identifier = "${task.id}_${occurrenceMs}_$reminderMinutes",
                                        triggerTimeMs = triggerTime,
                                        taskId = task.id,
                                        taskTitle = task.title,
                                        reminderMinutes = reminderMinutes,
                                        taskType = task.type,
                                        sharedRoomIds = task.sharedRoomIds
                                    )
                                )
                            }
                        }
                    }
                }
            } else {
                val baseTime = when (task.type) {
                    TaskType.TASK -> {
                        val taskDetails = task.specificDetails as? ItemDetails.Task
                        taskDetails?.deadline ?: task.startTime
                    }
                    else -> task.startTime
                }

                // If task deadline is already in the past, skip
                if (task.type == TaskType.TASK) {
                    val taskDeadline = (task.specificDetails as? ItemDetails.Task)?.deadline
                    if (taskDeadline != null && taskDeadline <= now) {
                        return@forEach
                    }
                }

                task.reminders.forEach { reminderMinutes ->
                    val triggerTime = baseTime - (reminderMinutes * 60_000L)
                    if (triggerTime > now + 1_000L) {
                        candidates.add(
                            ReminderCandidate(
                                identifier = generateIdentifier(task.id, reminderMinutes),
                                triggerTimeMs = triggerTime,
                                taskId = task.id,
                                taskTitle = task.title,
                                reminderMinutes = reminderMinutes,
                                taskType = task.type,
                                sharedRoomIds = task.sharedRoomIds
                            )
                        )
                    }
                }
            }
        }

        // iOS limits pending notifications to 64. Take earliest 50 to leave room for daily digests.
        val sortedToSchedule = candidates.sortedBy { it.triggerTimeMs }.take(50)
        sortedToSchedule.forEach { candidate ->
            scheduleNotification(
                identifier = candidate.identifier,
                triggerTimeMs = candidate.triggerTimeMs,
                taskId = candidate.taskId,
                taskTitle = candidate.taskTitle,
                reminderMinutes = candidate.reminderMinutes,
                taskType = candidate.taskType,
                sharedRoomIds = candidate.sharedRoomIds
            )
        }

        scheduleDailyDigests(tasks)
        Napier.d("Scheduled ${sortedToSchedule.size} local reminders out of ${candidates.size} future candidates", tag = TAG)
    }

    override fun cancelAllReminders() {
        center.removeAllPendingNotificationRequests()
        Napier.d("Cancelled all pending reminders", tag = TAG)
    }

    private fun scheduleDailyDigests(tasks: List<TaskDto>) {
        val now = getCurrentTimeMs()
        val currentUserId = sessionPreferences.cachedUserIdInt
        val timeZone = TimeZone.currentSystemDefault()
        val nowDateTime = Instant.fromEpochMilliseconds(now).toLocalDateTime(timeZone)
        val todayDate = nowDateTime.date

        // Filter active top-level uncompleted tasks/events for the current user
        val activeTasks = tasks.filter { task ->
            if (task.status == TaskStatus.COMPLETED || task.parentId != null) return@filter false
            if (task.type == TaskType.FOLDER) return@filter false
            if (task.visibility == TaskVisibility.ROOM_SHARED || task.sharedRoomIds.isNotEmpty()) {
                if (currentUserId != null) {
                    val isAssigned = task.creatorId == currentUserId ||
                            task.participants.any { it.userId == currentUserId } ||
                            task.participants.isEmpty()
                    if (!isAssigned) return@filter false
                }
            }
            true
        }

        val isTr = isTurkishLocale()
        val strings = ReminderStrings(isTr)

        // 1. Morning Digest (09:00)
        if (nowDateTime.hour < 9) {
            val todayUncompletedTasks = activeTasks.filter { task ->
                if (task.type != TaskType.TASK) return@filter false
                val baseTime = (task.specificDetails as? ItemDetails.Task)?.deadline ?: task.startTime
                val taskDate = Instant.fromEpochMilliseconds(baseTime).toLocalDateTime(timeZone).date
                taskDate == todayDate
            }

            val todayFutureEvents = activeTasks.filter { task ->
                if (task.type != TaskType.EVENT) return@filter false
                val eventEndTime = task.endTime ?: task.startTime
                if (eventEndTime <= now || task.startTime <= now) return@filter false
                val taskDateTime = Instant.fromEpochMilliseconds(task.startTime).toLocalDateTime(timeZone)
                taskDateTime.date == todayDate && taskDateTime.hour >= 9
            }.sortedBy { it.startTime }

            if (todayUncompletedTasks.isNotEmpty()) {
                val earliestTask = todayUncompletedTasks.minByOrNull { (it.specificDetails as? ItemDetails.Task)?.deadline ?: it.startTime }
                val link = earliestTask?.let { "planora://share/task?taskId=${it.id}" }
                scheduleCalendarDigest(
                    identifier = "planora_digest_morning",
                    year = todayDate.year.toLong(),
                    month = todayDate.monthNumber.toLong(),
                    day = todayDate.dayOfMonth.toLong(),
                    hour = 9,
                    title = strings.digestMorningTitle,
                    body = strings.digestMorningTasks(todayUncompletedTasks.size, earliestTask?.title ?: ""),
                    deepLinkUrl = link
                )
            } else if (todayFutureEvents.isNotEmpty()) {
                val eventTitlesStr = todayFutureEvents.joinToString(" ➔ ") { it.title }
                val earliestEvent = todayFutureEvents.firstOrNull()
                val link = earliestEvent?.let { "planora://share/event?eventId=${it.id}" }
                scheduleCalendarDigest(
                    identifier = "planora_digest_morning",
                    year = todayDate.year.toLong(),
                    month = todayDate.monthNumber.toLong(),
                    day = todayDate.dayOfMonth.toLong(),
                    hour = 9,
                    title = strings.digestMorningTitle,
                    body = strings.digestMorningEvents(eventTitlesStr),
                    deepLinkUrl = link
                )
            } else {
                scheduleCalendarDigest(
                    identifier = "planora_digest_morning",
                    year = todayDate.year.toLong(),
                    month = todayDate.monthNumber.toLong(),
                    day = todayDate.dayOfMonth.toLong(),
                    hour = 9,
                    title = strings.digestMorningTitle,
                    body = strings.digestMorningEmpty,
                    deepLinkUrl = null
                )
            }
        }

        // 2. Overdue Check (12:00)
        if (nowDateTime.hour < 12) {
            val overdueTasks = activeTasks.filter { task ->
                if (task.type != TaskType.TASK) return@filter false
                val baseTime = (task.specificDetails as? ItemDetails.Task)?.deadline ?: task.startTime
                val taskDate = Instant.fromEpochMilliseconds(baseTime).toLocalDateTime(timeZone).date
                val threeDaysAgo = Instant.fromEpochMilliseconds(now - 3 * 24 * 3600 * 1000L).toLocalDateTime(timeZone).date
                taskDate < todayDate && taskDate >= threeDaysAgo
            }
            if (overdueTasks.isNotEmpty()) {
                val urgentTask = overdueTasks.minByOrNull { (it.specificDetails as? ItemDetails.Task)?.deadline ?: it.startTime }
                val link = urgentTask?.let { "planora://share/task?taskId=${it.id}" }
                scheduleCalendarDigest(
                    identifier = "planora_digest_overdue",
                    year = todayDate.year.toLong(),
                    month = todayDate.monthNumber.toLong(),
                    day = todayDate.dayOfMonth.toLong(),
                    hour = 12,
                    title = strings.overdueTitle,
                    body = strings.overdueBody(overdueTasks.size),
                    deepLinkUrl = link
                )
            }
        }

        // 3. Afternoon Check (16:00)
        if (nowDateTime.hour < 16) {
            val todayRemainingTasks = activeTasks.filter { task ->
                if (task.type != TaskType.TASK) return@filter false
                val baseTime = (task.specificDetails as? ItemDetails.Task)?.deadline ?: task.startTime
                val taskDate = Instant.fromEpochMilliseconds(baseTime).toLocalDateTime(timeZone).date
                taskDate == todayDate
            }
            if (todayRemainingTasks.isNotEmpty()) {
                val nextTask = todayRemainingTasks.minByOrNull { (it.specificDetails as? ItemDetails.Task)?.deadline ?: it.startTime }
                val link = nextTask?.let { "planora://share/task?taskId=${it.id}" }
                scheduleCalendarDigest(
                    identifier = "planora_digest_afternoon",
                    year = todayDate.year.toLong(),
                    month = todayDate.monthNumber.toLong(),
                    day = todayDate.dayOfMonth.toLong(),
                    hour = 16,
                    title = strings.afternoonTitle,
                    body = strings.afternoonBody(todayRemainingTasks.size),
                    deepLinkUrl = link
                )
            }
        }
    }

    private fun scheduleCalendarDigest(
        identifier: String,
        year: Long,
        month: Long,
        day: Long,
        hour: Long,
        title: String,
        body: String,
        deepLinkUrl: String?
    ) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setSound(UNNotificationSound.soundNamed("planora_chime.wav"))
            setCategoryIdentifier("PLANORA_GENERAL_CATEGORY")
            deepLinkUrl?.let { setUserInfo(mapOf("deepLink" to it)) }
        }

        val dateComponents = NSDateComponents().apply {
            setYear(year)
            setMonth(month)
            setDay(day)
            setHour(hour)
            setMinute(0)
            setSecond(0)
        }

        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            dateComponents = dateComponents,
            repeats = false
        )

        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = identifier,
            content = content,
            trigger = trigger
        )

        center.addNotificationRequest(request) { error ->
            if (error != null) {
                Napier.e("Failed to schedule digest $identifier: ${error.localizedDescription}", tag = TAG)
            }
        }
    }

    private fun scheduleNotification(
        identifier: String,
        triggerTimeMs: Long,
        taskId: String,
        taskTitle: String,
        reminderMinutes: Int,
        taskType: TaskType,
        sharedRoomIds: List<String> = emptyList()
    ) {
        val now = getCurrentTimeMs()
        val secondsFromNow = (triggerTimeMs - now) / 1000.0

        // Strict future check: Do NOT schedule or immediately fire past reminders
        if (secondsFromNow < 1.0) {
            Napier.w("Skipping reminder $identifier because trigger time is in the past ($secondsFromNow s)", tag = TAG)
            return
        }

        val isTr = isTurkishLocale()
        val strings = ReminderStrings(isTr)
        val style = notificationStyleFor(taskType, strings)
        val timeText = strings.reminderTimeText(reminderMinutes)

        val roomId = sharedRoomIds.firstOrNull() ?: ""
        val deepLink = if (taskType == TaskType.EVENT) {
            if (roomId.isNotEmpty()) "planora://share/event?eventId=$taskId&roomId=$roomId"
            else "planora://share/event?eventId=$taskId"
        } else {
            if (roomId.isNotEmpty()) "planora://share/task?taskId=$taskId&roomId=$roomId"
            else "planora://share/task?taskId=$taskId"
        }

        val content = UNMutableNotificationContent().apply {
            setTitle("${style.emoji} ${style.titlePrefix}")
            setSubtitle(taskTitle)
            setBody("$taskTitle · $timeText")
            setSound(UNNotificationSound.soundNamed("planora_chime.wav"))
            setThreadIdentifier(style.threadId)
            setCategoryIdentifier(style.categoryId)
            setUserInfo(
                mapOf(
                    "taskId" to taskId,
                    "taskTitle" to taskTitle,
                    "taskType" to taskType.name,
                    "roomId" to roomId,
                    "deepLink" to deepLink
                )
            )
        }

        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
            timeInterval = secondsFromNow,
            repeats = false
        )

        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = identifier,
            content = content,
            trigger = trigger
        )

        center.addNotificationRequest(request) { error ->
            if (error != null) {
                Napier.e("Failed to schedule $identifier: ${error.localizedDescription}", tag = TAG)
            } else {
                Napier.d("Scheduled $identifier in ${secondsFromNow}s", tag = TAG)
            }
        }
    }

    override fun snoozeReminder(
        taskId: String,
        taskTitle: String,
        taskType: TaskType,
        delayMinutes: Int
    ) {
        val triggerTimeMs = getCurrentTimeMs() + (delayMinutes * 60_000L)
        val identifier = "$taskId:snooze_$delayMinutes"
        scheduleNotification(
            identifier = identifier,
            triggerTimeMs = triggerTimeMs,
            taskId = taskId,
            taskTitle = taskTitle,
            reminderMinutes = delayMinutes,
            taskType = taskType
        )
        Napier.i("Snoozed iOS reminder for $delayMinutes mins ($taskTitle)", tag = TAG)
    }
}

private data class ReminderCandidate(
    val identifier: String,
    val triggerTimeMs: Long,
    val taskId: String,
    val taskTitle: String,
    val reminderMinutes: Int,
    val taskType: TaskType,
    val sharedRoomIds: List<String>
)

private data class NotificationStyle(
    val emoji: String,
    val titlePrefix: String,
    val threadId: String,
    val categoryId: String
)

private fun notificationStyleFor(taskType: TaskType, strings: ReminderStrings): NotificationStyle {
    return when (taskType) {
        TaskType.TASK -> NotificationStyle(
            emoji = "✅",
            titlePrefix = strings.titlePrefixFor(TaskType.TASK),
            threadId = "planora_task_thread",
            categoryId = "PLANORA_TASK_CATEGORY"
        )

        TaskType.EVENT -> NotificationStyle(
            emoji = "📅",
            titlePrefix = strings.titlePrefixFor(TaskType.EVENT),
            threadId = "planora_event_thread",
            categoryId = "PLANORA_EVENT_CATEGORY"
        )

        else -> NotificationStyle(
            emoji = "⏰",
            titlePrefix = strings.titlePrefixFor(TaskType.NOTE),
            threadId = "planora_general_thread",
            categoryId = "PLANORA_GENERAL_CATEGORY"
        )
    }
}

private class ReminderStrings(val isTr: Boolean) {
    val digestMorningTitle: String = if (isTr) "🌅 Günaydın! Bugünkü Planların" else "🌅 Good Morning! Today's Plans"

    fun digestMorningTasks(count: Int, firstTitle: String): String =
        if (isTr) "Bugün $count görevin var. İlk görev: $firstTitle"
        else "You have $count task(s) today. First up: $firstTitle"

    fun digestMorningEvents(eventsStr: String): String =
        if (isTr) "Günün Etkinlikleri: $eventsStr"
        else "Today's Events: $eventsStr"

    val digestMorningEmpty: String =
        if (isTr) "Bugün henüz planlanmış görevin yok. Yeni bir hedef eklemek ister misin?"
        else "No tasks scheduled for today yet. Want to add a new goal?"

    val overdueTitle: String = if (isTr) "⚠️ Geciken Görev Uyarısı!" else "⚠️ Overdue Tasks Alert!"

    fun overdueBody(count: Int): String =
        if (isTr) "Son 3 gün içinde henüz tamamlanmamış $count görevin bulunuyor."
        else "You have $count overdue task(s) from the last 3 days."

    val afternoonTitle: String = if (isTr) "☀️ Gün Ortası Kontrolü" else "☀️ Afternoon Check-in"

    fun afternoonBody(count: Int): String =
        if (isTr) "Günün geri kalanı için $count tamamlanmamış görevin var."
        else "You have $count remaining task(s) for the rest of the day."

    fun reminderTimeText(minutes: Int): String = when {
        minutes == 0 -> if (isTr) "Şimdi" else "Now"
        minutes % 1440 == 0 -> if (isTr) "${minutes / 1440} gün sonra" else "In ${minutes / 1440} day(s)"
        minutes % 60 == 0 -> if (isTr) "${minutes / 60} saat sonra" else "In ${minutes / 60} hour(s)"
        else -> if (isTr) "$minutes dakika sonra" else "In $minutes minute(s)"
    }

    fun titlePrefixFor(taskType: TaskType): String = when (taskType) {
        TaskType.TASK -> if (isTr) "Görev Hatırlatıcı" else "Task Reminder"
        TaskType.EVENT -> if (isTr) "Etkinlik Hatırlatıcı" else "Event Reminder"
        else -> if (isTr) "Hatırlatıcı" else "Reminder"
    }
}

private fun isTurkishLocale(): Boolean {
    return try {
        val preferred = platform.Foundation.NSLocale.preferredLanguages.firstOrNull() as? String
        val current = platform.Foundation.NSLocale.currentLocale.languageCode
        val lang = preferred ?: current
        lang?.lowercase()?.startsWith("tr") == true
    } catch (e: Exception) {
        false
    }
}