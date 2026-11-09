package com.yusufteker.planora.core.utils

import com.yusufteker.planora.core.domain.usecase.RegisterFcmTokenUseCase
import com.yusufteker.planora.core.navigation.DeepLinkManager
import com.yusufteker.planora.core.preferences.SessionPreferences
import com.yusufteker.planora.core.reminder.ReminderManager
import com.yusufteker.planora.shared.api.TaskType
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

/**
 * Bridge between iOS Push Notifications / APNs and Planora's shared domain logic.
 *
 * Guarantees crash-free execution even during headless background wakeups:
 * - Safely resolves Koin components without throwing if Koin is not started yet.
 * - Uses a [SupervisorJob] scope so child errors do not crash the app.
 * - Catches all [Throwable]s to avoid unhandled native aborts.
 */
object IosNotificationBridge : KoinComponent {

    private val sessionPreferences: SessionPreferences?
        get() = runCatching { getKoin().get<SessionPreferences>() }.getOrNull()

    private val registerFcmTokenUseCase: RegisterFcmTokenUseCase?
        get() = runCatching { getKoin().get<RegisterFcmTokenUseCase>() }.getOrNull()

    private val bridgeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var onSyncTasksRequested: (suspend () -> Unit)? = null
    var onSyncRoomsRequested: (suspend () -> Unit)? = null

    /**
     * Stores the new FCM registration token and synchronizes it with the backend if logged in.
     */
    fun setFcmToken(token: String) {
        bridgeScope.launch {
            try {
                val prefs = sessionPreferences
                if (prefs != null) {
                    prefs.saveFcmToken(token)
                    // If the user is logged in, register with the server immediately
                    val userId = prefs.userIdFlow.first()
                    if (userId != null) {
                        registerFcmTokenUseCase?.invoke()
                    }
                } else {
                    Napier.w("SessionPreferences not yet available to save FCM token", tag = "IosNotificationBridge")
                }
            } catch (e: Throwable) {
                Napier.e("Failed to set FCM token on iOS: ${e.message}", e, tag = "IosNotificationBridge")
            }
        }
    }

    /**
     * Handles incoming silent/background push notifications.
     *
     * @param type The notification event type from the APNs payload.
     * @param onComplete Optional callback called when background processing finishes,
     *                   allowing iOS `fetchCompletionHandler` to be notified.
     */
    fun handlePushData(type: String, onComplete: (() -> Unit)? = null) {
        bridgeScope.launch {
            try {
                when (type) {
                    "follow_request" -> {
                        try {
                            sessionPreferences?.incrementPendingFollowRequestsCount()
                        } catch (e: Exception) {
                            Napier.e("Failed to increment follow requests count: ${e.message}", e, tag = "IosNotificationBridge")
                        }
                    }
                    "calendar_request" -> {
                        try {
                            sessionPreferences?.incrementPendingCalendarRequestsCount()
                        } catch (e: Exception) {
                            Napier.e("Failed to increment calendar requests count: ${e.message}", e, tag = "IosNotificationBridge")
                        }
                    }
                    "sync_tasks", "event_assignment", "task_assignment", "room_event", "room_task" -> {
                        try {
                            onSyncTasksRequested?.invoke()
                        } catch (e: Exception) {
                            Napier.e("Failed to execute sync_tasks on iOS: ${e.message}", e, tag = "IosNotificationBridge")
                        }
                    }
                    "room_invite", "room_invite_response", "room_member_removed" -> {
                        try {
                            onSyncRoomsRequested?.invoke()
                            onSyncTasksRequested?.invoke()
                        } catch (e: Exception) {
                            Napier.e("Failed to execute room sync on iOS: ${e.message}", e, tag = "IosNotificationBridge")
                        }
                    }
                    else -> {
                        Napier.d("Unhandled push type: $type", tag = "IosNotificationBridge")
                    }
                }
            } catch (e: Throwable) {
                Napier.e("handlePushData error on iOS: ${e.message}", e, tag = "IosNotificationBridge")
            } finally {
                try {
                    onComplete?.invoke()
                } catch (e: Throwable) {
                    Napier.e("Error invoking push onComplete: ${e.message}", e, tag = "IosNotificationBridge")
                }
            }
        }
    }

    /**
     * Handles user interaction with a local or push notification (tap or action button).
     */
    fun handleNotificationResponse(actionIdentifier: String, userInfo: Map<Any?, *>) {
        bridgeScope.launch {
            try {
                val taskId = userInfo["taskId"] as? String
                val taskTitle = userInfo["taskTitle"] as? String ?: ""
                val taskTypeStr = userInfo["taskType"] as? String
                val taskType = taskTypeStr?.let { runCatching { TaskType.valueOf(it) }.getOrNull() } ?: TaskType.TASK
                val roomId = userInfo["roomId"] as? String
                val rawDeepLink = userInfo["deepLink"] as? String
                val type = userInfo["type"] as? String

                when (actionIdentifier) {
                    "PLANORA_ACTION_SNOOZE" -> {
                        if (!taskId.isNullOrEmpty()) {
                            val reminderManager = runCatching { getKoin().get<ReminderManager>() }.getOrNull()
                            reminderManager?.snoozeReminder(taskId, taskTitle, taskType, delayMinutes = 10)
                        }
                    }
                    "PLANORA_ACTION_DISMISS" -> {
                        // Dismissed by user action
                    }
                    else -> {
                        val deepLinkUrl = when {
                            !rawDeepLink.isNullOrEmpty() -> rawDeepLink
                            type == "room_invite" -> "planora://share/roomInvite"
                            taskType == TaskType.EVENT -> {
                                if (!roomId.isNullOrEmpty() && !taskId.isNullOrEmpty()) {
                                    "planora://share/event?eventId=$taskId&roomId=$roomId"
                                } else if (!taskId.isNullOrEmpty()) {
                                    "planora://share/event?eventId=$taskId"
                                } else null
                            }
                            !roomId.isNullOrEmpty() && taskId.isNullOrEmpty() -> "planora://share/joinRoom?roomId=$roomId"
                            !taskId.isNullOrEmpty() -> {
                                if (!roomId.isNullOrEmpty()) {
                                    "planora://share/task?taskId=$taskId&roomId=$roomId"
                                } else {
                                    "planora://share/task?taskId=$taskId"
                                }
                            }
                            else -> null
                        }
                        if (!deepLinkUrl.isNullOrEmpty()) {
                            DeepLinkManager.emitLink(deepLinkUrl)
                        }
                    }
                }
            } catch (e: Throwable) {
                Napier.e("handleNotificationResponse error: ${e.message}", e, tag = "IosNotificationBridge")
            }
        }
    }
}
