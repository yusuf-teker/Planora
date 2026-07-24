package com.yusufteker.planora.core.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.yusufteker.planora.shared.api.ItemDetails
import com.yusufteker.planora.shared.api.TaskDto
import com.yusufteker.planora.shared.api.TaskType
import io.github.aakira.napier.Napier
import kotlinx.serialization.json.Json

/**
 * Android implementation of [ReminderManager] using [AlarmManager].
 *
 * Uses [AlarmManager.setExactAndAllowWhileIdle] for precise alarms
 * that work even in Doze mode.
 *
 * Scheduled alarm request codes are persisted in SharedPreferences
 * so they can be reliably cancelled later.
 */
class AndroidReminderManager(private val context: Context) : ReminderManager {

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
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun scheduleAllReminders(tasks: List<TaskDto>) {
        // 1. Cancel all existing alarms
        cancelAllReminders()

        val now = System.currentTimeMillis()
        val newRequestCodes = mutableSetOf<Int>()


        // 2. Schedule new alarms for each task with reminders
        tasks.forEach { task ->


            if (task.reminders.isEmpty()) {
                return@forEach
            }

            task.reminders.forEach { reminderMinutes ->
                // Calculate base time for the reminder
                val baseTime = when (task.type) {
                    TaskType.TASK -> {
                        val taskDetails = task.specificDetails as? ItemDetails.Task
                        taskDetails?.deadline ?: task.startTime
                    }
                    else -> task.startTime // EVENT and NOTE use startTime
                }
                
                val triggerTime = baseTime - (reminderMinutes * 60_000L)


                if (triggerTime > now - 60_000L) {

                    val requestCode = generateRequestCode(task.id, reminderMinutes)

                    Napier.d(
                        "Scheduling requestCode=$requestCode at $triggerTime",
                        tag = TAG
                    )
                    // Yeni
                    scheduleExactAlarm(
                        requestCode = requestCode,
                        triggerTimeMs = triggerTime,
                        taskId = task.id,
                        taskTitle = task.title,
                        reminderMinutes = reminderMinutes,
                        taskType = task.type
                    )
                    newRequestCodes.add(requestCode)
                }else{
                    Napier.w(
                        "Skipping reminder. triggerTime=$triggerTime <= now=$now",
                        tag = TAG
                    )
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

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMs,
                        pendingIntent
                    )
                } else {
                    // Fallback to inexact alarm
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
        } catch (e: SecurityException) {
            Napier.e("SecurityException scheduling alarm: ${e.message}", tag = TAG)
            // Fallback to inexact alarm
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMs,
                pendingIntent
            )
        }
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
