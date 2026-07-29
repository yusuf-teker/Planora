package com.yusufteker.planora.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.yusufteker.planora.MainActivity
import com.yusufteker.planora.R
import com.yusufteker.planora.core.database.PlanoraDatabase
import com.yusufteker.planora.shared.api.TaskStatus
import com.yusufteker.planora.shared.api.TaskType
import io.github.aakira.napier.Napier
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar
import androidx.core.net.toUri
import androidx.core.graphics.toColorInt

/**
 * BroadcastReceiver that processes 09:00, 12:00, and 16:00 daily digest alarms.
 *
 * Reads uncompleted tasks directly from local SQLDelight [PlanoraDatabase],
 * generates styled notifications, and attaches deep link URLs for opening
 * the earliest/most urgent task in the app.
 */
class DailyDigestReceiver : BroadcastReceiver(), KoinComponent {

    companion object {
        const val CHANNEL_ID_DIGEST = "planora_daily_digest_channel"
    }

    private val database: PlanoraDatabase by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DailyDigestScheduler.ACTION_DAILY_DIGEST) return

        val digestType = intent.getIntExtra(DailyDigestScheduler.EXTRA_DIGEST_TYPE, DailyDigestScheduler.DIGEST_TYPE_MORNING)
        Napier.d("DailyDigestReceiver triggered with type=$digestType", tag = "DailyDigestReceiver")

        try {
            val allEntities = database.planoraDatabaseQueries.getAllTasks().executeAsList()
            // Only top-level uncompleted entities
            val topLevelUncompleted = allEntities.filter { entity ->
                entity.status != TaskStatus.COMPLETED.name && entity.parentId == null
            }
            val uncompletedTasks = topLevelUncompleted.filter { entity ->
                entity.type == TaskType.TASK.name
            }

            val nowCalendar = Calendar.getInstance()

            // Start of today (00:00:00.000)
            val startOfToday = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            // 09:00 AM today (09:00:00.000)
            val nineAmToday = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            // End of today (23:59:59.999)
            val endOfToday = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            // 3 days ago (Start of D-3)
            val startOfThreeDaysAgo = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -3)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            when (digestType) {
                DailyDigestScheduler.DIGEST_TYPE_MORNING -> {
                    // 1. Today's uncompleted tasks
                    val todayTasks = uncompletedTasks.filter { task ->
                        task.startTime in startOfToday..endOfToday
                    }

                    if (todayTasks.isNotEmpty()) {
                        val earliestTask = todayTasks.minByOrNull { it.startTime }
                        val title = context.getString(R.string.digest_morning_title)
                        val body = context.getString(R.string.digest_morning_body_tasks, todayTasks.size, earliestTask?.title ?: "")
                        val deepLinkUrl = earliestTask?.let { "planora://share/task?taskId=${it.id}" }
                        showDigestNotification(context, title, body, deepLinkUrl, notificationId = 2001)
                    } else {
                        // 2. If no tasks, check events today starting at/after 09:00
                        val todayEventsAfterNine = topLevelUncompleted.filter { entity ->
                            entity.type == TaskType.EVENT.name && entity.startTime in nineAmToday..endOfToday
                        }.sortedBy { it.startTime }

                        if (todayEventsAfterNine.isNotEmpty()) {
                            val eventTitlesStr = todayEventsAfterNine.joinToString(" ➔ ") { it.title }
                            val title = context.getString(R.string.digest_morning_title)
                            val body = context.getString(R.string.digest_morning_body_events, eventTitlesStr)
                            val earliestEvent = todayEventsAfterNine.firstOrNull()
                            val deepLinkUrl = earliestEvent?.let { "planora://share/task?taskId=${it.id}" }
                            showDigestNotification(context, title, body, deepLinkUrl, notificationId = 2001)
                        } else {
                            // 3. No tasks or events
                            val title = context.getString(R.string.digest_morning_title)
                            val body = context.getString(R.string.digest_morning_body_empty)
                            showDigestNotification(context, title, body, null, notificationId = 2001)
                        }
                    }
                }

                DailyDigestScheduler.DIGEST_TYPE_OVERDUE -> {
                    // Uncompleted tasks from the last 3 days (excluding today)
                    val overdueTasks = uncompletedTasks.filter { task ->
                        task.startTime in startOfThreeDaysAgo until startOfToday
                    }

                    if (overdueTasks.isNotEmpty()) {
                        val urgentTask = overdueTasks.minByOrNull { it.startTime }
                        val title = context.getString(R.string.notification_overdue_title)
                        val body = context.getString(R.string.digest_evening_body_overdue, overdueTasks.size)
                        val deepLinkUrl = urgentTask?.let { "planora://share/task?taskId=${it.id}" }
                        showDigestNotification(context, title, body, deepLinkUrl, notificationId = 2002)
                    }
                }

                DailyDigestScheduler.DIGEST_TYPE_AFTERNOON -> {
                    // Remaining uncompleted tasks for today
                    val remainingTodayTasks = uncompletedTasks.filter { task ->
                        task.startTime in startOfToday..endOfToday
                    }

                    if (remainingTodayTasks.isNotEmpty()) {
                        val nextTask = remainingTodayTasks.minByOrNull { it.startTime }
                        val title = context.getString(R.string.digest_afternoon_title)
                        val body = context.getString(R.string.digest_afternoon_body_remaining, remainingTodayTasks.size)
                        val deepLinkUrl = nextTask?.let { "planora://share/task?taskId=${it.id}" }
                        showDigestNotification(context, title, body, deepLinkUrl, notificationId = 2003)
                    }
                }
            }

        } catch (e: Exception) {
            Napier.e("Failed to process daily digest notification: ${e.message}", e, tag = "DailyDigestReceiver")
        }
    }

    private fun showDigestNotification(
        context: Context,
        title: String,
        body: String,
        deepLinkUrl: String?,
        notificationId: Int
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val soundUri =
            "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.planora_chime}".toUri()

        ensureChannel(context, notificationManager, soundUri)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            deepLinkUrl?.let { data = it.toUri() }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_DIGEST)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor("#7C4DFF".toColorInt())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(body)
                    .setSummaryText("Planora ✨")
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 120, 80, 120))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun ensureChannel(context: Context, notificationManager: NotificationManager, soundUri: Uri) {
        val audioAttributes = android.media.AudioAttributes.Builder()
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
            .build()

        val channelName = context.getString(R.string.notification_channel_digest_name)
        val channelDesc = context.getString(R.string.notification_channel_default_desc)

        val channel = NotificationChannel(
            CHANNEL_ID_DIGEST,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = channelDesc
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 120, 80, 120)
            enableLights(true)
            lightColor = "#7C4DFF".toColorInt()
            setSound(soundUri, audioAttributes)
        }
        notificationManager.createNotificationChannel(channel)
    }
}
