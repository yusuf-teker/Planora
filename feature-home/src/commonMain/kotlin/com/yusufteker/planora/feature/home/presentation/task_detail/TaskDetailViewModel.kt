package com.yusufteker.planora.feature.home.presentation.task_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufteker.planora.feature.home.domain.repository.PlanRepository
import com.yusufteker.planora.feature.home.domain.repository.ProfileRepository
import com.yusufteker.planora.shared.api.extractBaseTaskId
import com.yusufteker.planora.shared.api.ItemDetails
import com.yusufteker.planora.shared.api.TaskPriority
import com.yusufteker.planora.shared.api.TaskStatus
import com.yusufteker.planora.shared.api.TaskType
import com.yusufteker.planora.feature.home.presentation.utils.encodeUrlParameter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import com.yusufteker.planora.shared.api.CreateTaskRequest
import com.yusufteker.planora.shared.api.TaskVisibility
import com.yusufteker.planora.core.utils.getCurrentTimeMs
import org.jetbrains.compose.resources.getString
import planora.core.generated.resources.Res
import planora.core.generated.resources.*

/**
 * ViewModel for TaskDetailScreen (Read-only view of a Task with completion toggling and actions).
 */
class TaskDetailViewModel(
    private val planRepository: PlanRepository,
    private val profileRepository: ProfileRepository,
    private val sessionPreferences: com.yusufteker.planora.core.preferences.SessionPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(TaskDetailState())
    val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<TaskDetailEffect>()
    val effect = _effect.asSharedFlow()

    fun onEvent(event: TaskDetailEvent) {
        when (event) {
            is TaskDetailEvent.OnLoadTask -> loadTask(
                taskId = event.taskId,
                planRoomId = event.planRoomId,
                sharedTitle = event.sharedTitle,
                sharedNote = event.sharedNote,
                sharedDate = event.sharedDate,
                sharedSender = event.sharedSender
            )
            TaskDetailEvent.OnToggleStatus -> toggleTaskStatus()
            is TaskDetailEvent.OnToggleSubtask -> toggleSubtask(event.subtaskId)
            TaskDetailEvent.OnEditClick -> {
                val taskId = _state.value.taskId
                if (taskId != null) {
                    setEffect(TaskDetailEffect.NavigateToEditTask(taskId, _state.value.planRoomId))
                }
            }
            TaskDetailEvent.OnFocusClick -> {
                val taskId = _state.value.taskId
                if (taskId != null) {
                    setEffect(TaskDetailEffect.NavigateToFocus(taskId))
                }
            }
            TaskDetailEvent.OnDeleteClick -> deleteTask()
            TaskDetailEvent.OnBackClick -> setEffect(TaskDetailEffect.NavigateBack)
            TaskDetailEvent.OnShareClick -> shareTask()
            TaskDetailEvent.OnCopyClick -> {
                val taskId = _state.value.taskId
                if (taskId != null) {
                    setEffect(TaskDetailEffect.NavigateToCopyTask(taskId, _state.value.planRoomId))
                }
            }
            is TaskDetailEvent.OnQuickDuplicate -> quickDuplicateTask(event.targetDateMs)
        }
    }

    private fun quickDuplicateTask(targetDateMs: Long) {
        viewModelScope.launch {
            val currentTask = _state.value
            val request = CreateTaskRequest(
                title = currentTask.title,
                description = currentTask.description.ifBlank { null },
                startTime = targetDateMs,
                endTime = null,
                type = TaskType.TASK,
                status = TaskStatus.PENDING,
                visibility = if (currentTask.planRoomId != null) TaskVisibility.ROOM_SHARED else TaskVisibility.PRIVATE,
                sharedRoomIds = currentTask.planRoomId?.let { listOf(it) } ?: emptyList(),
                isRecurring = false,
                recurrenceRule = null,
                isFlexible = true,
                isOptional = currentTask.task?.isOptional ?: false,
                isPostponable = true,
                isAllDay = false,
                reminders = currentTask.reminders,
                participants = currentTask.participants.associate { it.userId to it.name },
                specificDetails = ItemDetails.Task(
                    priority = currentTask.priority,
                    deadline = targetDateMs
                )
            )
            val result = planRepository.createTask(request, triggerSync = true)
            if (result.isSuccess) {
                val msg = getString(Res.string.msg_duplicated_successfully)
                setEffect(TaskDetailEffect.ShowSnackbar(msg))
            } else {
                setEffect(TaskDetailEffect.ShowSnackbar("Hata oluştu"))
            }
        }
    }

    private fun loadTask(
        taskId: String?,
        planRoomId: String?,
        sharedTitle: String?,
        sharedNote: String?,
        sharedDate: Long?,
        sharedSender: String?
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, taskId = taskId, planRoomId = planRoomId, sharedSender = sharedSender) }

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
                val baseTaskId = taskId?.extractBaseTaskId()
                val currentTask = tasks.find { it.id == taskId || it.id == baseTaskId }
                val subtaskList = tasks.filter { (it.parentId == taskId || (baseTaskId != null && it.parentId == baseTaskId)) && it.type == TaskType.TASK }
                val noteList = tasks.filter { (it.parentId == taskId || (baseTaskId != null && it.parentId == baseTaskId)) && it.type == TaskType.NOTE }

                if (currentTask != null) {
                    val actualPlanRoomId = _state.value.planRoomId ?: currentTask.sharedRoomIds.firstOrNull()
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

                    val deadline = (currentTask.specificDetails as? ItemDetails.Task)?.deadline
                    val priority = (currentTask.specificDetails as? ItemDetails.Task)?.priority ?: TaskPriority.MEDIUM
                    val ruleObj = try {
                        currentTask.recurrenceRule?.let { Json.decodeFromString<com.yusufteker.planora.shared.api.RecurrenceRule>(it) }
                    } catch (e: Exception) {
                        null
                    }

                    _state.update {
                        it.copy(
                            task = currentTask,
                            planRoomId = actualPlanRoomId,
                            title = currentTask.title,
                            description = currentTask.description ?: "",
                            status = currentTask.status,
                            priority = priority,
                            dueDateMs = deadline,
                            startTimeMs = currentTask.startTime,
                            endTimeMs = currentTask.endTime,
                            isAllDay = currentTask.isAllDay,
                            isRecurring = currentTask.isRecurring,
                            recurrenceRule = ruleObj,
                            reminders = currentTask.reminders,
                            participants = currentTask.participants,
                            subtasks = subtaskList,
                            notes = noteList,
                            isLoading = false
                        )
                    }
                } else {
                    _state.update { it.copy(isLoading = false, error = "Görev bulunamadı") }
                }
            }
        }
    }

    private fun toggleTaskStatus() {
        val currentTask = _state.value.task ?: return
        val dateMs = (currentTask.specificDetails as? ItemDetails.Task)?.deadline ?: currentTask.startTime
        val isCurrentlyCompleted = currentTask.status == TaskStatus.COMPLETED
        val baseId = currentTask.id.extractBaseTaskId()

        viewModelScope.launch {
            val newStatus = if (isCurrentlyCompleted) TaskStatus.PENDING else TaskStatus.COMPLETED
            _state.update { it.copy(status = newStatus) }

            val result = planRepository.completeTaskInstance(
                taskId = baseId,
                dateMs = dateMs,
                isCompleted = !isCurrentlyCompleted
            )

            if (result.isFailure) {
                _state.update { it.copy(status = currentTask.status) }
                setEffect(TaskDetailEffect.ShowSnackbar("Görev durumu güncellenemedi"))
            } else {
                val msg = if (!isCurrentlyCompleted) "Görev tamamlandı olarak işaretlendi" else "Görev yapılacak olarak işaretlendi"
                setEffect(TaskDetailEffect.ShowSnackbar(msg))
            }
        }
    }

    private fun toggleSubtask(subtaskId: String) {
        val subtask = _state.value.subtasks.find { it.id == subtaskId } ?: return
        val isCompleted = subtask.status == TaskStatus.COMPLETED

        viewModelScope.launch {
            val newStatus = if (isCompleted) TaskStatus.PENDING else TaskStatus.COMPLETED
            val updatedSubtasks = _state.value.subtasks.map {
                if (it.id == subtaskId) it.copy(status = newStatus) else it
            }
            _state.update { it.copy(subtasks = updatedSubtasks) }

            val result = planRepository.completeTaskInstance(
                taskId = subtaskId.extractBaseTaskId(),
                dateMs = subtask.startTime,
                isCompleted = !isCompleted
            )
            if (result.isFailure) {
                _state.update { state ->
                    state.copy(subtasks = state.subtasks.map {
                        if (it.id == subtaskId) subtask else it
                    })
                }
            }
        }
    }

    private fun deleteTask() {
        val id = _state.value.taskId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            planRepository.deleteTask(id)
            _state.update { it.copy(isLoading = false) }
            setEffect(TaskDetailEffect.NavigateBack)
        }
    }

    private fun shareTask() {
        viewModelScope.launch {
            val sender = sessionPreferences.getUserName() ?: ""
            val title = _state.value.title.encodeUrlParameter()
            val note = _state.value.description.encodeUrlParameter()
            val date = _state.value.dueDateMs ?: _state.value.startTimeMs ?: 0L
            val senderEncoded = sender.encodeUrlParameter()
            val taskId = _state.value.taskId
            val roomId = _state.value.planRoomId

            val url = if (taskId != null && roomId != null) {
                "https://planora.yusufteker.com/share/joinTask?taskId=$taskId&roomId=$roomId&title=$title&note=$note&date=$date&sender=$senderEncoded"
            } else {
                "https://planora.yusufteker.com/share/task?title=$title&note=$note&date=$date&sender=$senderEncoded"
            }

            val shareText = """
                $sender seni bir göreve davet etti:
                
                ${_state.value.title}
                ${_state.value.description}
                
                Planora'de aç: $url
            """.trimIndent()

            setEffect(TaskDetailEffect.ShareItem(shareText))
        }
    }

    private fun setEffect(effect: TaskDetailEffect) {
        viewModelScope.launch {
            _effect.emit(effect)
        }
    }
}
