package com.yusufteker.pulse.feature.home.presentation.event_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufteker.pulse.feature.home.domain.repository.PlanRepository
import com.yusufteker.pulse.feature.home.domain.repository.ProfileRepository
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class EventDetailViewModel(
    private val planRepository: PlanRepository,
    private val profileRepository: ProfileRepository,
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
            is EventDetailEvent.OnRecurrenceRuleChanged -> {
                _state.update { currentState ->
                    currentState.copy(recurrenceRule = event.rule, isRecurring = event.rule != null)
                }
            }
            
            is EventDetailEvent.OnReminderPickerVisibilityChanged -> _state.update { it.copy(isReminderPickerVisible = event.isVisible) }
            is EventDetailEvent.OnReminderToggled -> {
                _state.update {
                    val newReminders = if (it.reminders.contains(event.minutes)) {
                        it.reminders - event.minutes
                    } else {
                        it.reminders + event.minutes
                    }
                    it.copy(reminders = newReminders)
                }
            }
            
            is EventDetailEvent.OnParticipantPickerVisibilityChanged -> _state.update { it.copy(isParticipantPickerVisible = event.isVisible) }
            is EventDetailEvent.OnParticipantToggled -> _state.update {
                val currentMap = it.participants.toMutableMap()
                if (currentMap.containsKey(event.userId)) {
                    currentMap.remove(event.userId)
                } else {
                    val user = it.roomMembers.find { u -> u.id == event.userId }
                    if (user != null) {
                        currentMap[event.userId] = user.name
                    }
                }
                it.copy(participants = currentMap)
            }
            EventDetailEvent.OnSaveClick -> saveEvent()
            EventDetailEvent.OnDeleteClick -> deleteEvent()
            EventDetailEvent.OnBackClick -> setEffect(EventDetailEffect.NavigateBack)
        }
    }

    private fun loadEvent(eventId: String?, planRoomId: String?) {

        if (eventId == null) {
            _state.value = EventDetailState(planRoomId = planRoomId)
            if (planRoomId != null) {
                viewModelScope.launch {
                    planRepository.observeAllPlanRooms().collect { rooms ->
                        val room = rooms.find { it.id == planRoomId }
                        if (room != null) {
                            val membersList = mutableListOf<com.yusufteker.pulse.shared.api.UserProfileResponse>()
                            room.members.forEach { member ->
                                profileRepository.getProfile(member.userId.toString()).onSuccess { profile ->
                                    membersList.add(profile)
                                }
                            }
                            _state.update { it.copy(roomMembers = membersList) }
                        }
                    }
                }
            }
            return
        }


        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, id = eventId, planRoomId = planRoomId) }

        // Fetch room members if planRoomId is present
        if (planRoomId != null) {
            viewModelScope.launch {
                planRepository.observeAllPlanRooms().collect { rooms ->
                    val room = rooms.find { it.id == planRoomId }
                    if (room != null) {
                        val membersList = mutableListOf<com.yusufteker.pulse.shared.api.UserProfileResponse>()
                        room.members.forEach { member ->
                            profileRepository.getProfile(member.userId.toString()).onSuccess { profile ->
                                membersList.add(profile)
                            }
                        }
                        _state.update { it.copy(roomMembers = membersList) }
                    }
                }
            }
        }

            planRepository.observeAllTasks().collect { tasks ->
                val task = tasks.find { it.id == eventId && it.type == TaskType.EVENT }
                if (task != null) {
                    val location = (task.specificDetails as? ItemDetails.Event)?.location ?: ""
                    val ruleObj = try {
                        task.recurrenceRule?.let { Json.decodeFromString<com.yusufteker.pulse.shared.api.RecurrenceRule>(it) }
                    } catch (e: Exception) {
                        null
                    }
                    
                    _state.update { 
                        it.copy(
                            title = task.title,
                            description = task.description ?: "",
                            location = location,
                            startDateTimeMs = task.startTime,
                            endDateTimeMs = task.endTime ?: task.startTime,
                            isRecurring = task.isRecurring,
                            participants = task.participants,
                            recurrenceRule = ruleObj,
                            reminders = task.reminders,
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

            val recurrenceStr = currentState.recurrenceRule?.let { Json.encodeToString(it) }

            val request = com.yusufteker.pulse.shared.api.CreateTaskRequest(
                title = currentState.title,
                description = currentState.description.ifBlank { null },
                startTime = currentState.startDateTimeMs,
                endTime = currentState.endDateTimeMs,
                type = TaskType.EVENT,
                status = TaskStatus.PENDING,
                visibility = if (currentState.planRoomId != null) TaskVisibility.ROOM_SHARED else TaskVisibility.PRIVATE,
                sharedRoomIds = currentState.planRoomId?.let { listOf(it) } ?: emptyList(),
                isRecurring = currentState.isRecurring || currentState.recurrenceRule != null,
                recurrenceRule = recurrenceStr,
                isFlexible = false,
                isOptional = false,
                isPostponable = false,
                isAllDay = false,
                reminders = currentState.reminders,
                specificDetails = ItemDetails.Event(location = currentState.location.ifBlank { null }, meetingUrl = null),
                tags = emptyList(),
                color = null,
                participants = currentState.participants
            )

            if (currentState.id != null) {
                planRepository.updateTask(currentState.id, request)
            } else {
                planRepository.createTask(request)
            }
            
            _state.update { it.copy(isLoading = false) }
            setEffect(EventDetailEffect.NavigateBack)
        }
    }

    private fun deleteEvent() {
        val eventId = _state.value.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            planRepository.deleteTask(eventId)
            _state.update { it.copy(isLoading = false) }
            setEffect(EventDetailEffect.NavigateBack)
        }
    }

    private fun setEffect(effect: EventDetailEffect) {
        viewModelScope.launch {
            _effect.emit(effect)
        }
    }
    
}
