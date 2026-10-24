package com.yusufteker.planora.core.utils

import com.yusufteker.planora.core.domain.usecase.RegisterFcmTokenUseCase
import com.yusufteker.planora.core.navigation.DeepLinkManager
import com.yusufteker.planora.core.preferences.SessionPreferences
import com.yusufteker.planora.core.reminder.ReminderManager
import com.yusufteker.planora.shared.api.TaskType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object IosNotificationBridge : KoinComponent {
    private val sessionPreferences: SessionPreferences by inject()
    private val registerFcmTokenUseCase: RegisterFcmTokenUseCase by inject()

    var onSyncTasksRequested: (suspend () -> Unit)? = null

    fun setFcmToken(token: String) {
        CoroutineScope(Dispatchers.Main).launch {
            sessionPreferences.saveFcmToken(token)
            // If the user is logged in, register with the server immediately
            val userId = sessionPreferences.userIdFlow.first()
            if (userId != null) {
                registerFcmTokenUseCase()
            }
        }
    }

    fun handlePushData(type: String) {
        CoroutineScope(Dispatchers.Main).launch {
            when (type) {
                "follow_request" -> {
                    try {
                        sessionPreferences.incrementPendingFollowRequestsCount()
                    } catch (e: Exception) {
                        println("Failed to increment follow requests count: ${e.message}")
                    }
                }
                "calendar_request" -> {
                    try {
                        sessionPreferences.incrementPendingCalendarRequestsCount()
                    } catch (e: Exception) {
                        println("Failed to increment calendar requests count: ${e.message}")
                    }
                }
                "sync_tasks" -> {
                    try {
                        onSyncTasksRequested?.invoke()
                    } catch (e: Exception) {
                        println("Failed to execute sync_tasks on iOS: ${e.message}")
                    }
                }
            }
        }
    }

    fun handleNotificationResponse(actionIdentifier: String, userInfo: Map<Any?, *>) {
        CoroutineScope(Dispatchers.Main).launch {
            val taskId = userInfo["taskId"] as? String
            val taskTitle = userInfo["taskTitle"] as? String ?: ""
            val taskTypeStr = userInfo["taskType"] as? String
            val taskType = taskTypeStr?.let { runCatching { TaskType.valueOf(it) }.getOrNull() } ?: TaskType.TASK

            when (actionIdentifier) {
                "PLANORA_ACTION_SNOOZE" -> {
                    if (!taskId.isNullOrEmpty()) {
                        val reminderManager: ReminderManager by inject()
                        reminderManager.snoozeReminder(taskId, taskTitle, taskType, delayMinutes = 10)
                    }
                }
                "PLANORA_ACTION_DISMISS" -> {
                    // Dismissed by user action
                }
                else -> {
                    val deepLink = userInfo["deepLink"] as? String
                    val url = deepLink ?: taskId?.let { "planora://share/task?taskId=$it" }
                    if (!url.isNullOrEmpty()) {
                        DeepLinkManager.emitLink(url)
                    }
                }
            }
        }
    }
}
