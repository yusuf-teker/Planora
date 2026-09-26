package com.yusufteker.planora.feature.home.presentation.calendar_import

import com.yusufteker.planora.core.base.BaseViewModel
import com.yusufteker.planora.core.calendar.CalendarImportItem
import com.yusufteker.planora.core.calendar.CalendarSyncManager
import com.yusufteker.planora.feature.home.domain.repository.PlanRepository
import com.yusufteker.planora.shared.api.CreateTaskRequest
import com.yusufteker.planora.shared.api.ItemDetails
import com.yusufteker.planora.shared.api.TaskPriority
import com.yusufteker.planora.shared.api.TaskStatus
import com.yusufteker.planora.shared.api.TaskType
import com.yusufteker.planora.shared.api.TaskVisibility
import planora.core.generated.resources.Res
import planora.core.generated.resources.calendar_import_error
import planora.core.generated.resources.calendar_import_success

import com.yusufteker.planora.core.calendar.CalendarService

/**
 * ViewModel managing the lifecycle, native calendar queries, user event edits,
 * and batch importing into Planora.
 *
 * Adheres strictly to the project's Offline-First architecture:
 * All events are saved locally via [PlanRepository.createTask] before triggering background sync.
 *
 * @param calendarService Platform bridge for native calendar access.
 * @param planRepository Data layer for local database persistence and remote synchronization.
 */
class CalendarImportViewModel(
    private val calendarService: CalendarService,
    private val planRepository: PlanRepository
) : BaseViewModel<CalendarImportState, CalendarImportUiEvent, CalendarImportEffect>(CalendarImportState()) {

    init {
        checkPermissionAndLoad()
    }

    override fun onEvent(event: CalendarImportUiEvent) {
        when (event) {
            is CalendarImportUiEvent.CheckPermission -> checkPermissionAndLoad()
            is CalendarImportUiEvent.PermissionResult -> handlePermissionResult(event.isGranted)
            is CalendarImportUiEvent.SelectDateRange -> {
                setState { copy(selectedDateRange = event.range) }
                loadEvents()
            }
            is CalendarImportUiEvent.SelectCalendarFilter -> {
                setState { copy(selectedCalendarFilter = event.calendarName) }
            }
            is CalendarImportUiEvent.ToggleEventSelection -> toggleEventSelection(event.eventId)
            is CalendarImportUiEvent.ToggleSelectAll -> toggleSelectAll(event.selectAll)
            is CalendarImportUiEvent.ToggleTargetType -> toggleTargetType(event.eventId)
            is CalendarImportUiEvent.StartEditItem -> setState { copy(editingItem = event.item) }
            is CalendarImportUiEvent.SaveEditedItem -> saveEditedItem(event.item)
            is CalendarImportUiEvent.DismissEdit -> setState { copy(editingItem = null) }
            is CalendarImportUiEvent.ImportSelectedEvents -> importSelectedEvents()
            is CalendarImportUiEvent.NavigateBack -> setEffect(CalendarImportEffect.NavigateBack)
        }
    }

    private fun checkPermissionAndLoad() {
        val hasPerm = calendarService.hasCalendarReadPermission()
        setState { copy(hasPermission = hasPerm, isPermissionDenied = false) }
        if (hasPerm) {
            loadEvents()
        }
    }

    private fun handlePermissionResult(isGranted: Boolean) {
        if (isGranted) {
            setState { copy(hasPermission = true, isPermissionDenied = false) }
            loadEvents()
        } else {
            setState { copy(hasPermission = false, isPermissionDenied = true, isLoading = false) }
        }
    }

    private fun loadEvents() {
        launch {
            try {
                setState { copy(isLoading = true) }
                val (startMs, endMs) = state.value.selectedDateRange.getEpochRange()
                val retrieved = calendarService.fetchCalendarEvents(startMs, endMs)

                val distinctCalendars = retrieved.mapNotNull { it.calendarName }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()

                setState {
                    copy(
                        isLoading = false,
                        events = retrieved,
                        availableCalendars = distinctCalendars
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                setState { copy(isLoading = false) }
            }
        }
    }

    private fun toggleEventSelection(eventId: String) {
        setState {
            val updated = events.map { item ->
                if (item.id == eventId) item.copy(isSelected = !item.isSelected) else item
            }
            copy(events = updated)
        }
    }

    private fun toggleSelectAll(selectAll: Boolean) {
        setState {
            val filter = selectedCalendarFilter
            val updated = events.map { item ->
                if (filter == null || item.calendarName == filter) {
                    item.copy(isSelected = selectAll)
                } else {
                    item
                }
            }
            copy(events = updated)
        }
    }

    private fun toggleTargetType(eventId: String) {
        setState {
            val updated = events.map { item ->
                if (item.id == eventId) {
                    val nextType = if (item.targetType == TaskType.EVENT) TaskType.TASK else TaskType.EVENT
                    item.copy(targetType = nextType)
                } else {
                    item
                }
            }
            copy(events = updated)
        }
    }

    private fun saveEditedItem(updatedItem: CalendarImportItem) {
        setState {
            val updated = events.map { item ->
                if (item.id == updatedItem.id) updatedItem else item
            }
            copy(events = updated, editingItem = null)
        }
    }

    private fun importSelectedEvents() {
        val selectedItems = state.value.filteredEvents.filter { it.isSelected }
        if (selectedItems.isEmpty()) return

        launch {
            try {
                setState { copy(isImporting = true) }

                for (item in selectedItems) {
                    val req = if (item.targetType == TaskType.EVENT) {
                        CreateTaskRequest(
                            title = item.title.trim().ifBlank { "Event" },
                            description = item.description?.trim()?.ifBlank { null },
                            startTime = item.startTimeEpochMillis,
                            endTime = item.endTimeEpochMillis ?: (item.startTimeEpochMillis + 3600000L),
                            type = TaskType.EVENT,
                            status = TaskStatus.PENDING,
                            visibility = TaskVisibility.PRIVATE,
                            isAllDay = item.isAllDay,
                            specificDetails = ItemDetails.Event(
                                location = item.location?.trim()?.ifBlank { null }
                            )
                        )
                    } else {
                        CreateTaskRequest(
                            title = item.title.trim().ifBlank { "Task" },
                            description = item.description?.trim()?.ifBlank { null },
                            startTime = item.startTimeEpochMillis,
                            endTime = item.endTimeEpochMillis,
                            type = TaskType.TASK,
                            status = TaskStatus.PENDING,
                            visibility = TaskVisibility.PRIVATE,
                            isAllDay = item.isAllDay,
                            specificDetails = ItemDetails.Task(
                                priority = item.priority,
                                deadline = item.endTimeEpochMillis ?: item.startTimeEpochMillis
                            )
                        )
                    }

                    // Save locally first to guarantee zero data loss and offline resilience
                    planRepository.createTask(req, triggerSync = false)
                }

                // Trigger background server synchronization once for the entire batch
                planRepository.syncPendingChanges()

                setState { copy(isImporting = false) }
                setEffect(CalendarImportEffect.ShowSnackbar(Res.string.calendar_import_success, count = selectedItems.size))
                setEffect(CalendarImportEffect.NavigateBack)
            } catch (e: Exception) {
                e.printStackTrace()
                setState { copy(isImporting = false) }
                setEffect(CalendarImportEffect.ShowSnackbar(Res.string.calendar_import_error))
            }
        }
    }
}
