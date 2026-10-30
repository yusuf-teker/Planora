package com.yusufteker.planora.core.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.yusufteker.planora.core.preferences.SessionPreferences
import com.yusufteker.planora.core.utils.RecurringTaskEvaluator
import com.yusufteker.planora.shared.api.ItemDetails
import com.yusufteker.planora.shared.api.RecurrenceRule
import com.yusufteker.planora.shared.api.TaskDto
import com.yusufteker.planora.shared.api.TaskStatus
import com.yusufteker.planora.shared.api.TaskType
import com.yusufteker.planora.shared.api.TaskVisibility
import io.github.aakira.napier.Napier
import kotlinx.serialization.json.Json

/**
 * Android implementation of [ReminderManager] using [AlarmManager].
 *
 * Uses [AlarmManager.setExactAndAllowWhileIdle] or [AlarmManager.setAlarmClock] for precise alarms
 * that work even in Doze mode.
 *
 * Guarantees that:
 * 1. Notifications/alarms are NEVER scheduled in the past.
 * 2. Events that have already started or ended are strictly excluded.
 * 3. In shared rooms, alarms are only scheduled for assigned members (or creator / all when unassigned).
 * 4. Completed tasks and folders are filtered out.
 * 5. Recurring tasks within the next 14 days are evaluated and scheduled.
 */
class AndroidReminderManager(
    private val context: Context,
    private val sessionPreferences: SessionPreferences? = null
) : ReminderManager {

    companion object {
        private const val TAG = "ReminderManager"
        private const val PREFS_NAME = "planora_reminder_prefs"
        private const val KEY_REQUEST_CODES = "active_request_codes"

        // Intent extras
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_REMINDER_MINUTES = "extra_reminder_minutes"
        const val EXTRA_TASK_TYPE = "extra_task_type"

        // Receiver action
        const val ACTION_REMINDER = "com.yusufteker.planora.ACTION_REMINDER"

        /**
         * Generates a deterministic, positive request code for a (taskId, reminderMinutes) pair.
         */
        fun generateRequestCode(taskId: String, reminderMinutes: Int): Int {
            return ("$taskId:$reminderMinutes".hashCode() and 0x7FFFFFFF)
        }

        fun generateOccurrenceRequestCode(taskId: String, occurrenceMs: Long, reminderMinutes: Int): Int {
            return ("${taskId}_${occurrenceMs}_$reminderMinutes".hashCode() and 0x7FFFFFFF)
        }
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun scheduleAllReminders(tasks: List<TaskDto>) {
        // 1. Cancel all existing alarms
        cancelAllReminders()

        val now = System.currentTimeMillis()
        val currentUserId = sessionPreferences?.cachedUserIdInt
        val newRequestCodes = mutableSetOf<Int>()

        Napier.d("Evaluating ${tasks.size} tasks for Android alarm scheduling (currentUserId=$currentUserId)", tag = TAG)

        // 2. Schedule new alarms for each valid task/event with reminders
        tasks.forEach { task ->
            // Skip completed tasks and folders
            if (task.status == TaskStatus.COMPLETED || task.type == TaskType.FOLDER) {
                return@forEach
            }

            // Room duty / assignment check
            if (task.visibility == TaskVisibility.ROOM_SHARED || task.sharedRoomIds.isNotEmpty()) {
                if (currentUserId != null) {
                    val isAssigned = task.creatorId == currentUserId ||
                            task.participants.any { it.userId == currentUserId } ||
                            task.participants.isEmpty()
                    if (!isAssigned) {
                        return@forEach
                    }
                }
            }

            // Past check for Events: Never schedule alarms for past/current events
            if (task.type == TaskType.EVENT) {
                val eventEndTime = task.endTime ?: task.startTime
                if (eventEndTime <= now || task.startTime <= now) {
                    return@forEach
                }
            }

            if (task.reminders.isEmpty()) {
                return@forEach
            }

            // Handle recurring vs single tasks
            val ruleStr = task.recurrenceRule
            if (task.isRecurring && ruleStr != null) {
                val rule = try {
                    Json.decodeFromString<RecurrenceRule>(ruleStr)
                } catch (e: Exception) {
                    null
                }

                if (rule != null) {
                    val occurrences = RecurringTaskEvaluator.generateOccurrences(
                        startTimeMs = task.startTime,
                        rule = rule,
                        rangeStartMs = now,
                        rangeEndMs = now + 14 * 24 * 3600 * 1000L
                    )

                    occurrences.forEach { occurrenceMs ->
                        if (task.type == TaskType.EVENT && occurrenceMs <= now) {
                            return@forEach
                        }
                        task.reminders.forEach { reminderMinutes ->
                            val triggerTime = occurrenceMs - (reminderMinutes * 60_000L)
                            if (triggerTime > now + 1_000L) {
                                val requestCode = generateOccurrenceRequestCode(task.id, occurrenceMs, reminderMinutes)
                                scheduleExactAlarm(
                                    requestCode = requestCode,
                                    triggerTimeMs = triggerTime,
                                    taskId = task.id,
                                    taskTitle = task.title,
                                    reminderMinutes = reminderMinutes,
                                    taskType = task.type
                                )
                                newRequestCodes.add(requestCode)
                            }
                        }
                    }
                }
            } else {
                val baseTime = when (task.type) {
                    TaskType.TASK -> {
                        val taskDetails = task.specificDetails as? ItemDetails.Task
                        taskDetails?.deadline ?: task.startTime
                    }
                    else -> task.startTime
                }

                if (task.type == TaskType.TASK) {
                    val taskDeadline = (task.specificDetails as? ItemDetails.Task)?.deadline
                    if (taskDeadline != null && taskDeadline <= now) {
                        return@forEach
                    }
                }

                task.reminders.forEach { reminderMinutes ->
                    val triggerTime = baseTime - (reminderMinutes * 60_000L)

                    // Strict future check: must be at least 1 second in future
                    if (triggerTime > now + 1_000L) {
                        val requestCode = generateRequestCode(task.id, reminderMinutes)
                        scheduleExactAlarm(
                            requestCode = requestCode,
                            triggerTimeMs = triggerTime,
                            taskId = task.id,
                            taskTitle = task.title,
                            reminderMinutes = reminderMinutes,
                            taskType = task.type
                        )
                        newRequestCodes.add(requestCode)
                    }
                }
            }
        }

        // 3. Persist active request codes
        saveRequestCodes(newRequestCodes)
        Napier.d("Scheduled ${newRequestCodes.size} reminders for ${tasks.size} tasks", tag = TAG)
    }

    override fun cancelAllReminders() {
        val codes = loadRequestCodes()
        codes.forEach { requestCode ->
            val intent = createReminderIntent("", "", 0)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }
        saveRequestCodes(emptySet())
        Napier.d("Cancelled ${codes.size} reminders", tag = TAG)
    }

    private fun scheduleExactAlarm(
        requestCode: Int,
        triggerTimeMs: Long,
        taskId: String,
        taskTitle: String,
        reminderMinutes: Int,
        taskType: TaskType
    ) {
        val intent = createReminderIntent(taskId, taskTitle, reminderMinutes, taskType)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent().apply {
            setClassName(context.packageName, "${context.packageName}.MainActivity")
            putExtra("taskId", taskId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val clockInfo = AlarmManager.AlarmClockInfo(triggerTimeMs, showPendingIntent)
            alarmManager.setAlarmClock(clockInfo, pendingIntent)
            Napier.d("Scheduled Clock Alarm for $taskTitle at $triggerTimeMs (code=$requestCode)", tag = TAG)
        } catch (e: SecurityException) {
            Napier.e("SecurityException in setAlarmClock, falling back: ${e.message}", tag = TAG)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTimeMs,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTimeMs,
                            pendingIntent
                        )
                        Napier.w("Exact alarm permission not granted, using inexact alarm", tag = TAG)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMs,
                        pendingIntent
                    )
                }
            } catch (e2: Exception) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            }
        }
    }

    /**
     * Snoozes an alarm for [delayMinutes] minutes (default 10).
     */
    override fun snoozeReminder(
        taskId: String,
        taskTitle: String,
        taskType: TaskType,
        delayMinutes: Int
    ) {
        val triggerTimeMs = System.currentTimeMillis() + (delayMinutes * 60_000L)
        val requestCode = generateRequestCode(taskId, -delayMinutes) // negative minutes avoids collision
        scheduleExactAlarm(
            requestCode = requestCode,
            triggerTimeMs = triggerTimeMs,
            taskId = taskId,
            taskTitle = taskTitle,
            reminderMinutes = delayMinutes,
            taskType = taskType
        )
        val codes = loadRequestCodes().toMutableSet()
        codes.add(requestCode)
        saveRequestCodes(codes)
        Napier.i("Snoozed alarm for $delayMinutes mins ($taskTitle)", tag = TAG)
    }

    private fun createReminderIntent(
        taskId: String,
        taskTitle: String,
        reminderMinutes: Int,
        taskType: TaskType = TaskType.NOTE
    ): Intent {
        return Intent(ACTION_REMINDER).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, taskTitle)
            putExtra(EXTRA_REMINDER_MINUTES, reminderMinutes)
            putExtra(EXTRA_TASK_TYPE, taskType.name)
        }
    }

    // ─── Persistence ──────────────────────────────────────────

    private fun saveRequestCodes(codes: Set<Int>) {
        prefs.edit()
            .putStringSet(KEY_REQUEST_CODES, codes.map { it.toString() }.toSet())
            .apply()
    }

    private fun loadRequestCodes(): Set<Int> {
        return prefs.getStringSet(KEY_REQUEST_CODES, emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet()
            ?: emptySet()
    }
}
