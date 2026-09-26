package com.yusufteker.planora.core.calendar

import com.yusufteker.planora.shared.api.TaskPriority
import com.yusufteker.planora.shared.api.TaskType

/**
 * Represents a calendar event retrieved from the device's native calendar (Apple Calendar or Google Calendar),
 * prepared for user review, modification, and batch import into Planora.
 *
 * @property id Unique event identifier provided by the native calendar provider.
 * @property title Title or summary of the calendar event.
 * @property description Optional detailed notes or description.
 * @property location Optional physical or virtual event location.
 * @property startTimeEpochMillis Start time in milliseconds since Unix epoch.
 * @property endTimeEpochMillis End time in milliseconds since Unix epoch, if available.
 * @property isAllDay Whether the event is scheduled as an all-day event.
 * @property calendarName Display name of the calendar (e.g., "Google Calendar", "iCloud", "Work").
 * @property accountName Account identifier owning the calendar (e.g., email address).
 * @property targetType The destination entity type in Planora ([TaskType.EVENT] or [TaskType.TASK]).
 * @property priority Selected task priority if imported as a task.
 * @property isSelected Whether this item is currently selected for import by the user.
 */
data class CalendarImportItem(
    val id: String,
    val title: String,
    val description: String? = null,
    val location: String? = null,
    val startTimeEpochMillis: Long,
    val endTimeEpochMillis: Long? = null,
    val isAllDay: Boolean = false,
    val calendarName: String? = null,
    val accountName: String? = null,
    val targetType: TaskType = TaskType.EVENT,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val isSelected: Boolean = true
)
