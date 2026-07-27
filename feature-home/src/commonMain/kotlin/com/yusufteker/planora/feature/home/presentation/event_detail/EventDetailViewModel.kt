package com.yusufteker.planora.feature.home.presentation.event_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufteker.planora.feature.home.domain.repository.PlanRepository
import com.yusufteker.planora.feature.home.domain.repository.ProfileRepository
import com.yusufteker.planora.shared.api.ItemDetails
import com.yusufteker.planora.shared.api.TaskType
import com.yusufteker.planora.feature.home.presentation.utils.encodeUrlParameter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json

/**
 * ViewModel for viewing Event details in read-only mode.
 */
class EventDetailViewModel(
    private val planRepository: PlanRepository,
    private val profileRepository: ProfileRepository,
    private val sessionPreferences: com.yusufteker.planora.core.preferences.SessionPreferences
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
            EventDetailEvent.OnEditClick -> {
                val currentId = _state.value.eventId
                if (currentId != null) {
                    setEffect(EventDetailEffect.NavigateToEditEvent(currentId, _state.value.planRoomId))
                }
            }
            EventDetailEvent.OnDeleteClick -> deleteEvent()
            EventDetailEvent.OnBackClick -> setEffect(EventDetailEffect.NavigateBack)
            EventDetailEvent.OnShareClick -> shareEvent()
        }
    }

    private fun loadEvent(
        eventId: String?,
        planRoomId: String?,
        sharedTitle: String?,
        sharedNote: String?,
        sharedDate: Long?,
        sharedSender: String?
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, eventId = eventId, planRoomId = planRoomId) }

            if (planRoomId != null) {
                viewModelScope.launch {
                    planRepository.observeAllPlanRooms().collect { rooms ->
                        val room = rooms.find { it.id == planRoomId }
                        if (room != null) {
                            _state.update { it.copy(planRoomName = room.name, planRoomImageUrl = room.imageUrl) }
                        }
                    }
                }
            }

            planRepository.observeAllTasks().collect { tasks ->
                val task = tasks.find { it.id == eventId && it.type == TaskType.EVENT }
                val subItemsList = tasks.filter { it.parentId == eventId }

                if (task != null) {
                    val actualPlanRoomId = _state.value.planRoomId ?: task.sharedRoomIds.firstOrNull()
                    if (actualPlanRoomId != null && _state.value.planRoomName == null) {
                        viewModelScope.launch {
                            planRepository.observeAllPlanRooms().collect { rooms ->
                                val room = rooms.find { it.id == actualPlanRoomId }
                                if (room != null) {
                                    _state.update { it.copy(planRoomId = actualPlanRoomId, planRoomName = room.name, planRoomImageUrl = room.imageUrl) }
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
                            event = task,
                            planRoomId = actualPlanRoomId,
                            title = task.title,
                            description = task.description ?: "",
                            location = location,
                            startDateTimeMs = task.startTime,
                            endDateTimeMs = task.endTime ?: task.startTime,
                            isRecurring = task.isRecurring,
                            participants = task.participants,
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

    private fun deleteEvent() {
        val eventId = _state.value.eventId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            planRepository.deleteTask(eventId)
            _state.update { it.copy(isLoading = false) }
            setEffect(EventDetailEffect.NavigateBack)
        }
    }

    private fun shareEvent() {
        viewModelScope.launch {
            val sender = sessionPreferences.getUserName() ?: ""
            val title = _state.value.title.encodeUrlParameter()
            val note = _state.value.description.encodeUrlParameter()
            val date = _state.value.startDateTimeMs
            val senderEncoded = sender.encodeUrlParameter()
            val eventId = _state.value.eventId
            val roomId = _state.value.planRoomId

            val url = if (eventId != null && roomId != null) {
                "https://pulse.yusufteker.com/share/joinEvent?eventId=$eventId&roomId=$roomId&title=$title&note=$note&date=$date&sender=$senderEncoded"
            } else {
                "https://pulse.yusufteker.com/share/event?title=$title&note=$note&date=$date&sender=$senderEncoded"
            }

            val shareText = if (eventId != null && roomId != null) {
                """
                    $sender seni ortak odadaki bir etkinliğe davet etti:
                    
                    Oda: ${_state.value.planRoomName ?: "Ortak Oda"}
                    Etkinlik: ${_state.value.title}
                    Tarih: ${Instant.fromEpochMilliseconds(_state.value.startDateTimeMs).toLocalDateTime(TimeZone.currentSystemDefault()).date}
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
            setEffect(EventDetailEffect.ShareItem(shareText))
        }
    }

    private fun setEffect(effect: EventDetailEffect) {
        viewModelScope.launch {
            _effect.emit(effect)
        }
    }
}
