package com.yusufteker.planora.core.calendar

/**
 * Platform-agnostic manager for synchronizing events and tasks
 * with the native operating system calendar (Google Calendar on Android, Apple Calendar on iOS).
 */
expect class CalendarSyncManager {

    /**
     * Adds an event or task to the native system calendar.
     *
     * On Android: Launches the system calendar insert intent (Google Calendar / default).
     * On iOS: Inserts the event into the default iOS EventStore / Apple Calendar.
     *
     * @param title Title of the event or task.
     * @param description Optional description or notes.
     * @param location Optional physical or virtual location.
     * @param startTimeEpochMillis Start time in milliseconds since epoch.
     * @param endTimeEpochMillis Optional end time in milliseconds since epoch. If null, defaults to 1 hour after start.
     * @return Boolean indicating whether the action was successfully initiated.
     */
    fun addToSystemCalendar(
        title: String,
        description: String? = null,
        location: String? = null,
        startTimeEpochMillis: Long,
        endTimeEpochMillis: Long? = null
    ): Boolean
}
