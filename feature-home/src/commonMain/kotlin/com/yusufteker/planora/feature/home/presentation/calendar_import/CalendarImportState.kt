package com.yusufteker.planora.feature.home.presentation.calendar_import

import com.yusufteker.planora.core.base.UiState
import com.yusufteker.planora.core.calendar.CalendarImportDateRange
import com.yusufteker.planora.core.calendar.CalendarImportItem

/**
 * UI State for the Calendar Import screen.
 *
 * @property isLoading True while calendar events are being queried.
 * @property isImporting True while selected events are being batch-imported into Planora.
 * @property hasPermission True if calendar read permission is granted.
 * @property isPermissionDenied True if permission was denied by the user.
 * @property selectedDateRange Currently active date range filter.
 * @property availableCalendars Set of unique calendar names detected (e.g. "Google Calendar", "iCloud").
 * @property selectedCalendarFilter Filter by calendar name, or null for all calendars.
 * @property events The list of retrieved calendar events.
 * @property editingItem The item currently opened in the quick edit dialog, if any.
 */
data class CalendarImportState(
    val isLoading: Boolean = false,
    val isImporting: Boolean = false,
    val hasPermission: Boolean = false,
    val isPermissionDenied: Boolean = false,
    val selectedDateRange: CalendarImportDateRange = CalendarImportDateRange.PAST_AND_FUTURE_30_DAYS,
    val availableCalendars: List<String> = emptyList(),
    val selectedCalendarFilter: String? = null,
    val events: List<CalendarImportItem> = emptyList(),
    val editingItem: CalendarImportItem? = null,
    val isGoogleTasksLoading: Boolean = false
) : UiState {
    /**
     * Events filtered by the active calendar source filter.
     */
    val filteredEvents: List<CalendarImportItem>
        get() = if (selectedCalendarFilter == null) {
            events
        } else {
            events.filter { it.sourceDisplayName == selectedCalendarFilter || it.calendarName == selectedCalendarFilter }
        }

    /**
     * Total number of selected events to import.
     */
    val selectedCount: Int
        get() = filteredEvents.count { it.isSelected }

    /**
     * Total number of events currently visible.
     */
    val totalCount: Int
        get() = filteredEvents.size

    /**
     * True if all visible events are selected.
     */
    val isAllSelected: Boolean
        get() = filteredEvents.isNotEmpty() && filteredEvents.all { it.isSelected }
}
