package com.yusufteker.pulse

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.yusufteker.pulse.core.reminder.ReminderManager
import com.yusufteker.pulse.shared.api.TaskDto
import com.yusufteker.pulse.shared.api.TaskStatus
import com.yusufteker.pulse.shared.api.TaskType
import com.yusufteker.pulse.shared.api.TaskVisibility
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue

class PulsyFcmService : FirebaseMessagingService(), KoinComponent {

    companion object {
        const val DEFAULT_CHANNEL_ID = "pulse_default_channel"
        const val DEFAULT_CHANNEL_NAME = "Pulsy Bildirimleri"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Napier.d("New FCM Token: $token", tag = "PulsyFcmService")
        
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                // Save the token locally
                val sessionPreferences: com.yusufteker.pulse.core.preferences.SessionPreferences by inject()
                sessionPreferences.saveFcmToken(token)

                // Try to send it to backend if logged in
                val authRepository: com.yusufteker.pulse.feature.auth.domain.repository.AuthRepository by inject()
                if (authRepository.hasValidSession()) {
                    authRepository.registerFcmToken(token)
                }
            } catch (e: Exception) {
                Napier.e("Failed to handle new FCM token", e, tag = "PulsyFcmService")
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Napier.d("FCM Message Received: ${message.data}", tag = "PulsyFcmService")

        val type = message.data["type"]

        when (type) {
            "sync_tasks" -> handleSyncTasksTrigger()
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
        Napier.d("Received sync_tasks trigger — syncing tasks in background", tag = "PulsyFcmService")

        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                val planRepository: com.yusufteker.pulse.feature.home.domain.repository.PlanRepository by inject()
                planRepository.fetchMyTasks()

                // After sync, reschedule all reminders
                rescheduleRemindersFromDb()
            } catch (e: Exception) {
                Napier.e("Failed to sync tasks on FCM trigger", e, tag = "PulsyFcmService")
            }
        }
    }

    /**
     * Handles general notification messages (with title/body).
     */
    private fun handleGeneralNotification(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "Pulsy"
        val body = message.notification?.body ?: message.data["body"] ?: ""

        showNotification(title, body)
    }

    /**
     * Reads all tasks from local DB and reschedules reminder alarms.
     */
    private fun rescheduleRemindersFromDb() {
        try {
            val database: com.yusufteker.pulse.core.database.PulsyDatabase by inject()
            val reminderManager: ReminderManager by inject()

            val taskEntities = database.pulsyDatabaseQueries.getAllTasks().executeAsList()
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
            Napier.d("Rescheduled reminders after FCM sync: ${tasks.size} tasks", tag = "PulsyFcmService")
        } catch (e: Exception) {
            Napier.e("Failed to reschedule reminders", e, tag = "PulsyFcmService")
        }
    }

    private fun showNotification(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, DEFAULT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                DEFAULT_CHANNEL_ID,
                DEFAULT_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
