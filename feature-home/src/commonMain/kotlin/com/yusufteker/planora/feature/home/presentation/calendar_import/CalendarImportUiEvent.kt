package com.yusufteker.planora.feature.home.presentation.calendar_import

import com.yusufteker.planora.core.base.UiEvent
import com.yusufteker.planora.core.calendar.CalendarImportDateRange
import com.yusufteker.planora.core.calendar.CalendarImportItem

/**
 * User interactions and events for the Calendar Import screen.
 */
sealed interface CalendarImportUiEvent : UiEvent {
    /** Check existing calendar permission status on screen load */
    data object CheckPermission : CalendarImportUiEvent

    /** Result of the native permission request prompt */
    data class PermissionResult(val isGranted: Boolean) : CalendarImportUiEvent

    /** User selected a new date range filter */
    data class SelectDateRange(val range: CalendarImportDateRange) : CalendarImportUiEvent

    /** User selected a calendar source filter */
    data class SelectCalendarFilter(val calendarName: String?) : CalendarImportUiEvent

    /** User toggled selection of a single event */
    data class ToggleEventSelection(val eventId: String) : CalendarImportUiEvent

    /** User toggled select all / deselect all */
    data class ToggleSelectAll(val selectAll: Boolean) : CalendarImportUiEvent

    /** User toggled the target entity type (Event <-> Task) for a specific item */
    data class ToggleTargetType(val eventId: String) : CalendarImportUiEvent

    /** Opens the edit dialog for the specified item */
    data class StartEditItem(val item: CalendarImportItem) : CalendarImportUiEvent

    /** Saves modifications from the edit dialog */
    data class SaveEditedItem(val item: CalendarImportItem) : CalendarImportUiEvent

    /** Dismisses the edit dialog without changes */
    data object DismissEdit : CalendarImportUiEvent

    /** Triggers batch import of all selected items into Planora */
    data object ImportSelectedEvents : CalendarImportUiEvent

    /** Navigates back to the previous screen */
    data object NavigateBack : CalendarImportUiEvent
}
