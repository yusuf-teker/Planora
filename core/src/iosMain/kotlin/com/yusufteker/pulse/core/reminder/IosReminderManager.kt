package com.yusufteker.pulse.core.reminder

import com.yusufteker.pulse.shared.api.TaskDto

/**
 * iOS stub implementation of [ReminderManager].
 *
 * Local notification scheduling via UNUserNotificationCenter
 * will be implemented in Phase 2.
 */
class IosReminderManager : ReminderManager {

    override fun scheduleAllReminders(tasks: List<TaskDto>) {
        // iOS implementation deferred to Phase 2
        // Will use UNUserNotificationCenter for scheduling local notifications
    }

    override fun cancelAllReminders() {
        // iOS implementation deferred to Phase 2
    }
}
