package com.yusufteker.planora

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.yusufteker.planora.core.reminder.ReminderManager
import com.yusufteker.planora.shared.api.TaskDto
import com.yusufteker.planora.shared.api.TaskStatus
import com.yusufteker.planora.shared.api.TaskType
import com.yusufteker.planora.shared.api.TaskVisibility
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue
import androidx.core.net.toUri
import androidx.core.graphics.toColorInt

class PlanoraFcmService : FirebaseMessagingService(), KoinComponent {

    private val sessionPreferences: com.yusufteker.planora.core.preferences.SessionPreferences by inject()
    private val authRepository: com.yusufteker.planora.feature.auth.domain.repository.AuthRepository by inject()
    private val planRepository: com.yusufteker.planora.feature.home.domain.repository.PlanRepository by inject()
    private val database: com.yusufteker.planora.core.database.PlanoraDatabase by inject()
    private val reminderManager: ReminderManager by inject()


    companion object {
        const val DEFAULT_CHANNEL_ID = "planora_v2_channel"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Napier.d("New FCM Token: $token", tag = "PlanoraFcmService")
        
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                // Save the token locally
                sessionPreferences.saveFcmToken(token)

                // Try to send it to backend if logged in
                if (authRepository.hasValidSession()) {
                    authRepository.registerFcmToken(token)
                }
            } catch (e: Exception) {
                Napier.e("Failed to handle new FCM token", e, tag = "PlanoraFcmService")
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Napier.d("FCM Message Received: ${message.data}", tag = "PlanoraFcmService")

        when (val type = message.data["type"]) {
            "sync_tasks" -> handleSyncTasksTrigger()
            "room_invite" -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        planRepository.getMyPendingInvitations()
                        planRepository.fetchMyRooms()
                    } catch (e: Exception) {
                        Napier.e("Failed to handle room_invite trigger", e, tag = "PlanoraFcmService")
                    }
                }
                handleGeneralNotification(message)
            }
            "room_invite_response", "room_member_removed" -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        planRepository.fetchMyRooms()
                    } catch (e: Exception) {
                        Napier.e("Failed to handle $type trigger", e, tag = "PlanoraFcmService")
                    }
                }
                handleGeneralNotification(message)
            }
            "follow_request" -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        sessionPreferences.incrementPendingFollowRequestsCount()
                    } catch (e: Exception) {
                        Napier.e("Failed to increment follow requests count", e, tag = "PlanoraFcmService")
                    }
                }
                handleGeneralNotification(message)
            }
            "calendar_request" -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        sessionPreferences.incrementPendingCalendarRequestsCount()
                    } catch (e: Exception) {
                        Napier.e("Failed to increment calendar requests count", e, tag = "PlanoraFcmService")
                    }
                }
                handleGeneralNotification(message)
            }
            "event_assignment", "task_assignment" -> {
                handleSyncTasksTrigger()
                handleGeneralNotification(message)
            }
            else -> handleGeneralNotification(message)
        }
    }

    /**
     * Handles "sync_tasks" data-only FCM messages from the backend.
     *
     * When a plan room task is created/updated/deleted, the backend sends this
     * trigger to all room members so they can sync tasks and update local alarms.
     */
    private fun handleSyncTasksTrigger() {
        Napier.d("Received sync_tasks trigger — syncing tasks in background", tag = "PlanoraFcmService")

        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                planRepository.fetchMyTasks()

                // After sync, reschedule all reminders
                rescheduleRemindersFromDb()
            } catch (e: Exception) {
                Napier.e("Failed to sync tasks on FCM trigger", e, tag = "PlanoraFcmService")
            }
        }
    }

    /**
     * Handles general notification messages (with title/body).
     */
    private fun handleGeneralNotification(message: RemoteMessage) {
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "Planora"

        val body = message.notification?.body
            ?: message.data["body"]
            ?: ""

        val type = message.data["type"] ?: "general"
        val roomId = message.data["roomId"]
        val taskId = message.data["taskId"]
        val rawDeepLink = message.data["deepLink"]

        val deepLinkUrl = when {
            !rawDeepLink.isNullOrEmpty() -> rawDeepLink
            type == "room_invite" -> "planora://share/roomInvite"
            !roomId.isNullOrEmpty() -> "planora://share/joinRoom?roomId=$roomId"
            !taskId.isNullOrEmpty() -> "planora://share/task?taskId=$taskId"
            else -> null
        }

        showNotification(
            title = title,
            body = body,
            type = type,
            deepLinkUrl = deepLinkUrl
        )
    }

    /**
     * Reads all tasks from local DB and reschedules reminder alarms.
     */
    private fun rescheduleRemindersFromDb() {
        try {
            val taskEntities = database.planoraDatabaseQueries.getAllTasks().executeAsList()
            val tasks = taskEntities.mapNotNull { entity ->
                try {
                    TaskDto(
                        id = entity.id,
                        creatorId = entity.creatorId.toInt(),
                        title = entity.title,
                        description = entity.description,
                        startTime = entity.startTime,
                        endTime = entity.endTime,
                        type = TaskType.valueOf(entity.type),
                        status = TaskStatus.valueOf(entity.status),
                        visibility = TaskVisibility.valueOf(entity.visibility),
                        sharedRoomIds = emptyList(),
                        reminders = entity.reminders?.let {
                            try { Json.decodeFromString(it) } catch (_: Exception) { emptyList() }
                        } ?: emptyList(),
                        specificDetails = entity.specificDetails?.let {
                            try { Json.decodeFromString(it) } catch(_: Exception) { null }
                        }
                    )
                } catch (_: Exception) {
                    null
                }
            }

            reminderManager.scheduleAllReminders(tasks)
            Napier.d("Rescheduled reminders after FCM sync: ${tasks.size} tasks", tag = "PlanoraFcmService")
        } catch (e: Exception) {
            Napier.e("Failed to reschedule reminders", e, tag = "PlanoraFcmService")
        }
    }

    private fun showNotification(
        title: String,
        body: String,
        type: String = "general",
        deepLinkUrl: String? = null
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            deepLinkUrl?.let { data = it.toUri() }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val soundUri =
            "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${packageName}/${R.raw.planora_chime}".toUri()

        val audioAttributes = android.media.AudioAttributes.Builder()
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
            .build()

        val channelName = getString(R.string.notification_channel_default_name)
        val channelDesc = getString(R.string.notification_channel_default_desc)

        val channel = NotificationChannel(
            DEFAULT_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = channelDesc
            enableLights(true)
            lightColor = "#7C4DFF".toColorInt()
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 120, 80, 120)
            setSound(soundUri, audioAttributes)
            setShowBadge(true)
        }

        notificationManager.createNotificationChannel(channel)

        val (styledTitle, category) = when (type.lowercase()) {
            "task" -> "✅ $title" to NotificationCompat.CATEGORY_REMINDER
            "event" -> "🎉 $title" to NotificationCompat.CATEGORY_EVENT
            "room_invite" -> "💌 $title" to NotificationCompat.CATEGORY_MESSAGE
            "follow_request" -> "👤 $title" to NotificationCompat.CATEGORY_SOCIAL
            "calendar_request" -> "📅 $title" to NotificationCompat.CATEGORY_EVENT
            else -> "✨ $title" to NotificationCompat.CATEGORY_MESSAGE
        }

        val builder = NotificationCompat.Builder(this, DEFAULT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor("#7C4DFF".toColorInt())
            .setContentTitle(styledTitle)
            .setContentText(body)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(styledTitle)
                    .bigText(body)
                    .setSummaryText("Planora ✨")
            )
            .setCategory(category)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 120, 80, 120))
            .setOnlyAlertOnce(false)

        notificationManager.notify(
            System.currentTimeMillis().toInt(),
            builder.build()
        )
    }
}
