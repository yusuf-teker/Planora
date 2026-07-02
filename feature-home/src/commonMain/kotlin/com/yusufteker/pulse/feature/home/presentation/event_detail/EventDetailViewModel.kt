package com.yusufteker.pulse.feature.home.presentation.event_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufteker.pulse.feature.home.domain.repository.PlanRepository
import com.yusufteker.pulse.shared.api.ItemDetails
import com.yusufteker.pulse.shared.api.TaskDto
import com.yusufteker.pulse.shared.api.TaskStatus
import com.yusufteker.pulse.shared.api.TaskType
import com.yusufteker.pulse.shared.api.TaskVisibility
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.Instant
import kotlinx.datetime.toLocalDateTime

class EventDetailViewModel(
    private val planRepository: PlanRepository,
    private val sessionPreferences: com.yusufteker.pulse.core.preferences.SessionPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(EventDetailState())
    val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<EventDetailEffect>()
    val effect = _effect.asSharedFlow()

    fun onEvent(event: EventDetailEvent) {
        when (event) {
            is EventDetailEvent.OnLoadEvent -> loadEvent(event.eventId, event.planRoomId)
            is EventDetailEvent.OnTitleChange -> _state.update { it.copy(title = event.title) }
            is EventDetailEvent.OnDescriptionChange -> _state.update { it.copy(description = event.description) }
            is EventDetailEvent.OnLocationChange -> _state.update { it.copy(location = event.location) }
            
            is EventDetailEvent.OnStartDateTimeSelected -> {
                _state.update { 
                    if (!it.isEndTimeManuallyChanged) {
                        val duration = it.endDateTimeMs - it.startDateTimeMs
                        it.copy(
                            startDateTimeMs = event.dateMs,
                            endDateTimeMs = event.dateMs + duration,
                            isStartPickerOpen = false
                        )
                    } else {
                        it.copy(startDateTimeMs = event.dateMs, isStartPickerOpen = false)
                    }
                }
            }
            is EventDetailEvent.OnEndDateTimeSelected -> _state.update { 
                it.copy(endDateTimeMs = event.dateMs, isEndPickerOpen = false, isEndTimeManuallyChanged = true) 
            }
            
            is EventDetailEvent.OnStartPickerVisibilityChanged -> _state.update { it.copy(isStartPickerOpen = event.isVisible) }
            is EventDetailEvent.OnEndPickerVisibilityChanged -> _state.update { it.copy(isEndPickerOpen = event.isVisible) }
            is EventDetailEvent.OnRepeatPickerVisibilityChanged -> _state.update { it.copy(isRepeatPickerOpen = event.isVisible) }
            
            is EventDetailEvent.OnToggleRecurring -> _state.update { it.copy(isRecurring = event.isRecurring) }
            is EventDetailEvent.OnToggleDayOfWeek -> {
                _state.update { currentState ->
                    val newDays = currentState.selectedDaysOfWeek.toMutableSet()
                    if (newDays.contains(event.dayOfWeek)) {
                        newDays.remove(event.dayOfWeek)
                    } else {
                        newDays.add(event.dayOfWeek)
                    }
                    currentState.copy(selectedDaysOfWeek = newDays, isRecurring = newDays.isNotEmpty())
                }
            }
            
            EventDetailEvent.OnSaveClick -> saveEvent()
            EventDetailEvent.OnDeleteClick -> deleteEvent()
            EventDetailEvent.OnBackClick -> setEffect(EventDetailEffect.NavigateBack)
        }
    }

    private fun loadEvent(eventId: String?, planRoomId: String?) {
        if (eventId == null) {
            _state.value = EventDetailState(planRoomId = planRoomId)
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, id = eventId, planRoomId = planRoomId) }
            planRepository.observeAllTasks().collect { tasks ->
                val task = tasks.find { it.id == eventId && it.type == TaskType.EVENT }
                if (task != null) {
                    val location = (task.specificDetails as? ItemDetails.Event)?.location ?: ""
                    val days = parseRecurrenceDays(task.recurrenceRule)
                    
                    _state.update { 
                        it.copy(
                            title = task.title,
                            description = task.description ?: "",
                            location = location,
                            startDateTimeMs = task.startTime,
                            endDateTimeMs = task.endTime ?: task.startTime,
                            isRecurring = task.isRecurring,
                            selectedDaysOfWeek = days,
                            isLoading = false
                        ) 
                    }
                } else {
                    _state.update { it.copy(isLoading = false, error = "Etkinlik bulunamadı") }
                }
            }
        }
    }

    private fun saveEvent() {
        val currentState = _state.value
        if (currentState.title.isBlank()) {
            setEffect(EventDetailEffect.ShowToast("Lütfen bir başlık girin."))
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val recurrenceRule = if (currentState.selectedDaysOfWeek.isNotEmpty()) {
                buildRecurrenceRule(currentState.selectedDaysOfWeek)
            } else null

            val request = com.yusufteker.pulse.shared.api.CreateTaskRequest(
                title = currentState.title,
                description = currentState.description.ifBlank { null },
                startTime = currentState.startDateTimeMs,
                endTime = currentState.endDateTimeMs,
                type = TaskType.EVENT,
                status = TaskStatus.PENDING,
                visibility = TaskVisibility.PRIVATE,
                sharedRoomIds = currentState.planRoomId?.let { listOf(it) } ?: emptyList(),
                isRecurring = currentState.isRecurring || currentState.selectedDaysOfWeek.isNotEmpty(),
                recurrenceRule = recurrenceRule,
                isFlexible = false,
                isOptional = false,
                isPostponable = false,
                isAllDay = false,
                reminders = emptyList(),
                specificDetails = ItemDetails.Event(location = currentState.location.ifBlank { null }, meetingUrl = null),
                tags = emptyList(),
                color = null
            )

            planRepository.createTask(request)
            _state.update { it.copy(isLoading = false) }
            setEffect(EventDetailEffect.NavigateBack)
        }
    }

    private fun deleteEvent() {
        val eventId = _state.value.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            // planRepository.deleteTask(eventId)
            _state.update { it.copy(isLoading = false) }
            setEffect(EventDetailEffect.NavigateBack)
        }
    }

    private fun setEffect(effect: EventDetailEffect) {
        viewModelScope.launch {
            _effect.emit(effect)
        }
    }
    
    private fun buildRecurrenceRule(days: Set<Int>): String {
        val dayMap = mapOf(1 to "MO", 2 to "TU", 3 to "WE", 4 to "TH", 5 to "FR", 6 to "SA", 7 to "SU")
        val dayString = days.mapNotNull { dayMap[it] }.joinToString(",")
        return "FREQ=WEEKLY;BYDAY=$dayString"
    }
    
    private fun parseRecurrenceDays(rule: String?): Set<Int> {
        if (rule == null || !rule.contains("BYDAY=")) return emptySet()
        val byDayPart = rule.substringAfter("BYDAY=").substringBefore(";")
        val dayMap = mapOf("MO" to 1, "TU" to 2, "WE" to 3, "TH" to 4, "FR" to 5, "SA" to 6, "SU" to 7)
        return byDayPart.split(",").mapNotNull { dayMap[it] }.toSet()
    }
}
