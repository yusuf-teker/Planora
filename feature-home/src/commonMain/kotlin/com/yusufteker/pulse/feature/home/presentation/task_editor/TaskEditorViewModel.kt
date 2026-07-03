package com.yusufteker.pulse.feature.home.presentation.task_editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufteker.pulse.core.base.BaseViewModel
import com.yusufteker.pulse.core.utils.getCurrentTimeMs
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
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TaskEditorViewModel(
    private val planRepository: PlanRepository,
    private val profileRepository: ProfileRepository,
    private val sessionPreferences: com.yusufteker.pulse.core.preferences.SessionPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(TaskEditorState())
    val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<TaskEditorEffect>()
    val effect = _effect.asSharedFlow()

    fun onEvent(event: TaskEditorEvent) {
        when (event) {
            is TaskEditorEvent.OnLoadTask -> loadTask(event.taskId, event.planRoomId)
            is TaskEditorEvent.TitleChanged -> _state.update { it.copy(title = event.title) }
            is TaskEditorEvent.DescriptionChanged -> _state.update { it.copy(description = event.description) }
            
            is TaskEditorEvent.OnDeadlinePickerVisibilityChanged -> _state.update { it.copy(isDeadlinePickerVisible = event.isVisible) }
            is TaskEditorEvent.OnDeadlineSelected -> _state.update { it.copy(deadlineDateMs = event.dateMs, isDeadlinePickerVisible = false) }
            
            is TaskEditorEvent.OnIsRecurringChanged -> _state.update { it.copy(isRecurring = event.isRecurring) }
            is TaskEditorEvent.OnRepeatPickerVisibilityChanged -> _state.update { it.copy(isRepeatPickerVisible = event.isVisible) }
            is TaskEditorEvent.OnRecurrenceRuleChanged -> _state.update {
                it.copy(recurrenceRule = event.rule, isRecurring = event.rule != null)
            }
            
            is TaskEditorEvent.OnIsOptionalChanged -> _state.update { it.copy(isOptional = event.isOptional) }
            is TaskEditorEvent.OnReminderPickerVisibilityChanged -> _state.update { it.copy(isReminderPickerVisible = event.isVisible) }
            is TaskEditorEvent.OnReminderToggled -> _state.update {
                val newReminders = if (it.reminders.contains(event.minutes)) {
                    it.reminders - event.minutes
                } else {
                    it.reminders + event.minutes
                }
                it.copy(reminders = newReminders)
            }
            
            is TaskEditorEvent.OnParticipantPickerVisibilityChanged -> _state.update { it.copy(isParticipantPickerVisible = event.isVisible) }
            is TaskEditorEvent.OnParticipantToggled -> _state.update {
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
            TaskEditorEvent.SaveClicked -> saveTask()
            TaskEditorEvent.DeleteClicked -> deleteTask()
            TaskEditorEvent.OnBackClick -> setEffect(TaskEditorEffect.NavigateBack)
        }
    }

    private fun loadTask(taskId: String?, planRoomId: String?) {

        if (taskId == null) {
            _state.value = TaskEditorState(planRoomId = planRoomId)
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
            _state.update { it.copy(isLoading = true, id = taskId, planRoomId = planRoomId) }

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
                val task = tasks.find { it.id == taskId && it.type == TaskType.TASK }
                if (task != null) {
                    val ruleObj = try {
                        task.recurrenceRule?.let { Json.decodeFromString<com.yusufteker.pulse.shared.api.RecurrenceRule>(it) }
                    } catch (e: Exception) {
                        null
                    }

                    val details = task.specificDetails as? com.yusufteker.pulse.shared.api.ItemDetails.Task
                    val deadline = details?.deadline ?: task.endTime

                    _state.update { 
                        it.copy(
                            title = task.title,
                            description = task.description ?: "",
                            deadlineDateMs = deadline,
                            isRecurring = task.isRecurring,
                            recurrenceRule = ruleObj,
                            isOptional = task.isOptional,
                            reminders = task.reminders,
                            participants = task.participants,
                            isLoading = false
                        ) 
                    }
                } else {
                    _state.update { it.copy(isLoading = false, error = "Görev bulunamadı") }
                }
            }
        }
    }

    private fun saveTask() {
        val currentState = _state.value
        if (currentState.title.isBlank()) {
            setEffect(TaskEditorEffect.ShowSnackbar("Lütfen bir başlık girin."))
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val now = getCurrentTimeMs()
            
            val recurrenceStr = currentState.recurrenceRule?.let { Json.encodeToString(it) }

            val request = com.yusufteker.pulse.shared.api.CreateTaskRequest(
                title = currentState.title,
                description = currentState.description.ifBlank { null },
                startTime = now,
                endTime = null,
                type = TaskType.TASK,
                status = TaskStatus.PENDING,
                visibility = if (currentState.planRoomId != null) TaskVisibility.ROOM_SHARED else TaskVisibility.PRIVATE,
                sharedRoomIds = currentState.planRoomId?.let { listOf(it) } ?: emptyList(),
                isRecurring = currentState.isRecurring || currentState.recurrenceRule != null,
                recurrenceRule = recurrenceStr,
                isFlexible = true,
                isOptional = currentState.isOptional,
                isPostponable = true,
                isAllDay = false,
                reminders = currentState.reminders,
                participants = currentState.participants,
                specificDetails = com.yusufteker.pulse.shared.api.ItemDetails.Task(
                    subtasks = emptyList(), 
                    priority = com.yusufteker.pulse.shared.api.TaskPriority.MEDIUM,
                    deadline = currentState.deadlineDateMs
                ),
                tags = emptyList(),
                color = null
            )

            if (currentState.id != null) {
                planRepository.updateTask(currentState.id, request)
            } else {
                planRepository.createTask(request)
            }
            
            _state.update { it.copy(isLoading = false) }
            setEffect(TaskEditorEffect.NavigateBack)
        }
    }

    private fun deleteTask() {
        val taskId = _state.value.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            planRepository.deleteTask(taskId)
            _state.update { it.copy(isLoading = false) }
            setEffect(TaskEditorEffect.NavigateBack)
        }
    }

    private fun setEffect(effect: TaskEditorEffect) {
        viewModelScope.launch {
            _effect.emit(effect)
        }
    }
}
