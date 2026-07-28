package com.yusufteker.planora.feature.home.presentation.event_editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufteker.planora.feature.home.domain.repository.PlanRepository
import com.yusufteker.planora.feature.home.domain.repository.ProfileRepository
import com.yusufteker.planora.shared.api.ItemDetails
import com.yusufteker.planora.shared.api.TaskStatus
import com.yusufteker.planora.shared.api.TaskType
import com.yusufteker.planora.shared.api.TaskVisibility
import com.yusufteker.planora.feature.home.presentation.utils.encodeUrlParameter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.yusufteker.planora.shared.api.extractBaseTaskId

/**
 * ViewModel for creating and editing events in Planora.
 * Controls field state, auto-saving drafts, and persisting to local DB / remote server.
 */
class EventEditorViewModel(
    private val planRepository: PlanRepository,
    private val profileRepository: ProfileRepository,
    private val sessionPreferences: com.yusufteker.planora.core.preferences.SessionPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(EventEditorState())
    val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<EventEditorEffect>()
    val effect = _effect.asSharedFlow()

    private val _saveTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        @OptIn(kotlinx.coroutines.FlowPreview::class)
        _saveTrigger
            .debounce(1000L)
            .onEach { autoSaveEvent() }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: EventEditorEvent) {
        when (event) {
            is EventEditorEvent.OnLoadEvent -> loadEvent(
                eventId = event.eventId, 
                planRoomId = event.planRoomId,
                sharedTitle = event.sharedTitle,
                sharedNote = event.sharedNote,
                sharedDate = event.sharedDate,
                sharedSender = event.sharedSender,
                copyFromEventId = event.copyFromEventId
            )
            is EventEditorEvent.OnTitleChange -> {
                _state.update { it.copy(title = event.title) }
                _saveTrigger.tryEmit(Unit)
            }
            is EventEditorEvent.OnDescriptionChange -> {
                _state.update { it.copy(description = event.description) }
                _saveTrigger.tryEmit(Unit)
            }
            is EventEditorEvent.OnLocationChange -> {
                _state.update { it.copy(location = event.location) }
                _saveTrigger.tryEmit(Unit)
            }
            
            is EventEditorEvent.OnStartDateTimeSelected -> {
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
                _saveTrigger.tryEmit(Unit)
            }
            is EventEditorEvent.OnEndDateTimeSelected -> {
                _state.update { 
                    it.copy(endDateTimeMs = event.dateMs, isEndPickerOpen = false, isEndTimeManuallyChanged = true) 
                }
                _saveTrigger.tryEmit(Unit)
            }
            
            is EventEditorEvent.OnStartPickerVisibilityChanged -> _state.update { it.copy(isStartPickerOpen = event.isVisible) }
            is EventEditorEvent.OnEndPickerVisibilityChanged -> _state.update { it.copy(isEndPickerOpen = event.isVisible) }
            is EventEditorEvent.OnRepeatPickerVisibilityChanged -> _state.update { it.copy(isRepeatPickerOpen = event.isVisible) }
            
            is EventEditorEvent.OnToggleRecurring -> {
                _state.update { it.copy(isRecurring = event.isRecurring) }
                _saveTrigger.tryEmit(Unit)
            }
            is EventEditorEvent.OnRecurrenceRuleChanged -> {
                _state.update { currentState ->
                    currentState.copy(recurrenceRule = event.rule, isRecurring = event.rule != null)
                }
                _saveTrigger.tryEmit(Unit)
            }
            
            is EventEditorEvent.OnReminderPickerVisibilityChanged -> _state.update { it.copy(isReminderPickerVisible = event.isVisible) }
            is EventEditorEvent.OnReminderToggled -> {
                _state.update {
                    val newReminders = if (it.reminders.contains(event.minutes)) {
                        it.reminders - event.minutes
                    } else {
                        it.reminders + event.minutes
                    }
                    it.copy(reminders = newReminders)
                }
                _saveTrigger.tryEmit(Unit)
            }
            
            is EventEditorEvent.OnParticipantPickerVisibilityChanged -> _state.update { it.copy(isParticipantPickerVisible = event.isVisible) }
            is EventEditorEvent.OnParticipantToggled -> {
                val currentMap = _state.value.participants.toMutableMap()
                if (currentMap.containsKey(event.userId)) {
                    if (currentMap.size > 1) {
                        currentMap.remove(event.userId)
                        _state.update { it.copy(participants = currentMap) }
                        _saveTrigger.tryEmit(Unit)
                    } else {
                        setEffect(EventEditorEffect.ShowToast("En az 1 katılımcı olmalıdır."))
                    }
                } else {
                    val user = _state.value.roomMembers.find { u -> u.id == event.userId }
                    if (user != null) {
                        currentMap[event.userId] = user.name
                        _state.update { it.copy(participants = currentMap) }
                        _saveTrigger.tryEmit(Unit)
                    }
                }
            }
            EventEditorEvent.OnSaveClick -> saveEvent()
            EventEditorEvent.OnDeleteClick -> deleteEvent()
            EventEditorEvent.OnBackClick -> setEffect(EventEditorEffect.NavigateBack)
            EventEditorEvent.OnShareClick -> {
                viewModelScope.launch {
                    val sender = sessionPreferences.getUserName() ?: ""
                    val title = _state.value.title.encodeUrlParameter()
                    val note = _state.value.description.encodeUrlParameter()
                    val date = _state.value.startDateTimeMs
                    val senderEncoded = sender.encodeUrlParameter()
                    val eventId = _state.value.id
                    val roomId = _state.value.planRoomId
                    
                    val url = if (eventId != null && roomId != null) {
                        "https://planora.yusufteker.com/share/joinEvent?eventId=$eventId&roomId=$roomId&title=$title&note=$note&date=$date&sender=$senderEncoded"
                    } else {
                        "https://planora.yusufteker.com/share/event?title=$title&note=$note&date=$date&sender=$senderEncoded"
                    }
                    
                    val shareText = if (eventId != null && roomId != null) {
                        """
                            $sender seni ortak odadaki bir etkinliğe davet etti:
                            
                            Oda: Oda İsmi (Katıldığın oda)
                            Etkinlik: ${_state.value.title}
                            Tarih: ${kotlinx.datetime.Instant.fromEpochMilliseconds(_state.value.startDateTimeMs).toLocalDateTime(TimeZone.currentSystemDefault()).date}
                            Açıklama: ${_state.value.description}
                            
                            Etkinliğe katılmak için tıklayınız:
                            $url
                        """.trimIndent()
                    } else {
                        """
                            $sender seni bir etkinliğe davet etti:
                            
                            ${_state.value.title}
                            ${_state.value.description}
                            
                            Planora'de aç: $url
                        """.trimIndent()
                    }
                    setEffect(EventEditorEffect.ShareItem(shareText))
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
        sharedSender: String? = null,
        copyFromEventId: String? = null
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
                
                val now = com.yusufteker.planora.core.utils.getCurrentTimeMs()
                val startTime = sharedDate ?: now
                val endTime = startTime + 3600000L // +1 hour

                _state.value = EventEditorState(
                    planRoomId = planRoomId, 
                    participants = defaultParticipants,
                    title = sharedTitle ?: "",
                    description = finalNote,
                    startDateTimeMs = startTime,
                    endDateTimeMs = endTime
                )

                if (!copyFromEventId.isNullOrBlank()) {
                    val copyBaseId = copyFromEventId.extractBaseTaskId()
                    planRepository.observeAllTasks().collect { tasks ->
                        val sourceEvent = tasks.find { it.id == copyBaseId && it.type == TaskType.EVENT }
                        if (sourceEvent != null) {
                            val details = sourceEvent.specificDetails as? com.yusufteker.planora.shared.api.ItemDetails.Event
                            val ruleObj = try {
                                sourceEvent.recurrenceRule?.let { Json.decodeFromString<com.yusufteker.planora.shared.api.RecurrenceRule>(it) }
                            } catch (e: Exception) { null }

                            val targetRoomId = planRoomId ?: sourceEvent.sharedRoomIds.firstOrNull()

                            _state.update { currentState ->
                                currentState.copy(
                                    id = null,
                                    isCopyMode = true,
                                    title = sourceEvent.title,
                                    description = sourceEvent.description ?: "",
                                    location = details?.location ?: "",
                                    startDateTimeMs = sourceEvent.startTime ?: startTime,
                                    endDateTimeMs = sourceEvent.endTime ?: endTime,
                                    isRecurring = sourceEvent.isRecurring,
                                    recurrenceRule = ruleObj,
                                    reminders = sourceEvent.reminders,
                                    planRoomId = targetRoomId
                                )
                            }
                        }
                    }
                    return@launch
                }
                if (planRoomId != null) {
                    planRepository.observeAllPlanRooms().collect { rooms ->
                        val room = rooms.find { it.id == planRoomId }
                        if (room != null) {
                            val membersList = mutableListOf<com.yusufteker.planora.shared.api.UserProfileResponse>()
                            room.members.forEach { member ->
                                profileRepository.getProfile(member.userId.toString()).onSuccess { profile ->
                                    membersList.add(profile)
                                }
                            }
                            _state.update { it.copy(roomMembers = membersList, planRoomName = room.name) }
                        }
                    }
                }
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, id = eventId, planRoomId = planRoomId) }

            if (planRoomId != null) {
                viewModelScope.launch {
                    planRepository.observeAllPlanRooms().collect { rooms ->
                        val room = rooms.find { it.id == planRoomId }
                        if (room != null) {
                            val membersList = mutableListOf<com.yusufteker.planora.shared.api.UserProfileResponse>()
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
                val subItemsList = tasks.filter { it.parentId == eventId }
                
                if (task != null) {
                    val actualPlanRoomId = _state.value.planRoomId ?: task.sharedRoomIds.firstOrNull()
                    if (actualPlanRoomId != null) {
                        viewModelScope.launch {
                            planRepository.observeAllPlanRooms().collect { rooms ->
                                val name = rooms.find { it.id == actualPlanRoomId }?.name
                                if (name != null) {
                                    _state.update { it.copy(planRoomId = actualPlanRoomId, planRoomName = name) }
                                }
                            }
                        }
                    }

                    val location = (task.specificDetails as? ItemDetails.Event)?.location ?: ""
                    val ruleObj = try {
                        task.recurrenceRule?.let { Json.decodeFromString<com.yusufteker.planora.shared.api.RecurrenceRule>(it) }
                    } catch (e: Exception) {
                        null
                    }
                    
                    _state.update { 
                        it.copy(
                            planRoomId = actualPlanRoomId,
                            title = task.title,
                            description = task.description ?: "",
                            location = location,
                            startDateTimeMs = task.startTime,
                            endDateTimeMs = task.endTime ?: task.startTime,
                            isRecurring = task.isRecurring,
                            participants = task.participants.associate { p -> p.userId to p.name },
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

    private fun autoSaveEvent() {
        val currentState = _state.value
        if (currentState.isLoading || currentState.title.isBlank()) return

        viewModelScope.launch {
            val recurrenceStr = currentState.recurrenceRule?.let { Json.encodeToString(it) }

            val request = com.yusufteker.planora.shared.api.CreateTaskRequest(
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

            if (currentState.id == null) return@launch
            planRepository.updateTask(currentState.id, request, triggerSync = false)
        }
    }

    private fun saveEvent() {
        val currentState = _state.value
        if (currentState.isLoading) return

        if (currentState.title.isBlank()) {
            setEffect(EventEditorEffect.ShowToast("Lütfen bir başlık girin."))
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val recurrenceStr = currentState.recurrenceRule?.let { Json.encodeToString(it) }

            val request = com.yusufteker.planora.shared.api.CreateTaskRequest(
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
                planRepository.updateTask(currentState.id, request, triggerSync = true)
            } else {
                planRepository.createTask(request, triggerSync = true)
            }
            
            _state.update { it.copy(isLoading = false) }
            setEffect(EventEditorEffect.NavigateBack)
        }
    }

    private fun deleteEvent() {
        val eventId = _state.value.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            planRepository.deleteTask(eventId)
            _state.update { it.copy(isLoading = false) }
            setEffect(EventEditorEffect.NavigateBack)
        }
    }

    private fun setEffect(effect: EventEditorEffect) {
        viewModelScope.launch {
            _effect.emit(effect)
        }
    }
}
