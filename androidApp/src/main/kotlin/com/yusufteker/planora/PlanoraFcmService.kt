package com.yusufteker.planora

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
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

class PlanoraFcmService : FirebaseMessagingService(), KoinComponent {

    private val sessionPreferences: com.yusufteker.planora.core.preferences.SessionPreferences by inject()
    private val authRepository: com.yusufteker.planora.feature.auth.domain.repository.AuthRepository by inject()
    private val planRepository: com.yusufteker.planora.feature.home.domain.repository.PlanRepository by inject()
    private val database: com.yusufteker.planora.core.database.PlanoraDatabase by inject()
    private val reminderManager: ReminderManager by inject()


    companion object {
        const val DEFAULT_CHANNEL_ID = "planora_default_channel"
        const val DEFAULT_CHANNEL_NAME = "Planora Bildirimleri"
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

        val type = message.data["type"]

        when (type) {
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

        showNotification(
            title = title,
            body = body,
            type = type
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
                            try { Json.decodeFromString(it) } catch (e: Exception) { emptyList() }
                        } ?: emptyList(),
                        specificDetails = entity.specificDetails?.let {
                            try { Json.decodeFromString(it) } catch(e: Exception) { null }
                        }
                    )
                } catch (e: Exception) {
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
        type: String = "general"
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                DEFAULT_CHANNEL_ID,
                DEFAULT_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Task, event and reminder notifications"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(channel)
        }

        val category = when (type.lowercase()) {
            "task" -> NotificationCompat.CATEGORY_REMINDER
            "event" -> NotificationCompat.CATEGORY_EVENT
            else -> NotificationCompat.CATEGORY_MESSAGE
        }

        val builder = NotificationCompat.Builder(this, DEFAULT_CHANNEL_ID)
            // Kendi ikonunu koymanı öneririm
            //.setSmallIcon(R.drawable.ic_notification)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(body)
                    .setSummaryText("Planora")
            )
            .setCategory(category)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOnlyAlertOnce(false)

        notificationManager.notify(
            System.currentTimeMillis().toInt(),
            builder.build()
        )
    }
}
