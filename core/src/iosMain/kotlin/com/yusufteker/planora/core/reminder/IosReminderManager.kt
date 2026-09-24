package com.yusufteker.planora.core.reminder

import com.yusufteker.planora.shared.api.ItemDetails
import com.yusufteker.planora.shared.api.TaskDto
import com.yusufteker.planora.shared.api.TaskStatus
import com.yusufteker.planora.shared.api.TaskType
import io.github.aakira.napier.Napier
import kotlinx.datetime.toLocalDateTime
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDateComponents
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.preferredLanguages
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationPresentationOptionBadge
import platform.UserNotifications.UNNotificationPresentationOptionBanner
import platform.UserNotifications.UNNotificationPresentationOptionList
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject

/**
 * iOS implementation of [ReminderManager] using UNUserNotificationCenter.
 *
 * Uses UNTimeIntervalNotificationTrigger for scheduling local notifications.
 * Implements UNUserNotificationCenterDelegate so notifications are shown
 * even while the app is in foreground (default iOS behavior is to suppress them).
 */
@OptIn(ExperimentalForeignApi::class)
class IosReminderManager : ReminderManager {

    private val center = UNUserNotificationCenter.currentNotificationCenter()

    companion object {
        private const val TAG = "ReminderManager"

        private const val ACTION_SNOOZE = "PLANORA_ACTION_SNOOZE"
        private const val ACTION_DISMISS = "PLANORA_ACTION_DISMISS"

        fun generateIdentifier(taskId: String, reminderMinutes: Int): String {
            return "$taskId:$reminderMinutes"
        }
    }

    private val delegate = object : NSObject(), UNUserNotificationCenterDelegateProtocol {

        override fun userNotificationCenter(
            center: UNUserNotificationCenter,
            willPresentNotification: UNNotification,
            withCompletionHandler: (UNNotificationPresentationOptions) -> Unit
        ) {
            // Foreground'dayken de banner + ses + badge göster
            withCompletionHandler(
                UNNotificationPresentationOptionBanner or
                        UNNotificationPresentationOptionSound or
                        UNNotificationPresentationOptionBadge or
                        UNNotificationPresentationOptionList
            )
        }

        override fun userNotificationCenter(
            center: UNUserNotificationCenter,
            didReceiveNotificationResponse: UNNotificationResponse,
            withCompletionHandler: () -> Unit
        ) {
            val userInfo = didReceiveNotificationResponse.notification.request.content.userInfo
            val actionIdentifier = didReceiveNotificationResponse.actionIdentifier
            val taskId = userInfo["taskId"] as? String
            val taskTitle = userInfo["taskTitle"] as? String ?: ""
            val taskTypeStr = userInfo["taskType"] as? String
            val taskType = taskTypeStr?.let { runCatching { TaskType.valueOf(it) }.getOrNull() } ?: TaskType.TASK

            if (actionIdentifier == ACTION_SNOOZE && !taskId.isNullOrEmpty()) {
                Napier.d("iOS Notification Snooze tapped: taskId=$taskId", tag = TAG)
                snoozeReminder(taskId, taskTitle, taskType, delayMinutes = 10)
            } else if (actionIdentifier == ACTION_DISMISS) {
                Napier.d("iOS Notification Dismiss tapped: taskId=$taskId", tag = TAG)
            } else {
                val deepLink = userInfo["deepLink"] as? String
                val url = deepLink ?: taskId?.let { "planora://share/task?taskId=$it" }
                if (!url.isNullOrEmpty()) {
                    Napier.d("iOS Notification tapped: url=$url", tag = TAG)
                    com.yusufteker.planora.core.navigation.DeepLinkManager.emitLink(url)
                }
            }
            withCompletionHandler()
        }
    }

    init {
        center.delegate = delegate
        registerCategories()
        requestAuthorization()
    }

    private fun registerCategories() {
        val isTr = isTurkishLocale()
        val snoozeTitle = if (isTr) "10 Dk Ertele" else "Snooze 10m"
        val dismissTitle = if (isTr) "Kapat" else "Dismiss"

        val snoozeAction = platform.UserNotifications.UNNotificationAction.actionWithIdentifier(
            identifier = ACTION_SNOOZE,
            title = snoozeTitle,
            options = platform.UserNotifications.UNNotificationActionOptionNone
        )
        val dismissAction = platform.UserNotifications.UNNotificationAction.actionWithIdentifier(
            identifier = ACTION_DISMISS,
            title = dismissTitle,
            options = platform.UserNotifications.UNNotificationActionOptionDestructive
        )

        val taskCategory = platform.UserNotifications.UNNotificationCategory.categoryWithIdentifier(
            identifier = "PLANORA_TASK_CATEGORY",
            actions = listOf(snoozeAction, dismissAction),
            intentIdentifiers = emptyList<Any?>(),
            options = platform.UserNotifications.UNNotificationCategoryOptionCustomDismissAction
        )
        val eventCategory = platform.UserNotifications.UNNotificationCategory.categoryWithIdentifier(
            identifier = "PLANORA_EVENT_CATEGORY",
            actions = listOf(snoozeAction, dismissAction),
            intentIdentifiers = emptyList<Any?>(),
            options = platform.UserNotifications.UNNotificationCategoryOptionCustomDismissAction
        )
        center.setNotificationCategories(setOf(taskCategory, eventCategory))
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

        val now = com.yusufteker.planora.core.utils.getCurrentTimeMs()
        val identifiers = mutableListOf<String>()

        Napier.d("Scheduling reminders for ${tasks.size} tasks", tag = TAG)

        tasks.forEach { task ->
            if (task.reminders.isEmpty()) {
                Napier.d("No reminders for ${task.title}", tag = TAG)
                return@forEach
            }

            task.reminders.forEach { reminderMinutes ->
                val baseTime = when (task.type) {
                    TaskType.TASK -> {
                        val taskDetails = task.specificDetails as? ItemDetails.Task
                        taskDetails?.deadline ?: task.startTime
                    }

                    else -> task.startTime
                }

                val triggerTime = baseTime - (reminderMinutes * 60_000L)

                if (triggerTime > now - 60_000L) {
                    val identifier = generateIdentifier(task.id, reminderMinutes)
                    scheduleNotification(
                        identifier = identifier,
                        triggerTimeMs = triggerTime,
                        taskId = task.id,
                        taskTitle = task.title,
                        reminderMinutes = reminderMinutes,
                        taskType = task.type
                    )
                    identifiers.add(identifier)
                } else {
                    Napier.w(
                        "Skipping reminder. triggerTime=$triggerTime <= now=$now",
                        tag = TAG
                    )
                }
            }
        }

        scheduleDailyDigests(tasks)
        Napier.d("Scheduled ${identifiers.size} reminders for ${tasks.size} tasks", tag = TAG)
    }

    override fun cancelAllReminders() {
        center.removeAllPendingNotificationRequests()
        Napier.d("Cancelled all pending reminders", tag = TAG)
    }

    private fun scheduleDailyDigests(tasks: List<TaskDto>) {
        val topLevelUncompleted = tasks.filter { it.status != TaskStatus.COMPLETED && it.parentId == null }
        val uncompletedTasks = topLevelUncompleted.filter { it.type == TaskType.TASK }
        val now = com.yusufteker.planora.core.utils.getCurrentTimeMs()

        val timeZone = kotlinx.datetime.TimeZone.currentSystemDefault()
        val todayDate = kotlinx.datetime.Instant.fromEpochMilliseconds(now).toLocalDateTime(timeZone).date

        val isTr = isTurkishLocale()
        val strings = ReminderStrings(isTr)

        // 1. Morning Digest (09:00)
        val todayTasks = uncompletedTasks.filter { task ->
            val baseTime = (task.specificDetails as? ItemDetails.Task)?.deadline ?: task.startTime
            val taskDate = kotlinx.datetime.Instant.fromEpochMilliseconds(baseTime).toLocalDateTime(timeZone).date
            taskDate == todayDate
        }

        if (todayTasks.isNotEmpty()) {
            val earliestTask = todayTasks.minByOrNull { (it.specificDetails as? ItemDetails.Task)?.deadline ?: it.startTime }
            scheduleCalendarDigest(
                identifier = "planora_digest_morning",
                hour = 9,
                title = strings.digestMorningTitle,
                body = strings.digestMorningTasks(todayTasks.size, earliestTask?.title ?: ""),
                deepLinkUrl = earliestTask?.let { "planora://share/task?taskId=${it.id}" }
            )
        } else {
            // Check events today starting at/after 09:00 AM
            val todayEventsAfterNine = topLevelUncompleted.filter { task ->
                if (task.type != TaskType.EVENT) return@filter false
                val taskDateTime = kotlinx.datetime.Instant.fromEpochMilliseconds(task.startTime).toLocalDateTime(timeZone)
                taskDateTime.date == todayDate && taskDateTime.hour >= 9
            }.sortedBy { it.startTime }

            if (todayEventsAfterNine.isNotEmpty()) {
                val eventTitlesStr = todayEventsAfterNine.joinToString(" ➔ ") { it.title }
                val earliestEvent = todayEventsAfterNine.firstOrNull()
                scheduleCalendarDigest(
                    identifier = "planora_digest_morning",
                    hour = 9,
                    title = strings.digestMorningTitle,
                    body = strings.digestMorningEvents(eventTitlesStr),
                    deepLinkUrl = earliestEvent?.let { "planora://share/task?taskId=${it.id}" }
                )
            } else {
                scheduleCalendarDigest(
                    identifier = "planora_digest_morning",
                    hour = 9,
                    title = strings.digestMorningTitle,
                    body = strings.digestMorningEmpty,
                    deepLinkUrl = null
                )
            }
        }

        // 2. Overdue Check (12:00)
        val overdueTasks = uncompletedTasks.filter { task ->
            val baseTime = (task.specificDetails as? ItemDetails.Task)?.deadline ?: task.startTime
            val taskDate = kotlinx.datetime.Instant.fromEpochMilliseconds(baseTime).toLocalDateTime(timeZone).date
            taskDate < todayDate && taskDate >= kotlinx.datetime.LocalDate(todayDate.year, todayDate.month, todayDate.dayOfMonth).let {
                // Last 3 days
                kotlinx.datetime.Instant.fromEpochMilliseconds(now - 3 * 24 * 3600 * 1000L).toLocalDateTime(timeZone).date
            }
        }
        if (overdueTasks.isNotEmpty()) {
            val urgentTask = overdueTasks.minByOrNull { (it.specificDetails as? ItemDetails.Task)?.deadline ?: it.startTime }
            scheduleCalendarDigest(
                identifier = "planora_digest_overdue",
                hour = 12,
                title = strings.overdueTitle,
                body = strings.overdueBody(overdueTasks.size),
                deepLinkUrl = urgentTask?.let { "planora://share/task?taskId=${it.id}" }
            )
        }

        // 3. Afternoon Check (16:00)
        if (todayTasks.isNotEmpty()) {
            val nextTask = todayTasks.minByOrNull { (it.specificDetails as? ItemDetails.Task)?.deadline ?: it.startTime }
            scheduleCalendarDigest(
                identifier = "planora_digest_afternoon",
                hour = 16,
                title = strings.afternoonTitle,
                body = strings.afternoonBody(todayTasks.size),
                deepLinkUrl = nextTask?.let { "planora://share/task?taskId=${it.id}" }
            )
        }
    }

    private fun scheduleCalendarDigest(
        identifier: String,
        hour: Long,
        title: String,
        body: String,
        deepLinkUrl: String?
    ) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setSound(UNNotificationSound.defaultSound())
            deepLinkUrl?.let { setUserInfo(mapOf("deepLink" to it)) }
        }

        val dateComponents = NSDateComponents().apply {
            setHour(hour)
            setMinute(0)
        }

        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            dateComponents = dateComponents,
            repeats = true
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
        taskType: TaskType
    ) {
        val isTr = isTurkishLocale()
        val strings = ReminderStrings(isTr)
        val style = notificationStyleFor(taskType, strings)

        val timeText = strings.reminderTimeText(reminderMinutes)

        val content = UNMutableNotificationContent().apply {
            setTitle("${style.emoji} ${style.titlePrefix}")
            setSubtitle(taskTitle)
            setBody("$taskTitle · $timeText")
            setSound(UNNotificationSound.defaultSound())
            setThreadIdentifier(style.threadId)
            setCategoryIdentifier(style.categoryId)
            setUserInfo(mapOf("taskId" to taskId, "taskTitle" to taskTitle, "taskType" to taskType.name))
        }

        val secondsFromNow =
            (triggerTimeMs - com.yusufteker.planora.core.utils.getCurrentTimeMs()) / 1000.0
        if (secondsFromNow <= 0) {
            Napier.w("Trigger time already passed for $identifier, skipping", tag = TAG)
            return
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
                Napier.e(
                    "Failed to schedule $identifier: ${error.localizedDescription}",
                    tag = TAG
                )
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
        val triggerTimeMs = com.yusufteker.planora.core.utils.getCurrentTimeMs() + (delayMinutes * 60_000L)
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