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
) {
    /**
     * Human-friendly source badge label (e.g. "Google Calendar", "Apple Calendar", "Apple Reminders", "Samsung Calendar").
     */
    val sourceDisplayName: String
        get() {
            val acc = accountName?.lowercase()?.trim() ?: ""
            val cal = calendarName?.trim() ?: ""
            return when {
                acc.contains("google") || acc.contains("@gmail.com") -> {
                    if (cal.isNotBlank() && !cal.contains("@gmail.com") && !cal.equals("events", ignoreCase = true)) {
                        "Google Calendar • $cal"
                    } else {
                        "Google Calendar"
                    }
                }
                acc.contains("samsung") -> {
                    if (cal.isNotBlank() && !cal.equals("my calendar", ignoreCase = true)) "Samsung Calendar • $cal"
                    else "Samsung Calendar"
                }
                acc.contains("apple") || acc.contains("icloud") || cal.equals("icloud", ignoreCase = true) -> {
                    if (cal.contains("reminder", ignoreCase = true) || acc.contains("reminder", ignoreCase = true)) "Apple Reminders"
                    else if (cal.isNotBlank() && !cal.equals("calendar", ignoreCase = true)) "Apple Calendar • $cal"
                    else "Apple Calendar"
                }
                cal.contains("reminder", ignoreCase = true) || acc.contains("reminder", ignoreCase = true) -> "Apple Reminders"
                acc.contains("outlook") || acc.contains("hotmail") || acc.contains("live.com") || acc.contains("microsoft") -> {
                    if (cal.isNotBlank() && !cal.equals("calendar", ignoreCase = true)) "Outlook • $cal"
                    else "Outlook"
                }
                acc.contains("exchange") -> {
                    if (cal.isNotBlank() && !cal.equals("calendar", ignoreCase = true)) "Exchange • $cal"
                    else "Exchange"
                }
                cal.isNotBlank() -> cal
                acc.isNotBlank() -> acc
                else -> "Device Calendar"
            }
        }
}
