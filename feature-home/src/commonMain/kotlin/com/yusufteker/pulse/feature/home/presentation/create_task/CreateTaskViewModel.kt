package com.yusufteker.pulse.feature.home.presentation.create_task

import androidx.lifecycle.viewModelScope
import com.yusufteker.pulse.core.base.BaseViewModel
import com.yusufteker.pulse.core.snackbar.SnackbarManager
import com.yusufteker.pulse.core.snackbar.SnackbarType
import com.yusufteker.pulse.feature.home.domain.repository.PlanRepository
import com.yusufteker.pulse.shared.api.CreateTaskRequest
import com.yusufteker.pulse.shared.api.TaskStatus
import com.yusufteker.pulse.shared.api.TaskVisibility
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import pulse.core.generated.resources.Res
import pulse.core.generated.resources.error_task_create_failed
import pulse.core.generated.resources.error_task_end_time_before_start
import pulse.core.generated.resources.error_task_room_required
import pulse.core.generated.resources.error_task_title_empty
import pulse.core.generated.resources.error_task_time_empty
import pulse.core.generated.resources.task_created_success
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import com.yusufteker.pulse.core.utils.getCurrentTimeMs
import kotlinx.datetime.plus

class CreateTaskViewModel(
    private val planRepository: PlanRepository,
    private val snackbarManager: SnackbarManager
) : BaseViewModel<CreateTaskState, CreateTaskEvent, CreateTaskEffect>(CreateTaskState()) {

    init {
        onEvent(CreateTaskEvent.LoadRooms)
    }

    override fun onEvent(event: CreateTaskEvent) {
        when (event) {
            is CreateTaskEvent.OnTitleChange -> setState { copy(title = event.title) }
            is CreateTaskEvent.OnDescriptionChange -> setState { copy(description = event.description) }
            is CreateTaskEvent.OnStartTimeChange -> setState { copy(startTime = event.time) }
            is CreateTaskEvent.OnEndTimeChange -> setState { copy(endTime = event.time) }
            
            is CreateTaskEvent.OnStartTimeUpdate -> {
                val currentMillis = state.value.startTime ?: getCurrentTimeMs()
                val newMillis = updateTimeOfTimestamp(currentMillis, event.hour, event.minute)
                setState { copy(startTime = newMillis) }
            }
            is CreateTaskEvent.OnEndTimeUpdate -> {
                val currentMillis = state.value.endTime ?: getCurrentTimeMs()
                val newMillis = updateTimeOfTimestamp(currentMillis, event.hour, event.minute)
                setState { copy(endTime = newMillis) }
            }
            
            is CreateTaskEvent.OnTaskTypeChange -> setState { copy(taskType = event.type) }
            is CreateTaskEvent.OnVisibilityChange -> {
                val newVisibility = event.visibility
                setState { 
                    copy(
                        visibility = newVisibility,
                        // Eğer gizli olursa seçili odaları sıfırla
                        selectedRoomIds = if (newVisibility == TaskVisibility.PRIVATE) emptySet() else selectedRoomIds
                    ) 
                }
            }
            is CreateTaskEvent.OnRoomToggle -> {
                val currentSet = state.value.selectedRoomIds.toMutableSet()
                if (event.isSelected) {
                    currentSet.add(event.roomId)
                } else {
                    currentSet.remove(event.roomId)
                }
                setState { copy(selectedRoomIds = currentSet) }
            }
            is CreateTaskEvent.OnAllDayToggle -> setState { copy(isAllDay = event.isAllDay) }
            is CreateTaskEvent.OnRecurringToggle -> setState { copy(isRecurring = event.isRecurring) }
            is CreateTaskEvent.OnDayOfWeekToggle -> {
                val currentDays = state.value.selectedDaysOfWeek.toMutableSet()
                if (currentDays.contains(event.day)) {
                    currentDays.remove(event.day)
                } else {
                    currentDays.add(event.day)
                }
                setState { copy(selectedDaysOfWeek = currentDays) }
            }
            CreateTaskEvent.LoadRooms -> {
                viewModelScope.launch {
                    planRepository.observeAllPlanRooms().collect { rooms ->
                        setState { copy(availableRooms = rooms) }
                    }
                }
            }
            CreateTaskEvent.OnBackClick -> {
                setEffect(CreateTaskEffect.NavigateBack)
            }
            CreateTaskEvent.Submit -> submitTask()
            is CreateTaskEvent.OnClearState -> clearState()
        }
    }

    private fun clearState() {
        setState { CreateTaskState(availableRooms = this.availableRooms) }
    }

    private fun submitTask() {
        val currentState = state.value
        
        viewModelScope.launch {
            if (currentState.title.isBlank()) {
                snackbarManager.showMessage(getString(Res.string.error_task_title_empty), SnackbarType.ERROR)
                return@launch
            }
            
            if (currentState.startTime == null) {
                snackbarManager.showMessage(getString(Res.string.error_task_time_empty), SnackbarType.ERROR)
                return@launch
            }
            val startTime = currentState.startTime
            if (currentState.visibility == TaskVisibility.ROOM_SHARED && currentState.selectedRoomIds.isEmpty()) {
                snackbarManager.showMessage(getString(Res.string.error_task_room_required), SnackbarType.ERROR)
                return@launch
            }
            
            if (currentState.endTime != null && currentState.endTime < startTime) {
                snackbarManager.showMessage(getString(Res.string.error_task_end_time_before_start), SnackbarType.ERROR)
                return@launch
            }

        // Build recurrence rule if needed
        var rrule: String? = null
        var finalStartTime = startTime
        
        if (currentState.isRecurring && currentState.selectedDaysOfWeek.isNotEmpty()) {
            val dayMap = mapOf(1 to "MO", 2 to "TU", 3 to "WE", 4 to "TH", 5 to "FR", 6 to "SA", 7 to "SU")
            val selectedDaysString = currentState.selectedDaysOfWeek.mapNotNull { dayMap[it] }.joinToString(",")
            rrule = "FREQ=WEEKLY;BYDAY=$selectedDaysString"
            
            finalStartTime = calculateNextOccurrenceMillis(startTime, currentState.selectedDaysOfWeek)
        }

        setState { copy(isLoading = true) }
        
        val request = CreateTaskRequest(
            title = currentState.title,
            description = currentState.description.takeIf { it.isNotBlank() },
            startTime = finalStartTime,
            endTime = currentState.endTime?.let { it + (finalStartTime - startTime) }, // Shift endTime by the same amount if exists
            type = currentState.taskType,
            status = TaskStatus.PENDING,
            visibility = currentState.visibility,
            sharedRoomIds = currentState.selectedRoomIds.toList(),
            isRecurring = currentState.isRecurring,
            recurrenceRule = rrule,
            isFlexible = false,
            isOptional = false,
            isPostponable = true,
            isAllDay = currentState.isAllDay
        )
        
        val result = planRepository.createTask(request)
        
        result.onSuccess {
            setState { copy(isLoading = false) }
            snackbarManager.showMessage(getString(Res.string.task_created_success), SnackbarType.SUCCESS)
            setEffect(CreateTaskEffect.NavigateBack)
        }.onFailure {
            setState { copy(isLoading = false) }
            val errorMsg = getString(Res.string.error_task_create_failed, it.message ?: "")
            snackbarManager.showMessage(errorMsg, SnackbarType.ERROR)
        }
        }
    }
    
    private fun updateTimeOfTimestamp(millis: Long, hour: Int, minute: Int): Long {
        val instant = Instant.fromEpochMilliseconds(millis)
        val tz = TimeZone.currentSystemDefault()
        val local = instant.toLocalDateTime(tz)
        val updated = LocalDateTime(local.year, local.monthNumber, local.dayOfMonth, hour, minute, 0, 0)
        return updated.toInstant(tz).toEpochMilliseconds()
    }
    
    private fun calculateNextOccurrenceMillis(currentMillis: Long, selectedDays: Set<Int>): Long {
        if (selectedDays.isEmpty()) return currentMillis
        
        val instant = Instant.fromEpochMilliseconds(currentMillis)
        val tz = TimeZone.currentSystemDefault()
        val local = instant.toLocalDateTime(tz)
        
        val currentDayOfWeek = local.dayOfWeek.ordinal + 1 // 1 (Monday) to 7 (Sunday)
        
        // Find the minimum days to add
        val minDaysToAdd = selectedDays.map { targetDay ->
            if (targetDay >= currentDayOfWeek) {
                targetDay - currentDayOfWeek
            } else {
                7 - (currentDayOfWeek - targetDay)
            }
        }.minOrNull() ?: 0
        
        if (minDaysToAdd == 0) return currentMillis
        
        // Add days
        val currentDate = kotlinx.datetime.LocalDate(local.year, local.monthNumber, local.dayOfMonth)
        val nextDate = currentDate.plus(kotlinx.datetime.DatePeriod(days = minDaysToAdd))
        val nextDateTime = LocalDateTime(nextDate.year, nextDate.monthNumber, nextDate.dayOfMonth, local.hour, local.minute, 0, 0)
        
        return nextDateTime.toInstant(tz).toEpochMilliseconds()
    }
}
