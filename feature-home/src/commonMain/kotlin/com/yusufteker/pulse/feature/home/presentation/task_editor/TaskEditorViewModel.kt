package com.yusufteker.pulse.feature.home.presentation.task_editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufteker.pulse.core.utils.getCurrentTimeMs
import com.yusufteker.pulse.feature.home.domain.repository.PlanRepository
import com.yusufteker.pulse.feature.home.domain.repository.ProfileRepository
import com.yusufteker.pulse.shared.api.TaskStatus
import com.yusufteker.pulse.shared.api.TaskType
import com.yusufteker.pulse.shared.api.TaskVisibility
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.getString
import pulsy.core.generated.resources.Res
import pulsy.core.generated.resources.*
import com.yusufteker.pulse.shared.api.extractBaseTaskId
import io.github.aakira.napier.Napier
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job

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
            is TaskEditorEvent.OnLoadTask -> loadTask(event.taskId, event.planRoomId, event.parentId)
            is TaskEditorEvent.TitleChanged -> { _state.update { it.copy(title = event.title) } }
            is TaskEditorEvent.DescriptionChanged -> { _state.update { it.copy(description = event.description) } }
            
            is TaskEditorEvent.OnDeadlinePickerVisibilityChanged -> _state.update { it.copy(isDeadlinePickerVisible = event.isVisible) }
            is TaskEditorEvent.OnDeadlineSelected -> { _state.update { it.copy(deadlineDateMs = event.dateMs, isDeadlinePickerVisible = false) } }
            
            is TaskEditorEvent.OnIsRecurringChanged -> { _state.update { it.copy(isRecurring = event.isRecurring) } }
            is TaskEditorEvent.OnRepeatPickerVisibilityChanged -> _state.update { it.copy(isRepeatPickerVisible = event.isVisible) }
            is TaskEditorEvent.OnRecurrenceRuleChanged -> {
                _state.update {
                    it.copy(recurrenceRule = event.rule, isRecurring = event.rule != null)
                }
            }
            
            is TaskEditorEvent.OnIsOptionalChanged -> { _state.update { it.copy(isOptional = event.isOptional) } }
            is TaskEditorEvent.OnReminderPickerVisibilityChanged -> _state.update { it.copy(isReminderPickerVisible = event.isVisible) }
            is TaskEditorEvent.OnReminderToggled -> {
                _state.update {
                    val newReminders = if (it.reminders.contains(event.minutes)) {
                        it.reminders - event.minutes
                    } else {
                        it.reminders + event.minutes
                    }
                    it.copy(reminders = newReminders)
                }
            }
            is TaskEditorEvent.StatusChanged -> {
                _state.update { 
                    it.copy(status = if (event.isCompleted) TaskStatus.COMPLETED else TaskStatus.PENDING) 
                }
            }
            
            is TaskEditorEvent.OnParticipantPickerVisibilityChanged -> _state.update { it.copy(isParticipantPickerVisible = event.isVisible) }
            is TaskEditorEvent.OnParticipantToggled -> {
                val currentMap = _state.value.participants.toMutableMap()
                if (currentMap.containsKey(event.userId)) {
                    if (currentMap.size > 1) {
                        currentMap.remove(event.userId)
                        _state.update { it.copy(participants = currentMap) }
                    } else {
                        setEffect(TaskEditorEffect.ShowSnackbar("En az 1 katılımcı olmalıdır."))
                    }
                } else {
                    val user = _state.value.roomMembers.find { u -> u.id == event.userId }
                    if (user != null) {
                        currentMap[event.userId] = user.name
                        _state.update { it.copy(participants = currentMap) }
                    }
                }
            }
            is TaskEditorEvent.SaveClicked -> saveTask()
            is TaskEditorEvent.DeleteClicked -> deleteTask()
            is TaskEditorEvent.OnBackClick -> setEffect(TaskEditorEffect.NavigateBack)
            is TaskEditorEvent.OnDispose -> {}
        }
    }

    private fun loadTask(taskId: String?, planRoomId: String?, parentId: String?) {
        Napier.d { "TaskEditorViewModel.loadTask: taskId=$taskId, planRoomId=$planRoomId, parentId=$parentId" }

        if (taskId == null) {
            viewModelScope.launch {
                val currentUserId = sessionPreferences.getUserId()?.toIntOrNull()
                val currentUserName = sessionPreferences.getUserName()
                val defaultParticipants = if (currentUserId != null && currentUserName != null && planRoomId != null) {
                    mapOf(currentUserId to currentUserName)
                } else emptyMap()

                _state.value = TaskEditorState(
                    planRoomId = planRoomId, 
                    parentId = parentId, 
                    participants = defaultParticipants,
                    deadlineDateMs = com.yusufteker.pulse.core.utils.getCurrentTimeMs()
                )
                if (planRoomId != null) {
                    planRepository.observeAllPlanRooms().collect { rooms ->
                        val room = rooms.find { it.id == planRoomId }
                        if (room != null) {
                            val profiles = kotlinx.coroutines.coroutineScope {
                                room.members.map { member ->
                                    async {
                                        profileRepository.getProfile(member.userId.toString()).getOrNull()
                                    }
                                }.awaitAll().filterNotNull()
                            }
                            _state.update { it.copy(roomMembers = profiles) }
                        }
                    }
                }
            }
            return
        }

        viewModelScope.launch {
            val baseId = taskId.extractBaseTaskId()
            Napier.d { "TaskEditorViewModel.loadTask -> baseId=$baseId" }
            _state.update { it.copy(isLoading = true, id = baseId, planRoomId = planRoomId, parentId = parentId) }

        // Fetch room members if planRoomId is present
        if (planRoomId != null) {
            viewModelScope.launch {
                planRepository.observeAllPlanRooms().collect { rooms ->
                    val room = rooms.find { it.id == planRoomId }
                    if (room != null) {
                        val profiles = kotlinx.coroutines.coroutineScope {
                            room.members.map { member ->
                                async {
                                    profileRepository.getProfile(member.userId.toString()).getOrNull()
                                }
                            }.awaitAll().filterNotNull()
                        }
                        _state.update { it.copy(roomMembers = profiles) }
                    }
                }
            }
        }

            planRepository.observeAllTasks().collect { tasks ->
                val task = tasks.find { it.id == baseId && it.type == TaskType.TASK }
                Napier.d { "TaskEditorViewModel observeAllTasks COLLECT: taskFound=${task != null}, totalTasks=${tasks.size}" }
                
                // Fetch sub-items (Tasks and Notes) that belong to this task
                val subItemsList = tasks.filter { it.parentId == baseId }
                
                if (task != null) {
                    val ruleObj = try {
                        task.recurrenceRule?.let { Json.decodeFromString<com.yusufteker.pulse.shared.api.RecurrenceRule>(it) }
                    } catch (e: Exception) {
                        null
                    }

                    val details = task.specificDetails as? com.yusufteker.pulse.shared.api.ItemDetails.Task
                    val deadline = details?.deadline ?: task.endTime

                    _state.update { currentState ->
                        val newTitle = if (currentState.title.isBlank() || currentState.title == currentState.originalTask?.title) task.title else currentState.title
                        val newDescription = if (currentState.description.isBlank() || currentState.description == (currentState.originalTask?.description ?: "")) task.description ?: "" else currentState.description
                        val newDeadline = if (currentState.deadlineDateMs == (currentState.originalTask?.specificDetails as? com.yusufteker.pulse.shared.api.ItemDetails.Task)?.deadline) deadline else currentState.deadlineDateMs

                        Napier.d { "TaskEditorViewModel updating state with DB emission: title=$newTitle, isDataLoaded=true" }
                        currentState.copy(
                            originalTask = task,
                            title = newTitle,
                            description = newDescription,
                            originalStartTime = task.startTime,
                            deadlineDateMs = newDeadline,
                            status = task.status,
                            isRecurring = task.isRecurring,
                            recurrenceRule = ruleObj,
                            isOptional = task.isOptional,
                            reminders = task.reminders,
                            participants = task.participants.associate { it.userId to it.name },
                            subItems = subItemsList,
                            isLoading = false,
                            parentId = task.parentId
                        )
                    }
                } else {
                    val errorMsg = getString(Res.string.error_task_not_found)
                    Napier.w { "TaskEditorViewModel loadTask: task $baseId not found in DB list!" }
                    _state.update { it.copy(isLoading = false, error = errorMsg) }
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun saveTask() {
        val currentState = _state.value
        Napier.d { "TaskEditorViewModel.saveTask: id=${currentState.id}, title=${currentState.title}, isDeleted=${currentState.isDeleted}" }
        if (currentState.isDeleted) return   // silinmiş görevi asla diriltme
        if (currentState.title.isBlank()) {
            setEffect(TaskEditorEffect.ShowSnackbar("Lütfen bir başlık girin."))
            return
        }

        val request = buildCreateTaskRequest(currentState)

        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true) }
            val result = if (currentState.id != null) {
                Napier.d { "TaskEditorViewModel.saveTask: calling updateTask for ${currentState.id}" }
                planRepository.updateTask(currentState.id, request)
            } else {
                Napier.d { "TaskEditorViewModel.saveTask: calling createTask" }
                planRepository.createTask(request)
            }
            _state.update { it.copy(isLoading = false) }
            
            if (result.isSuccess) {
                Napier.d { "TaskEditorViewModel.saveTask SUCCESS, navigating back" }
                setEffect(TaskEditorEffect.NavigateBack)
            } else {
                val error = result.exceptionOrNull()
                Napier.e(error) { "TaskEditorViewModel.saveTask FAILED: ${error?.message}" }
                setEffect(TaskEditorEffect.ShowSnackbar("Görev güncellenemedi, lütfen tekrar deneyin."))
            }
        }
    }

    /**
     * State'den CreateTaskRequest oluşturur. autoSave, forceSave ve saveTask tarafından ortak kullanılır.
     */
    private fun buildCreateTaskRequest(state: TaskEditorState): com.yusufteker.pulse.shared.api.CreateTaskRequest {
        val now = state.originalStartTime ?: getCurrentTimeMs()
        val recurrenceStr = state.recurrenceRule?.let { Json.encodeToString(it) }

        return com.yusufteker.pulse.shared.api.CreateTaskRequest(
            title = state.title,
            description = state.description.ifBlank { null },
            startTime = now,
            endTime = state.originalTask?.endTime,
            type = state.originalTask?.type ?: TaskType.TASK,
            status = state.status,
            visibility = if (state.planRoomId != null) TaskVisibility.ROOM_SHARED else (state.originalTask?.visibility ?: TaskVisibility.PRIVATE),
            sharedRoomIds = state.planRoomId?.let { listOf(it) } ?: emptyList(),
            isRecurring = state.isRecurring || state.recurrenceRule != null,
            recurrenceRule = recurrenceStr,
            isFlexible = state.originalTask?.isFlexible ?: true,
            isOptional = state.isOptional,
            isPostponable = state.originalTask?.isPostponable ?: true,
            isAllDay = state.originalTask?.isAllDay ?: false,
            reminders = state.reminders,
            participants = state.participants,
            specificDetails = com.yusufteker.pulse.shared.api.ItemDetails.Task(
                subtasks = state.originalTask?.specificDetails?.let { (it as? com.yusufteker.pulse.shared.api.ItemDetails.Task)?.subtasks } ?: emptyList(),
                priority = state.originalTask?.specificDetails?.let { (it as? com.yusufteker.pulse.shared.api.ItemDetails.Task)?.priority } ?: com.yusufteker.pulse.shared.api.TaskPriority.MEDIUM,
                deadline = state.deadlineDateMs
            ),
            parentId = state.parentId,
            tags = state.originalTask?.tags ?: emptyList(),
            color = state.originalTask?.color,
            isPinned = state.originalTask?.isPinned ?: false
        )
    }

    private fun deleteTask() {
        val taskId = _state.value.id ?: return
        Napier.d { "TaskEditorViewModel.deleteTask: taskId=$taskId" }
        _state.update { it.copy(isDeleted = true) }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = planRepository.deleteTask(taskId)
            _state.update { it.copy(isLoading = false) }
            if (result.isSuccess) {
                Napier.d { "TaskEditorViewModel.deleteTask SUCCESS, navigating back" }
                setEffect(TaskEditorEffect.NavigateBack)
            } else {
                val error = result.exceptionOrNull()
                Napier.e(error) { "TaskEditorViewModel.deleteTask FAILED: ${error?.message}" }
                setEffect(TaskEditorEffect.ShowSnackbar("Görev silinemedi, lütfen tekrar deneyin."))
            }
        }
    }

    private fun setEffect(effect: TaskEditorEffect) {
        viewModelScope.launch {
            _effect.emit(effect)
        }
    }
}
