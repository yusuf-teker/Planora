package com.yusufteker.planora.core.reminder

import com.yusufteker.planora.shared.api.ItemDetails
import com.yusufteker.planora.shared.api.TaskDto
import com.yusufteker.planora.shared.api.TaskStatus
import com.yusufteker.planora.shared.api.TaskType
import io.github.aakira.napier.Napier
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDateComponents
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
            val deepLink = userInfo["deepLink"] as? String
            val taskId = userInfo["taskId"] as? String
            val url = deepLink ?: taskId?.let { "planora://share/task?taskId=$it" }
            if (!url.isNullOrEmpty()) {
                Napier.d("iOS Notification tapped: url=$url", tag = TAG)
                com.yusufteker.planora.core.navigation.DeepLinkManager.emitLink(url)
            }
            withCompletionHandler()
        }
    }

    init {
        center.delegate = delegate
        requestAuthorization()
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
        val uncompletedTasks = tasks.filter { it.status != TaskStatus.COMPLETED }
        val now = com.yusufteker.planora.core.utils.getCurrentTimeMs()

        // 1. Morning Digest (09:00)
        val todayTasks = uncompletedTasks.filter { task ->
            val baseTime = (task.specificDetails as? ItemDetails.Task)?.deadline ?: task.startTime
            baseTime in (now - 12 * 3600 * 1000L)..(now + 24 * 3600 * 1000L)
        }
        if (todayTasks.isNotEmpty()) {
            val earliestTask = todayTasks.minByOrNull { (it.specificDetails as? ItemDetails.Task)?.deadline ?: it.startTime }
            scheduleCalendarDigest(
                identifier = "planora_digest_morning",
                hour = 9,
                title = "☀️ Günaydın!",
                body = "Bugün tamamlanması gereken ${todayTasks.size} görevin bulunuyor.",
                deepLinkUrl = earliestTask?.let { "planora://share/task?taskId=${it.id}" }
            )
        }

        // 2. Overdue Check (12:00)
        val overdueTasks = uncompletedTasks.filter { task ->
            val baseTime = (task.specificDetails as? ItemDetails.Task)?.deadline ?: task.startTime
            baseTime in (now - 3 * 24 * 3600 * 1000L) until now
        }
        if (overdueTasks.isNotEmpty()) {
            val urgentTask = overdueTasks.minByOrNull { (it.specificDetails as? ItemDetails.Task)?.deadline ?: it.startTime }
            scheduleCalendarDigest(
                identifier = "planora_digest_overdue",
                hour = 12,
                title = "⚠️ Geciken Görev Uyarısı!",
                body = "Son 3 gün içinde henüz tamamlanmamış ${overdueTasks.size} görevin bulunuyor.",
                deepLinkUrl = urgentTask?.let { "planora://share/task?taskId=${it.id}" }
            )
        }

        // 3. Afternoon Check (16:00)
        if (todayTasks.isNotEmpty()) {
            val nextTask = todayTasks.minByOrNull { (it.specificDetails as? ItemDetails.Task)?.deadline ?: it.startTime }
            scheduleCalendarDigest(
                identifier = "planora_digest_afternoon",
                hour = 16,
                title = "🌆 Akşamüstü Kontrolü",
                body = "Günün bitimine yaklaşırken: Bugün tamamlanması gereken ${todayTasks.size} görevin henüz bitmedi.",
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
        val style = notificationStyleFor(taskType)

        val timeText = when {
            reminderMinutes == 0 -> "Şimdi"
            reminderMinutes % 1440 == 0 -> "${reminderMinutes / 1440} gün sonra"
            reminderMinutes % 60 == 0 -> "${reminderMinutes / 60} saat sonra"
            else -> "$reminderMinutes dakika sonra"
        }

        val content = UNMutableNotificationContent().apply {
            setTitle("${style.emoji} ${style.titlePrefix}")
            setSubtitle(taskTitle)
            setBody("$taskTitle · $timeText")
            setSound(UNNotificationSound.defaultSound())
            setThreadIdentifier(style.threadId)
            setCategoryIdentifier(style.categoryId)
            setUserInfo(mapOf("taskId" to taskId))
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
}

private data class NotificationStyle(
    val emoji: String,
    val titlePrefix: String,
    val threadId: String,
    val categoryId: String
)

private fun notificationStyleFor(taskType: TaskType): NotificationStyle {
    return when (taskType) {
        TaskType.TASK -> NotificationStyle(
            emoji = "✅",
            titlePrefix = "Görev Hatırlatıcı",
            threadId = "planora_task_thread",
            categoryId = "PLANORA_TASK_CATEGORY"
        )

        TaskType.EVENT -> NotificationStyle(
            emoji = "📅",
            titlePrefix = "Etkinlik Hatırlatıcı",
            threadId = "planora_event_thread",
            categoryId = "PLANORA_EVENT_CATEGORY"
        )

        else -> NotificationStyle(
            emoji = "⏰",
            titlePrefix = "Hatırlatıcı",
            threadId = "planora_general_thread",
            categoryId = "PLANORA_GENERAL_CATEGORY"
        )
    }
}