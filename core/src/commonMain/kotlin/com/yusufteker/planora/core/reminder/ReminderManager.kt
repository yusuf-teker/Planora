package com.yusufteker.planora.core.reminder

import com.yusufteker.planora.shared.api.TaskDto

/**
 * Platform-agnostic interface for scheduling task reminders.
 *
 * Android: Uses AlarmManager for exact local alarms.
 * iOS: Will use UNUserNotificationCenter (deferred to Phase 2).
 */
interface ReminderManager {

    /**
     * Cancels all existing reminders and schedules new ones
     * based on the provided task list.
     *
     * For each task with non-empty [TaskDto.reminders], an alarm is scheduled
     * at (startTime - reminderMinutes * 60_000) if that time is in the future.
     */
    fun scheduleAllReminders(tasks: List<TaskDto>)

    /**
     * Cancels all scheduled reminders (e.g., on logout).
     */
    fun cancelAllReminders()

    /**
     * Snoozes a reminder for a given duration (default 10 minutes).
     */
    fun snoozeReminder(
        taskId: String,
        taskTitle: String,
        taskType: com.yusufteker.planora.shared.api.TaskType,
        delayMinutes: Int = 10
    ) {}
}
