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
import com.yusufteker.pulse.feature.home.presentation.event_detail.EventDetailEvent
import com.yusufteker.pulse.feature.home.presentation.utils.encodeUrlParameter
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
            is EventDetailEvent.OnLoadEvent -> loadEvent(
                eventId = event.eventId, 
                planRoomId = event.planRoomId,
                sharedTitle = event.sharedTitle,
                sharedNote = event.sharedNote,
                sharedDate = event.sharedDate,
                sharedSender = event.sharedSender
            )
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
            is EventDetailEvent.OnParticipantToggled -> {
                val currentMap = _state.value.participants.toMutableMap()
                if (currentMap.containsKey(event.userId)) {
                    if (currentMap.size > 1) {
                        currentMap.remove(event.userId)
                        _state.update { it.copy(participants = currentMap) }
                    } else {
                        setEffect(EventDetailEffect.ShowToast("En az 1 katılımcı olmalıdır."))
                    }
                } else {
                    val user = _state.value.roomMembers.find { u -> u.id == event.userId }
                    if (user != null) {
                        currentMap[event.userId] = user.name
                        _state.update { it.copy(participants = currentMap) }
                    }
                }
            }
            EventDetailEvent.OnSaveClick -> saveEvent()
            EventDetailEvent.OnDeleteClick -> deleteEvent()
            EventDetailEvent.OnBackClick -> setEffect(EventDetailEffect.NavigateBack)
            EventDetailEvent.OnShareClick -> {
                viewModelScope.launch {
                    val sender = sessionPreferences.getUserName() ?: ""
                    val title = _state.value.title.encodeUrlParameter()
                    val note = _state.value.description.encodeUrlParameter()
                    val date = _state.value.startDateTimeMs
                    val senderEncoded = sender.encodeUrlParameter()
                    val eventId = _state.value.id
                    val roomId = _state.value.planRoomId
                    
                    val url = if (eventId != null && roomId != null) {
                        "https://pulse.yusufteker.com/share/joinEvent?eventId=$eventId&roomId=$roomId&title=$title&note=$note&date=$date&sender=$senderEncoded"
                    } else {
                        "https://pulse.yusufteker.com/share/event?title=$title&note=$note&date=$date&sender=$senderEncoded"
                    }
                    
                    val shareText = if (eventId != null && roomId != null) {
                        """
                            $sender seni ortak odadaki bir etkinliğe davet etti:
                            
                            Oda: Oda İsmi (Katıldığın oda)
                            Etkinlik: ${_state.value.title}
                            Tarih: ${kotlinx.datetime.Instant.fromEpochMilliseconds(_state.value.startDateTimeMs).toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date}
                            Açıklama: ${_state.value.description}
                            
                            Etkinliğe katılmak için tıklayınız:
                            $url
                        """.trimIndent()
                    } else {
                        """
                            $sender seni bir etkinliğe davet etti:
                            
                            ${_state.value.title}
                            ${_state.value.description}
                            
                            Pulsy'de aç: $url
                        """.trimIndent()
                    }
                    setEffect(EventDetailEffect.ShareItem(shareText))
                }
            }
        }
    }

    private fun loadEvent(
        eventId: String?, 
        planRoomId: String?,
        sharedTitle: String? = null,
        sharedNote: String? = null,
        sharedDate: Long? = null,
        sharedSender: String? = null
    ) {

        if (eventId == null) {
            viewModelScope.launch {
                val currentUserId = sessionPreferences.getUserId()?.toIntOrNull()
                val currentUserName = sessionPreferences.getUserName()
                val defaultParticipants = if (currentUserId != null && currentUserName != null && planRoomId != null) {
                    mapOf(currentUserId to currentUserName)
                } else emptyMap()

                val finalNote = buildString {
                    if (!sharedNote.isNullOrBlank()) append(sharedNote)
                    if (!sharedSender.isNullOrBlank()) {
                        if (isNotEmpty()) append("\n\n")
                        append("$sharedSender tarafından paylaşıldı.")
                    }
                }
                
                val now = com.yusufteker.pulse.core.utils.getCurrentTimeMs()
                val startTime = sharedDate ?: now
                val endTime = startTime + 3600000L // +1 hour

                _state.value = EventDetailState(
                    planRoomId = planRoomId, 
                    participants = defaultParticipants,
                    title = sharedTitle ?: "",
                    description = finalNote,
                    startDateTimeMs = startTime,
                    endDateTimeMs = endTime
                )
                if (planRoomId != null) {
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
                
                // Fetch sub-items that belong to this event
                val subItemsList = tasks.filter { it.parentId == eventId }
                
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
                            participants = task.participants.associate { it.userId to it.name },
                            recurrenceRule = ruleObj,
                            reminders = task.reminders,
                            subItems = subItemsList,
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
        
        if (currentState.isLoading) return

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
