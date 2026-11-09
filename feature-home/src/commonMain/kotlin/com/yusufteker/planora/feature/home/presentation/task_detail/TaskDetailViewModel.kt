package com.yusufteker.planora.feature.home.presentation.task_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufteker.planora.feature.home.domain.repository.PlanRepository
import com.yusufteker.planora.feature.home.domain.repository.ProfileRepository
import com.yusufteker.planora.shared.api.extractBaseTaskId
import com.yusufteker.planora.shared.api.ItemDetails
import com.yusufteker.planora.shared.api.RoomMemberStatus
import com.yusufteker.planora.shared.api.TaskPriority
import com.yusufteker.planora.shared.api.TaskStatus
import com.yusufteker.planora.shared.api.TaskType
import com.yusufteker.planora.feature.home.presentation.utils.encodeUrlParameter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import com.yusufteker.planora.shared.api.CreateTaskRequest
import com.yusufteker.planora.shared.api.TaskVisibility
import com.yusufteker.planora.core.utils.getCurrentTimeMs
import org.jetbrains.compose.resources.getString
import planora.core.generated.resources.Res
import planora.core.generated.resources.*

/**
 * ViewModel for TaskDetailScreen (Read-only view of a Task with completion toggling and actions).
 * Manages task details, quick child notes, duplication, and moving the task between personal
 * state and group plan rooms with member assignment.
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

    init {
        // Mevcut grup odalarını sürekli dinle
        viewModelScope.launch {
            planRepository.observeAllPlanRooms().collect { rooms ->
                _state.update { currentState ->
                    val currentRoomId = currentState.planRoomId ?: currentState.task?.sharedRoomIds?.firstOrNull()
                    val matchingRoom = rooms.find { it.id == currentRoomId }
                    currentState.copy(
                        availableRooms = rooms,
                        planRoomName = matchingRoom?.name ?: currentState.planRoomName,
                        planRoomImageUrl = matchingRoom?.imageUrl ?: currentState.planRoomImageUrl
                    )
                }
            }
        }
    }

    /**
     * Handles incoming UI events for task detail actions.
     */
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
            is TaskDetailEvent.OnAddQuickNote -> addQuickNote(event.content)
            is TaskDetailEvent.OnDeleteNote -> deleteNote(event.noteId)

            // Grup Odasına Taşıma
            TaskDetailEvent.OnOpenMoveToRoomSheet -> openMoveToRoomSheet()
            TaskDetailEvent.OnDismissMoveToRoomSheet -> _state.update { it.copy(isMoveToRoomSheetVisible = false) }
            is TaskDetailEvent.OnSelectTargetRoom -> selectTargetRoom(event.roomId)
            is TaskDetailEvent.OnToggleTargetParticipant -> toggleTargetParticipant(event.userId)
            is TaskDetailEvent.OnConfirmMoveToRoom -> confirmMoveToRoom(event.targetRoomId, event.participantIds)
        }
    }

    /**
     * Taşıma alt sayfasını açar ve geçerli odanın üyelerini yükler.
     */
    private fun openMoveToRoomSheet() {
        val currentRoomId = _state.value.planRoomId ?: _state.value.task?.sharedRoomIds?.firstOrNull()
        val currentParticipantIds = _state.value.participants.map { it.userId }.toSet()

        _state.update {
            it.copy(
                isMoveToRoomSheetVisible = true,
                selectedParticipantIds = currentParticipantIds
            )
        }

        if (currentRoomId != null) {
            loadTargetRoomMembers(currentRoomId)
        } else {
            _state.update { it.copy(targetRoomMembers = emptyList()) }
        }
    }

    /**
     * Taşıma alt sayfasında seçilen hedef odayı günceller ve üyelerini çeker.
     *
     * @param roomId Hedef oda ID'si veya kişisel yapılacaksa null.
     */
    private fun selectTargetRoom(roomId: String?) {
        if (roomId == null) {
            _state.update {
                it.copy(
                    targetRoomMembers = emptyList(),
                    selectedParticipantIds = emptySet()
                )
            }
        } else {
            loadTargetRoomMembers(roomId)
        }
    }

    /**
     * Hedef odanın üyelerini yükler ve mevcut kullanıcıyı varsayılan olarak seçili katılımcılara ekler.
     *
     * @param roomId Hedef grup odasının ID'si.
     */
    private fun loadTargetRoomMembers(roomId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isTargetRoomMembersLoading = true) }
            val rooms = _state.value.availableRooms.ifEmpty {
                planRepository.observeAllPlanRooms().firstOrNull() ?: emptyList()
            }
            val room = rooms.find { it.id == roomId }
            if (room != null) {
                val validMembers = room.members.filter { it.status != RoomMemberStatus.DECLINED }
                val profiles = coroutineScope {
                    validMembers.map { member ->
                        async {
                            profileRepository.getProfile(member.userId.toString()).getOrNull()
                        }
                    }.awaitAll().filterNotNull()
                }

                val currentUserId = sessionPreferences.getUserId()?.toIntOrNull()
                val initialParticipants = _state.value.selectedParticipantIds.toMutableSet()
                if (currentUserId != null && !initialParticipants.contains(currentUserId)) {
                    initialParticipants.add(currentUserId)
                }

                _state.update {
                    it.copy(
                        targetRoomMembers = profiles,
                        isTargetRoomMembersLoading = false,
                        selectedParticipantIds = initialParticipants
                    )
                }
            } else {
                _state.update { it.copy(isTargetRoomMembersLoading = false) }
            }
        }
    }

    /**
     * Taşıma alt sayfasında bir katılımcının seçim durumunu tersine çevirir.
     *
     * @param userId Katılımcının kullanıcı ID'si.
     */
    private fun toggleTargetParticipant(userId: Int) {
        val current = _state.value.selectedParticipantIds
        val updated = if (current.contains(userId)) current - userId else current + userId
        _state.update { it.copy(selectedParticipantIds = updated) }
    }

    /**
     * Görevi hedef odaya taşır veya kişisel göreve dönüştürür.
     * Yerel veritabanında günceller ve arka planda sunucu senkronizasyonunu başlatır.
     *
     * @param targetRoomId Hedef grup odasının ID'si (kişisel ise null).
     * @param participantIds Atanan kullanıcıların ID kümesi.
     */
    private fun confirmMoveToRoom(targetRoomId: String?, participantIds: Set<Int>) {
        viewModelScope.launch {
            val currentTask = _state.value.task ?: return@launch
            val baseId = currentTask.id.extractBaseTaskId()
            _state.update { it.copy(isLoading = true, isMoveToRoomSheetVisible = false) }

            val participantsMap = if (targetRoomId != null) {
                val membersMap = _state.value.targetRoomMembers.associate { it.id to it.name }
                val currentUserId = sessionPreferences.getUserId()?.toIntOrNull()
                val currentUserName = sessionPreferences.getUserName()
                participantIds.associateWith { id ->
                    membersMap[id] ?: if (id == currentUserId && currentUserName != null) currentUserName else "Üye $id"
                }
            } else emptyMap()

            val request = CreateTaskRequest(
                title = currentTask.title,
                description = currentTask.description,
                startTime = currentTask.startTime,
                endTime = currentTask.endTime,
                type = currentTask.type,
                status = currentTask.status,
                visibility = if (targetRoomId != null) TaskVisibility.ROOM_SHARED else TaskVisibility.PRIVATE,
                sharedRoomIds = targetRoomId?.let { listOf(it) } ?: emptyList(),
                isRecurring = currentTask.isRecurring,
                recurrenceRule = currentTask.recurrenceRule,
                isFlexible = currentTask.isFlexible,
                isOptional = currentTask.isOptional,
                isPostponable = currentTask.isPostponable,
                isAllDay = currentTask.isAllDay,
                parentId = currentTask.parentId,
                aiMetadata = currentTask.aiMetadata,
                reminders = currentTask.reminders,
                specificDetails = currentTask.specificDetails,
                tags = currentTask.tags,
                color = currentTask.color,
                participants = participantsMap,
                isPinned = currentTask.isPinned
            )

            val result = planRepository.updateTask(baseId, request, triggerSync = true)
            _state.update { it.copy(isLoading = false) }

            if (result.isSuccess) {
                val msg = if (targetRoomId != null) {
                    val roomName = _state.value.availableRooms.find { it.id == targetRoomId }?.name ?: ""
                    runCatching { getString(Res.string.msg_moved_to_room, roomName) }.getOrDefault("Görev '$roomName' odasına taşındı")
                } else {
                    runCatching { getString(Res.string.msg_converted_to_personal) }.getOrDefault("Görev kişisel göreve dönüştürüldü")
                }
                setEffect(TaskDetailEffect.ShowSnackbar(msg))
            } else {
                setEffect(TaskDetailEffect.ShowSnackbar("Hata oluştu"))
            }
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
                val msg = runCatching { getString(Res.string.msg_duplicated_successfully) }.getOrDefault("Görev çoğaltıldı")
                setEffect(TaskDetailEffect.ShowSnackbar(msg))
            } else {
                setEffect(TaskDetailEffect.ShowSnackbar("Hata oluştu"))
            }
        }
    }

    /**
     * Creates and attaches a quick note to the current task.
     *
     * @param content The note content entered by the user.
     */
    private fun addQuickNote(content: String) {
        val trimmed = content.trim()
        if (trimmed.isBlank()) return
        val taskId = _state.value.taskId ?: _state.value.task?.id ?: return
        val now = getCurrentTimeMs()

        val request = CreateTaskRequest(
            title = trimmed,
            description = trimmed,
            startTime = now,
            endTime = now,
            type = TaskType.NOTE,
            status = TaskStatus.PENDING,
            visibility = if (_state.value.planRoomId != null) TaskVisibility.ROOM_SHARED else TaskVisibility.PRIVATE,
            sharedRoomIds = _state.value.planRoomId?.let { listOf(it) } ?: emptyList(),
            isRecurring = false,
            recurrenceRule = null,
            isFlexible = true,
            isOptional = true,
            isPostponable = false,
            isAllDay = false,
            parentId = taskId,
            reminders = emptyList(),
            specificDetails = ItemDetails.Note(
                content = trimmed,
                attachments = emptyList(),
                checklist = emptyList()
            )
        )

        viewModelScope.launch {
            val result = planRepository.createTask(request, triggerSync = true)
            if (result.isSuccess) {
                val msg = runCatching { getString(Res.string.quick_note_added) }.getOrDefault("Not eklendi")
                setEffect(TaskDetailEffect.ShowSnackbar(msg))
            } else {
                val errMsg = runCatching { getString(Res.string.quick_note_error) }.getOrDefault("Not eklenirken hata oluştu")
                setEffect(TaskDetailEffect.ShowSnackbar(errMsg))
            }
        }
    }

    /**
     * Deletes a child note attached to this task.
     *
     * @param noteId The ID of the note to delete.
     */
    private fun deleteNote(noteId: String) {
        viewModelScope.launch {
            val result = planRepository.deleteTask(noteId)
            if (result.isSuccess) {
                val msg = runCatching { getString(Res.string.note_deleted) }.getOrDefault("Not silindi")
                setEffect(TaskDetailEffect.ShowSnackbar(msg))
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
