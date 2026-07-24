package com.yusufteker.planora.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yusufteker.planora.core.database.PlanoraDatabase
import com.yusufteker.planora.core.reminder.ReminderManager
import com.yusufteker.planora.shared.api.TaskDto
import com.yusufteker.planora.shared.api.TaskStatus
import com.yusufteker.planora.shared.api.TaskType
import com.yusufteker.planora.shared.api.TaskVisibility
import io.github.aakira.napier.Napier
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * BroadcastReceiver that restores all task reminder alarms after a device reboot.
 *
 * Android clears all AlarmManager alarms on reboot, so this receiver
 * reads tasks from the local SQLDelight database and re-schedules them.
 */
class BootReceiver : BroadcastReceiver(), KoinComponent {

    private val database: PlanoraDatabase by inject()
    private val reminderManager: ReminderManager by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Napier.d("Boot completed — restoring reminder alarms", tag = "BootReceiver")

        try {
            // Read all tasks from local DB synchronously (BroadcastReceiver has limited time)
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
                        isRecurring = entity.isRecurring == 1L,
                        recurrenceRule = entity.recurrenceRule,
                        isFlexible = entity.isFlexible == 1L,
                        isOptional = entity.isOptional == 1L,
                        isPostponable = entity.isPostponable == 1L,
                        isAllDay = entity.isAllDay == 1L,
                        reminders = entity.reminders?.let {
                            try { Json.decodeFromString(it) } catch (e: Exception) { emptyList() }
                        } ?: emptyList(),
                        specificDetails = entity.specificDetails?.let {
                            try { Json.decodeFromString(it) } catch(e: Exception) { null }
                        }
                    )
                } catch (e: Exception) {
                    Napier.e("Failed to map task ${entity.id} in BootReceiver: ${e.message}", tag = "BootReceiver")
                    null
                }
            }

            reminderManager.scheduleAllReminders(tasks)
            Napier.d("Restored ${tasks.size} task alarms after boot", tag = "BootReceiver")
        } catch (e: Exception) {
            Napier.e("Failed to restore alarms after boot: ${e.message}", e, tag = "BootReceiver")
        }
    }
}
