package com.yusufteker.pulse.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.yusufteker.pulse.MainActivity
import com.yusufteker.pulse.core.reminder.AndroidReminderManager
import com.yusufteker.pulse.shared.api.TaskType
import io.github.aakira.napier.Napier

/**
 * BroadcastReceiver that fires when a scheduled reminder alarm triggers.
 *
 * Shows a styled local notification depending on the task type
 * (TASK, EVENT, or general/NOTE), each with its own channel, icon and color.
 */
class ReminderAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID_TASK = "pulse_reminder_task_channel"
        const val CHANNEL_ID_EVENT = "pulse_reminder_event_channel"
        const val CHANNEL_ID_GENERAL = "pulse_reminder_general_channel"

        const val CHANNEL_NAME_TASK = "Görev Hatırlatıcıları"
        const val CHANNEL_NAME_EVENT = "Etkinlik Hatırlatıcıları"
        const val CHANNEL_NAME_GENERAL = "Genel Hatırlatıcılar"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AndroidReminderManager.ACTION_REMINDER) return

        val taskId = intent.getStringExtra(AndroidReminderManager.EXTRA_TASK_ID) ?: return
        val taskTitle = intent.getStringExtra(AndroidReminderManager.EXTRA_TASK_TITLE) ?: "Görev"
        val reminderMinutes = intent.getIntExtra(AndroidReminderManager.EXTRA_REMINDER_MINUTES, 0)
        val taskType = intent.getStringExtra(AndroidReminderManager.EXTRA_TASK_TYPE)
            ?.let { runCatching { TaskType.valueOf(it) }.getOrNull() }
            ?: TaskType.NOTE

        Napier.d(
            "Reminder fired: taskId=$taskId, title=$taskTitle, minutes=$reminderMinutes, type=$taskType",
            tag = "ReminderReceiver"
        )

        showNotification(context, taskId, taskTitle, reminderMinutes, taskType)
    }

    private fun showNotification(
        context: Context,
        taskId: String,
        taskTitle: String,
        reminderMinutes: Int,
        taskType: TaskType
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val style = notificationStyleFor(taskType)

        ensureChannel(notificationManager, style.channelId, style.channelName, style.channelColor)

        val timeText = when (reminderMinutes) {
            0 -> "Şimdi"
            else -> "$reminderMinutes dakika sonra"
        }
        val notificationTitle = "${style.emoji} ${style.titlePrefix}"
        val notificationBody = "$taskTitle · $timeText"

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("taskId", taskId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            tapIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, style.channelId)
            .setSmallIcon(style.smallIcon)
            .setColor(style.channelColor)
            .setContentTitle(notificationTitle)
            .setContentText(notificationBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationBody))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        val notificationId = taskId.hashCode()
        notificationManager.notify(notificationId, notification)
    }

    private fun ensureChannel(
        notificationManager: NotificationManager,
        channelId: String,
        channelName: String,
        color: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = channelName
                enableVibration(true)
                enableLights(true)
                lightColor = color
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private data class NotificationStyle(
        val channelId: String,
        val channelName: String,
        val emoji: String,
        val titlePrefix: String,
        val smallIcon: Int,
        val channelColor: Int
    )

    private fun notificationStyleFor(taskType: TaskType): NotificationStyle {
        return when (taskType) {
            TaskType.TASK -> NotificationStyle(
                channelId = CHANNEL_ID_TASK,
                channelName = CHANNEL_NAME_TASK,
                emoji = "✅",
                titlePrefix = "Görev Hatırlatıcı",
                smallIcon = android.R.drawable.ic_menu_agenda,
                channelColor = Color.parseColor("#4CAF50") // yeşil
            )
            TaskType.EVENT -> NotificationStyle(
                channelId = CHANNEL_ID_EVENT,
                channelName = CHANNEL_NAME_EVENT,
                emoji = "📅",
                titlePrefix = "Etkinlik Hatırlatıcı",
                smallIcon = android.R.drawable.ic_menu_my_calendar,
                channelColor = Color.parseColor("#2196F3") // mavi
            )
            else -> NotificationStyle(
                channelId = CHANNEL_ID_GENERAL,
                channelName = CHANNEL_NAME_GENERAL,
                emoji = "⏰",
                titlePrefix = "Hatırlatıcı",
                smallIcon = android.R.drawable.ic_dialog_info,
                channelColor = Color.parseColor("#9E9E9E") // gri
            )
        }
    }
}