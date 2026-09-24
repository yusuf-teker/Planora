package com.yusufteker.planora.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.yusufteker.planora.MainActivity
import com.yusufteker.planora.R
import com.yusufteker.planora.core.reminder.AndroidReminderManager
import com.yusufteker.planora.core.reminder.ReminderManager
import com.yusufteker.planora.shared.api.TaskType
import io.github.aakira.napier.Napier
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Foreground Service for ringing alarms.
 *
 * Plays continuous alarm audio in the [AudioAttributes.USAGE_ALARM] stream
 * (ringing even in silent/DND mode) and loops vibration until the user
 * dismisses or snoozes the alarm, or until the 2-minute auto-timeout.
 */
class AlarmService : Service(), KoinComponent {

    companion object {
        const val TAG = "AlarmService"
        const val CHANNEL_ID_ALARM = "planora_alarm_channel_v1"
        const val NOTIFICATION_ID = 999991

        const val ACTION_START_ALARM = "com.yusufteker.planora.ACTION_START_ALARM"
        const val ACTION_STOP_ALARM = "com.yusufteker.planora.ACTION_STOP_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.yusufteker.planora.ACTION_SNOOZE_ALARM"

        // Auto-stop after 2 minutes to prevent battery exhaustion
        private const val ALARM_TIMEOUT_MS = 120_000L

        fun startAlarm(
            context: Context,
            taskId: String,
            taskTitle: String,
            reminderMinutes: Int,
            taskType: TaskType
        ) {
            val intent = Intent(context, AlarmService::class.java).apply {
                action = ACTION_START_ALARM
                putExtra(AndroidReminderManager.EXTRA_TASK_ID, taskId)
                putExtra(AndroidReminderManager.EXTRA_TASK_TITLE, taskTitle)
                putExtra(AndroidReminderManager.EXTRA_REMINDER_MINUTES, reminderMinutes)
                putExtra(AndroidReminderManager.EXTRA_TASK_TYPE, taskType.name)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopAlarm(context: Context) {
            val intent = Intent(context, AlarmService::class.java).apply {
                action = ACTION_STOP_ALARM
            }
            context.startService(intent)
        }
    }

    private val reminderManager: ReminderManager by inject()
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())
    private val autoStopRunnable = Runnable {
        Napier.i("Alarm auto-stopped after timeout", tag = TAG)
        stopAlarmInternal()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START_ALARM -> {
                val taskId = intent.getStringExtra(AndroidReminderManager.EXTRA_TASK_ID) ?: ""
                val defaultTitle = getString(R.string.reminder_default_task)
                val taskTitle = intent.getStringExtra(AndroidReminderManager.EXTRA_TASK_TITLE) ?: defaultTitle
                val reminderMinutes = intent.getIntExtra(AndroidReminderManager.EXTRA_REMINDER_MINUTES, 0)
                val taskType = intent.getStringExtra(AndroidReminderManager.EXTRA_TASK_TYPE)
                    ?.let { runCatching { TaskType.valueOf(it) }.getOrNull() }
                    ?: TaskType.NOTE

                startAlarmInternal(taskId, taskTitle, reminderMinutes, taskType)
            }
            ACTION_STOP_ALARM -> {
                stopAlarmInternal()
            }
            ACTION_SNOOZE_ALARM -> {
                val taskId = intent.getStringExtra(AndroidReminderManager.EXTRA_TASK_ID) ?: ""
                val taskTitle = intent.getStringExtra(AndroidReminderManager.EXTRA_TASK_TITLE) ?: getString(R.string.reminder_default_task)
                val taskType = intent.getStringExtra(AndroidReminderManager.EXTRA_TASK_TYPE)
                    ?.let { runCatching { TaskType.valueOf(it) }.getOrNull() }
                    ?: TaskType.TASK

                reminderManager.snoozeReminder(taskId, taskTitle, taskType, delayMinutes = 10)
                stopAlarmInternal()
            }
        }

        return START_NOT_STICKY
    }

    private fun startAlarmInternal(
        taskId: String,
        taskTitle: String,
        reminderMinutes: Int,
        taskType: TaskType
    ) {
        ensureAlarmChannel()

        // 1. Show Foreground Notification
        val notification = buildForegroundNotification(taskId, taskTitle, reminderMinutes, taskType)
        startForeground(NOTIFICATION_ID, notification)

        // 2. Play Alarm Sound
        playAlarmSound()

        // 3. Start Vibration Pattern
        startVibration()

        // 4. Set auto-timeout
        handler.removeCallbacks(autoStopRunnable)
        handler.postDelayed(autoStopRunnable, ALARM_TIMEOUT_MS)

        Napier.i("Alarm ringing for task: $taskTitle ($taskId)", tag = TAG)
    }

    private fun playAlarmSound() {
        try {
            stopAudio()
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: "${android.content.ContentResolver.SCHEME_ANDROID_RESOURCE}://${packageName}/${R.raw.planora_chime}".toUri()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Napier.e("Failed to play alarm ringtone, falling back to chime: ${e.message}", tag = TAG)
            try {
                val chimeUri = "${android.content.ContentResolver.SCHEME_ANDROID_RESOURCE}://${packageName}/${R.raw.planora_chime}".toUri()
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(applicationContext, chimeUri)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (e2: Exception) {
                Napier.e("Failed to play fallback chime: ${e2.message}", tag = TAG)
            }
        }
    }

    private fun startVibration() {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            val pattern = longArrayOf(0, 600, 300, 600)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Napier.e("Vibration error: ${e.message}", tag = TAG)
        }
    }

    private fun stopAlarmInternal() {
        handler.removeCallbacks(autoStopRunnable)
        stopAudio()
        stopVibrator()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Napier.i("Alarm stopped successfully", tag = TAG)
    }

    private fun stopAudio() {
        mediaPlayer?.run {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }

    private fun stopVibrator() {
        vibrator?.cancel()
        vibrator = null
    }

    override fun onDestroy() {
        stopAlarmInternal()
        super.onDestroy()
    }

    private fun buildForegroundNotification(
        taskId: String,
        taskTitle: String,
        reminderMinutes: Int,
        taskType: TaskType
    ): android.app.Notification {
        val timeText = when {
            reminderMinutes == 0 -> getString(R.string.alarm_time_remaining_now)
            reminderMinutes % 1440 == 0 -> getString(R.string.alarm_time_remaining_days, reminderMinutes / 1440)
            reminderMinutes % 60 == 0 -> getString(R.string.alarm_time_remaining_hours, reminderMinutes / 60)
            else -> getString(R.string.alarm_time_remaining_minutes, reminderMinutes)
        }

        // 1. FullScreenIntent to launch AlarmActivity
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AndroidReminderManager.EXTRA_TASK_ID, taskId)
            putExtra(AndroidReminderManager.EXTRA_TASK_TITLE, taskTitle)
            putExtra(AndroidReminderManager.EXTRA_REMINDER_MINUTES, reminderMinutes)
            putExtra(AndroidReminderManager.EXTRA_TASK_TYPE, taskType.name)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            taskId.hashCode(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Action: Dismiss
        val dismissIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_STOP_ALARM
        }
        val dismissPendingIntent = PendingIntent.getService(
            this,
            taskId.hashCode() + 1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Action: Snooze 10m
        val snoozeIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_SNOOZE_ALARM
            putExtra(AndroidReminderManager.EXTRA_TASK_ID, taskId)
            putExtra(AndroidReminderManager.EXTRA_TASK_TITLE, taskTitle)
            putExtra(AndroidReminderManager.EXTRA_TASK_TYPE, taskType.name)
        }
        val snoozePendingIntent = PendingIntent.getService(
            this,
            taskId.hashCode() + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 4. Content intent (open app)
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("taskId", taskId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            taskId.hashCode() + 3,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID_ALARM)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFE53935.toInt()) // Vibrant Crimson
            .setContentTitle("⏰ $taskTitle")
            .setContentText(timeText)
            .setTicker(getString(R.string.alarm_notification_ticker))
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.alarm_action_dismiss),
                dismissPendingIntent
            )
            .addAction(
                android.R.drawable.ic_popup_reminder,
                getString(R.string.alarm_action_snooze),
                snoozePendingIntent
            )
            .build()
    }

    private fun ensureAlarmChannel() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelName = getString(R.string.notification_channel_alarm_name)
        val channelDesc = getString(R.string.notification_channel_alarm_desc)

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build()

        val channel = NotificationChannel(
            CHANNEL_ID_ALARM,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = channelDesc
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 600, 300, 600)
            enableLights(true)
            lightColor = 0xFFE53935.toInt()
            // Sound is handled directly by MediaPlayer in USAGE_ALARM stream
            setSound(null, audioAttributes)
            setBypassDnd(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)
    }
}
