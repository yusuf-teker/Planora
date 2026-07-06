package com.yusufteker.pulse.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.yusufteker.pulse.MainActivity
import com.yusufteker.pulse.core.reminder.AndroidReminderManager
import io.github.aakira.napier.Napier

/**
 * BroadcastReceiver that fires when a scheduled reminder alarm triggers.
 *
 * Reads task info from the intent extras and shows a local notification
 * on the "pulse_reminder_channel" channel.
 */
class ReminderAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "pulse_reminder_channel"
        const val CHANNEL_NAME = "Hatırlatıcılar"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AndroidReminderManager.ACTION_REMINDER) return

        val taskId = intent.getStringExtra(AndroidReminderManager.EXTRA_TASK_ID) ?: return
        val taskTitle = intent.getStringExtra(AndroidReminderManager.EXTRA_TASK_TITLE) ?: "Görev"
        val reminderMinutes = intent.getIntExtra(AndroidReminderManager.EXTRA_REMINDER_MINUTES, 0)

        Napier.d("Reminder fired: taskId=$taskId, title=$taskTitle, minutes=$reminderMinutes", tag = "ReminderReceiver")

        val notificationTitle = when (reminderMinutes) {
            0 -> "⏰ Şimdi: $taskTitle"
            else -> "⏰ $reminderMinutes dakika sonra: $taskTitle"
        }

        showNotification(context, taskId, notificationTitle, taskTitle)
    }

    private fun showNotification(
        context: Context,
        taskId: String,
        title: String,
        body: String
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Ensure channel exists (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Görev hatırlatıcı bildirimleri"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent to open the app when notification is tapped
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

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        val notificationId = taskId.hashCode()
        notificationManager.notify(notificationId, notification)
    }
}
