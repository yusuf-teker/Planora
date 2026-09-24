package com.yusufteker.planora.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.yusufteker.planora.MainActivity
import com.yusufteker.planora.core.reminder.AndroidReminderManager
import com.yusufteker.planora.shared.api.TaskType
import androidx.core.net.toUri
import androidx.core.graphics.toColorInt

/**
 * BroadcastReceiver that fires when a scheduled reminder alarm triggers.
 *
 * Shows a styled local notification depending on the task type
 * (TASK, EVENT, or general/NOTE), each with its own channel, icon and color.
 */
class ReminderAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID_TASK = "planora_reminder_task_v2"
        const val CHANNEL_ID_EVENT = "planora_reminder_event_v2"
        const val CHANNEL_ID_GENERAL = "planora_reminder_general_v2"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AndroidReminderManager.ACTION_REMINDER) return

        val taskId = intent.getStringExtra(AndroidReminderManager.EXTRA_TASK_ID) ?: return
        val defaultTitle = context.getString(com.yusufteker.planora.R.string.reminder_default_task)
        val taskTitle = intent.getStringExtra(AndroidReminderManager.EXTRA_TASK_TITLE) ?: defaultTitle
        val reminderMinutes = intent.getIntExtra(AndroidReminderManager.EXTRA_REMINDER_MINUTES, 0)
        val taskType = intent.getStringExtra(AndroidReminderManager.EXTRA_TASK_TYPE)
            ?.let { runCatching { TaskType.valueOf(it) }.getOrNull() }
            ?: TaskType.NOTE

        // Start AlarmService which rings continuously in USAGE_ALARM stream,
        // vibrates, and triggers the full-screen AlarmActivity over the lock screen.
        AlarmService.startAlarm(context, taskId, taskTitle, reminderMinutes, taskType)
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

        val style = notificationStyleFor(context, taskType)

        val soundUri =
            "${android.content.ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${com.yusufteker.planora.R.raw.planora_chime}".toUri()

        ensureChannel( notificationManager, style.channelId, style.channelName, style.channelColor, soundUri)

        val timeText = when {
            reminderMinutes == 0 -> context.getString(com.yusufteker.planora.R.string.reminder_time_now)
            reminderMinutes % 1440 == 0 -> context.getString(com.yusufteker.planora.R.string.reminder_time_days_later, reminderMinutes / 1440)
            reminderMinutes % 60 == 0 -> context.getString(com.yusufteker.planora.R.string.reminder_time_hours_later, reminderMinutes / 60)
            else -> context.getString(com.yusufteker.planora.R.string.reminder_time_minutes_later, reminderMinutes)
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
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(notificationTitle)
                    .bigText(notificationBody)
                    .setSummaryText("Planora ✨")
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 120, 80, 120))
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
        color: Int,
        soundUri: android.net.Uri
    ) {
        val audioAttributes = android.media.AudioAttributes.Builder()
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
            .build()

        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = channelName
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 120, 80, 120)
            enableLights(true)
            lightColor = color
            setSound(soundUri, audioAttributes)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private data class NotificationStyle(
        val channelId: String,
        val channelName: String,
        val emoji: String,
        val titlePrefix: String,
        val smallIcon: Int,
        val channelColor: Int
    )

    private fun notificationStyleFor(context: Context, taskType: TaskType): NotificationStyle {
        return when (taskType) {
            TaskType.TASK -> NotificationStyle(
                channelId = CHANNEL_ID_TASK,
                channelName = context.getString(com.yusufteker.planora.R.string.notification_channel_task_name),
                emoji = "✅",
                titlePrefix = context.getString(com.yusufteker.planora.R.string.reminder_title_task),
                smallIcon = com.yusufteker.planora.R.drawable.ic_notification,
                channelColor = "#10B981".toColorInt() // emerald
            )
            TaskType.EVENT -> NotificationStyle(
                channelId = CHANNEL_ID_EVENT,
                channelName = context.getString(com.yusufteker.planora.R.string.notification_channel_event_name),
                emoji = "🎉",
                titlePrefix = context.getString(com.yusufteker.planora.R.string.reminder_title_event),
                smallIcon = com.yusufteker.planora.R.drawable.ic_notification,
                channelColor = "#3B82F6".toColorInt() // blue
            )
            else -> NotificationStyle(
                channelId = CHANNEL_ID_GENERAL,
                channelName = context.getString(com.yusufteker.planora.R.string.notification_channel_general_name),
                emoji = "✨",
                titlePrefix = context.getString(com.yusufteker.planora.R.string.reminder_title_general),
                smallIcon = com.yusufteker.planora.R.drawable.ic_notification,
                channelColor = "#7C4DFF".toColorInt() // purple
            )
        }
    }
}