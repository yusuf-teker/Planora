package com.yusufteker.planora.core.calendar

/**
 * Platform-agnostic manager for synchronizing events and tasks
 * with the native operating system calendar (Google Calendar on Android, Apple Calendar on iOS).
 */
expect class CalendarSyncManager : CalendarService {
    override fun addToSystemCalendar(
        title: String,
        description: String?,
        location: String?,
        startTimeEpochMillis: Long,
        endTimeEpochMillis: Long?
    ): Boolean

    /**
     * Checks if the app currently has permission to read system calendar events.
     *
     * @return True if calendar read permission is granted, false otherwise.
     */
    override fun hasCalendarReadPermission(): Boolean

    /**
     * Queries and retrieves events from native device calendars (Google Calendar, Apple Calendar, etc.)
     * within the specified epoch millisecond range.
     *
     * @param startEpochMillis The start of the time window in milliseconds since epoch.
     * @param endEpochMillis The end of the time window in milliseconds since epoch.
     * @return A list of [CalendarImportItem] instances representing native calendar events.
     */
    override suspend fun fetchCalendarEvents(
        startEpochMillis: Long,
        endEpochMillis: Long
    ): List<CalendarImportItem>
}
