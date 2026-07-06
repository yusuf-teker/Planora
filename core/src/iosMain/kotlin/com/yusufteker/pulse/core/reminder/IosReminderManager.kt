package com.yusufteker.pulse.core.reminder

import com.yusufteker.pulse.shared.api.ItemDetails
import com.yusufteker.pulse.shared.api.TaskDto
import com.yusufteker.pulse.shared.api.TaskType
import io.github.aakira.napier.Napier
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
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
            // Kullanıcı bildirime tıkladığında (isteğe bağlı navigation vs. buraya eklenebilir)
            Napier.d(
                "Notification tapped: ${didReceiveNotificationResponse.notification.request.identifier}",
                tag = TAG
            )
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

        val now = com.yusufteker.pulse.core.utils.getCurrentTimeMs()
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

                // Yeni
                if (triggerTime > now - 60_000L) {
                    val identifier = generateIdentifier(task.id, reminderMinutes)
                    scheduleNotification(
                        identifier = identifier,
                        triggerTimeMs = triggerTime,
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

        Napier.d("Scheduled ${identifiers.size} reminders for ${tasks.size} tasks", tag = TAG)
    }

    override fun cancelAllReminders() {
        center.removeAllPendingNotificationRequests()
        Napier.d("Cancelled all pending reminders", tag = TAG)
    }

    private fun scheduleNotification(
        identifier: String,
        triggerTimeMs: Long,
        taskTitle: String,
        reminderMinutes: Int,
        taskType: TaskType
    ) {
        val style = notificationStyleFor(taskType)

        val timeText = if (reminderMinutes > 0) "$reminderMinutes dakika sonra" else "Şimdi"

        val content = UNMutableNotificationContent().apply {
            setTitle("${style.emoji} ${style.titlePrefix}")
            setSubtitle(taskTitle)
            setBody("$taskTitle · $timeText")
            setSound(UNNotificationSound.defaultSound())
            setThreadIdentifier(style.threadId)
            setCategoryIdentifier(style.categoryId)
        }

        val secondsFromNow =
            (triggerTimeMs - com.yusufteker.pulse.core.utils.getCurrentTimeMs()) / 1000.0
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
            threadId = "pulse_task_thread",
            categoryId = "PULSE_TASK_CATEGORY"
        )

        TaskType.EVENT -> NotificationStyle(
            emoji = "📅",
            titlePrefix = "Etkinlik Hatırlatıcı",
            threadId = "pulse_event_thread",
            categoryId = "PULSE_EVENT_CATEGORY"
        )

        else -> NotificationStyle(
            emoji = "⏰",
            titlePrefix = "Hatırlatıcı",
            threadId = "pulse_general_thread",
            categoryId = "PULSE_GENERAL_CATEGORY"
        )
    }
}