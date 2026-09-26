package com.yusufteker.planora.core.calendar

/**
 * Common abstraction for reading and adding events to the device's native calendar
 * (Apple Calendar / Google Calendar).
 */
interface CalendarService {

    /**
     * Adds an event or task to the native system calendar.
     */
    fun addToSystemCalendar(
        title: String,
        description: String? = null,
        location: String? = null,
        startTimeEpochMillis: Long,
        endTimeEpochMillis: Long? = null
    ): Boolean

    /**
     * Checks if the app currently has permission to read system calendar events.
     */
    fun hasCalendarReadPermission(): Boolean

    /**
     * Queries and retrieves events from native device calendars within the specified epoch millisecond range.
     */
    suspend fun fetchCalendarEvents(
        startEpochMillis: Long,
        endEpochMillis: Long
    ): List<CalendarImportItem>
}
