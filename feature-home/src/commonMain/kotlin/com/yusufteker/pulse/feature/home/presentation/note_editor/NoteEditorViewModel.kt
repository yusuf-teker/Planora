package com.yusufteker.pulse.feature.home.presentation.note_editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufteker.pulse.feature.home.domain.repository.PlanRepository
import com.yusufteker.pulse.shared.api.ItemDetails
import com.yusufteker.pulse.shared.api.TaskStatus
import com.yusufteker.pulse.feature.home.presentation.utils.encodeUrlParameter
import com.yusufteker.pulse.shared.api.TaskType
import com.yusufteker.pulse.shared.api.TaskVisibility
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import io.github.aakira.napier.Napier

class NoteEditorViewModel(
    private val noteId: String?,
    private val planRoomId: String?,
    private val parentId: String?,
    private val planRepository: PlanRepository,
    private val sessionPreferences: com.yusufteker.pulse.core.preferences.SessionPreferences,
    private val cloudAiManager: com.yusufteker.pulse.core.ai.CloudAiManager
) : ViewModel() {

    private val _state = MutableStateFlow(NoteEditorState())
    val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<NoteEditorEffect>()
    val effect = _effect.asSharedFlow()

    private val _saveTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private var isDeleted = false

    init {
        @OptIn(kotlinx.coroutines.FlowPreview::class)
        _saveTrigger
            .debounce(1000L)
            .onEach { autoSaveNote() }
            .launchIn(viewModelScope)
        
        loadNote(noteId, planRoomId, parentId)
    }

    fun onEvent(event: NoteEditorEvent) {
        when (event) {
            is NoteEditorEvent.OnLoadNote -> loadNote(
                noteId = event.noteId, 
                planRoomId = event.planRoomId, 
                parentId = event.parentId,
                sharedNote = event.sharedNote,
                sharedSender = event.sharedSender
            )
            is NoteEditorEvent.OnTitleChange -> { 
                _state.update { it.copy(title = event.title) } 
                _saveTrigger.tryEmit(Unit)
            }
            is NoteEditorEvent.OnContentChange -> { 
                _state.update { it.copy(content = event.content) } 
                _saveTrigger.tryEmit(Unit)
            }
            NoteEditorEvent.OnSaveClick -> saveNote()
            NoteEditorEvent.OnDeleteClick -> deleteNote()
            NoteEditorEvent.OnBackClick -> setEffect(NoteEditorEffect.NavigateBack)
            NoteEditorEvent.OnDispose -> {}
            NoteEditorEvent.OnShareClick -> {
                viewModelScope.launch {
                    val sender = sessionPreferences.getUserName() ?: ""
                    val note = _state.value.content.encodeUrlParameter()
                    val senderEncoded = sender.encodeUrlParameter()
                    val url = "https://pulse.yusufteker.com/share/note?note=$note&sender=$senderEncoded"
                    val shareText = """
                        $sender sana bir not paylaştı:
                        
                        ${_state.value.content}
                        
                        Pulsy'de aç: $url
                    """.trimIndent()
                    setEffect(NoteEditorEffect.ShareItem(shareText))
                }
            }
            is NoteEditorEvent.OnAiActionClick -> processAiPrompt(event.prompt)
            NoteEditorEvent.OnAiCancelClick -> cancelAiProcessing()
            NoteEditorEvent.OnAiPreviewAccept -> acceptAiPreview()
            NoteEditorEvent.OnAiPreviewReject -> rejectAiPreview()
            is NoteEditorEvent.OnFolderSelected -> {
                _state.update { it.copy(parentId = event.folderId) }
                _saveTrigger.tryEmit(Unit)
            }
            is NoteEditorEvent.OnCreateFolderClick -> createFolder(event.folderName)
            is NoteEditorEvent.OnAddChecklistItem -> {
                val newItem = com.yusufteker.pulse.shared.api.SubTask(
                    id = com.yusufteker.pulse.core.utils.generateUUID(),
                    title = event.title,
                    isDone = false
                )
                _state.update { it.copy(checklist = it.checklist + newItem) }
                _saveTrigger.tryEmit(Unit)
            }
            is NoteEditorEvent.OnToggleChecklistItem -> {
                _state.update { state ->
                    state.copy(checklist = state.checklist.map { item ->
                        if (item.id == event.itemId) item.copy(isDone = !item.isDone) else item
                    })
                }
                _saveTrigger.tryEmit(Unit)
            }
            is NoteEditorEvent.OnDeleteChecklistItem -> {
                _state.update { state ->
                    state.copy(checklist = state.checklist.filter { item -> item.id != event.itemId })
                }
                _saveTrigger.tryEmit(Unit)
            }
            is NoteEditorEvent.OnUpdateChecklistItem -> {
                _state.update { state ->
                    state.copy(checklist = state.checklist.map { item ->
                        if (item.id == event.itemId) item.copy(title = event.newTitle) else item
                    })
                }
                _saveTrigger.tryEmit(Unit)
            }
        }
    }

    private fun createFolder(folderName: String) {
        viewModelScope.launch {
            val now = com.yusufteker.pulse.core.utils.getCurrentTimeMs()
            val request = com.yusufteker.pulse.shared.api.CreateTaskRequest(
                title = folderName,
                description = null,
                startTime = now,
                endTime = now,
                type = TaskType.FOLDER,
                status = TaskStatus.PENDING,
                visibility = TaskVisibility.PRIVATE
            )
            val result = planRepository.createTask(request)
            result.onSuccess { newFolder ->
                _state.update { it.copy(parentId = newFolder.id) }
            }
        }
    }

    private var aiJob: kotlinx.coroutines.Job? = null

    private fun processAiPrompt(prompt: String) {
        val currentTitle = _state.value.title
        val currentContent = _state.value.content

        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            _state.update { it.copy(isAiLoading = true) }
            val result = cloudAiManager.editNoteContent(currentTitle, currentContent, prompt)
            
            if (result != null) {
                _state.update { 
                    it.copy(
                        isAiLoading = false,
                        aiPreviewTitle = result.first,
                        aiPreviewContent = result.second
                    )
                }
            } else {
                _state.update { it.copy(isAiLoading = false) }
                setEffect(NoteEditorEffect.ShowToast("Yapay zeka ile bağlantı kurulamadı veya kota doldu."))
            }
        }
    }

    private fun cancelAiProcessing() {
        aiJob?.cancel()
        _state.update { it.copy(isAiLoading = false) }
    }

    private fun acceptAiPreview() {
        val previewTitle = _state.value.aiPreviewTitle
        val previewContent = _state.value.aiPreviewContent

        if (previewTitle != null && previewContent != null) {
            val (newContent, newChecklistItems) = parseContentAndChecklist(previewContent)
            
            _state.update { 
                it.copy(
                    title = previewTitle,
                    content = newContent,
                    checklist = it.checklist + newChecklistItems,
                    aiPreviewTitle = null,
                    aiPreviewContent = null
                )
            }
            _saveTrigger.tryEmit(Unit)
        }
    }

    private fun parseContentAndChecklist(content: String): Pair<String, List<com.yusufteker.pulse.shared.api.SubTask>> {
        val lines = content.lines()
        val newContentLines = mutableListOf<String>()
        val newChecklist = mutableListOf<com.yusufteker.pulse.shared.api.SubTask>()

        // Match typical markdown list formats: "- item", "* item", "1. item", "- [ ] item"
        val listPattern = Regex("^(?:-|\\*|\\d+\\.|-\\s?\\[\\s?\\])\\s+(.+)$")

        for (line in lines) {
            val match = listPattern.find(line.trim())
            if (match != null) {
                val itemTitle = match.groupValues[1].trim()
                newChecklist.add(
                    com.yusufteker.pulse.shared.api.SubTask(
                        id = com.yusufteker.pulse.core.utils.generateUUID(),
                        title = itemTitle,
                        isDone = false
                    )
                )
            } else {
                newContentLines.add(line)
            }
        }

        // Clean up excessive empty lines
        val cleanedContent = newContentLines.joinToString("\n")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()

        return Pair(cleanedContent, newChecklist)
    }

    private fun rejectAiPreview() {
        _state.update { 
            it.copy(
                aiPreviewTitle = null,
                aiPreviewContent = null
            )
        }
    }

    private fun loadNote(
        noteId: String?, 
        planRoomId: String? = null, 
        parentId: String? = null,
        sharedNote: String? = null,
        sharedSender: String? = null
    ) {
        Napier.d { "NoteEditorViewModel.loadNote: noteId=$noteId, planRoomId=$planRoomId, parentId=$parentId" }
        if (noteId == null) {
            val formattedDate = com.yusufteker.pulse.core.utils.formatFullDate(com.yusufteker.pulse.core.utils.getCurrentTimeMs())
            val finalNote = buildString {
                if (!sharedNote.isNullOrBlank()) append(sharedNote)
                if (!sharedSender.isNullOrBlank()) {
                    if (isNotEmpty()) append("\n\n")
                    append("$sharedSender tarafından paylaşıldı.")
                }
            }
            _state.value = NoteEditorState(
                dateText = formattedDate, 
                planRoomId = planRoomId, 
                parentId = parentId,
                content = finalNote
            )
            
            // Still need to load folders for new note
            viewModelScope.launch {
                planRepository.observeAllTasks().collect { tasks ->
                    val folders = tasks.filter { it.type == TaskType.FOLDER }
                    _state.update { it.copy(folders = folders) }
                }
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, id = noteId, planRoomId = planRoomId, parentId = parentId) }
            planRepository.observeAllTasks().collect { tasks ->
                val folders = tasks.filter { it.type == TaskType.FOLDER }
                val note = tasks.find { it.id == noteId && it.type == TaskType.NOTE }
                Napier.d { "NoteEditorViewModel observeAllTasks COLLECT: noteFound=${note != null}, totalTasks=${tasks.size}" }
                if (note != null) {
                    val formattedDate = com.yusufteker.pulse.core.utils.formatFullDate(note.startTime)
                    val checklist = (note.specificDetails as? ItemDetails.Note)?.checklist ?: emptyList()
                    _state.update { currentState ->
                        val newTitle = if (currentState.title.isBlank() || currentState.title == currentState.originalTask?.title) note.title else currentState.title
                        val newContent = if (currentState.content.isBlank() || currentState.content == (currentState.originalTask?.description ?: "")) note.description ?: "" else currentState.content
                        
                        Napier.d { "NoteEditorViewModel updating state with DB emission: title=$newTitle" }
                        currentState.copy(
                            originalTask = note,
                            title = newTitle,
                            content = newContent,
                            isLoading = false,
                            dateText = formattedDate,
                            parentId = note.parentId,
                            folders = folders,
                            checklist = checklist
                        )
                    }
                } else {
                    Napier.w { "NoteEditorViewModel loadNote: note $noteId not found in DB list!" }
                    _state.update { it.copy(isLoading = false, error = "Note not found", folders = folders) }
                }
            }
        }
    }

    private fun autoSaveNote() {
        if (isDeleted) return
        val currentState = _state.value
        if (currentState.isLoading || (currentState.title.isBlank() && currentState.content.isBlank())) return

        val now = com.yusufteker.pulse.core.utils.getCurrentTimeMs()
        val request = com.yusufteker.pulse.shared.api.CreateTaskRequest(
            title = currentState.title.ifBlank { "İsimsiz Not" },
            description = currentState.content,
            startTime = currentState.originalTask?.startTime ?: now,
            endTime = currentState.originalTask?.endTime ?: now,
            type = currentState.originalTask?.type ?: TaskType.NOTE,
            status = currentState.originalTask?.status ?: TaskStatus.PENDING,
            visibility = if (currentState.planRoomId != null) TaskVisibility.ROOM_SHARED else (currentState.originalTask?.visibility ?: TaskVisibility.PRIVATE),
            sharedRoomIds = currentState.planRoomId?.let { listOf(it) } ?: emptyList(),
            isRecurring = currentState.originalTask?.isRecurring ?: false,
            recurrenceRule = currentState.originalTask?.recurrenceRule,
            isFlexible = currentState.originalTask?.isFlexible ?: true,
            isOptional = currentState.originalTask?.isOptional ?: true,
            isPostponable = currentState.originalTask?.isPostponable ?: false,
            isAllDay = currentState.originalTask?.isAllDay ?: false,
            parentId = currentState.parentId,
            reminders = currentState.originalTask?.reminders ?: emptyList(),
            specificDetails = ItemDetails.Note(
                content = currentState.content,
                attachments = (currentState.originalTask?.specificDetails as? ItemDetails.Note)?.attachments ?: emptyList(),
                checklist = currentState.checklist
            ),
            tags = currentState.originalTask?.tags ?: emptyList(),
            color = currentState.originalTask?.color,
            isPinned = currentState.originalTask?.isPinned ?: false
        )

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            if (currentState.id != null) {
                planRepository.updateTask(currentState.id, request, triggerSync = false)
            } else {
                val result = planRepository.createTask(request, triggerSync = false)
                if (result.isSuccess) {
                    val newId = result.getOrNull()?.id
                    if (newId != null) {
                        _state.update { it.copy(id = newId) }
                    }
                }
            }
        }
    }

    private fun saveNote() {
        if (isDeleted) return
        val currentState = _state.value
        Napier.d { "NoteEditorViewModel.saveNote: id=${currentState.id}, title=${currentState.title}" }
        if (currentState.title.isBlank() && currentState.content.isBlank()) return

        val now = com.yusufteker.pulse.core.utils.getCurrentTimeMs()
        val request = com.yusufteker.pulse.shared.api.CreateTaskRequest(
            title = currentState.title.ifBlank { "İsimsiz Not" },
            description = currentState.content,
            startTime = currentState.originalTask?.startTime ?: now,
            endTime = currentState.originalTask?.endTime ?: now,
            type = currentState.originalTask?.type ?: TaskType.NOTE,
            status = currentState.originalTask?.status ?: TaskStatus.PENDING,
            visibility = if (currentState.planRoomId != null) TaskVisibility.ROOM_SHARED else (currentState.originalTask?.visibility ?: TaskVisibility.PRIVATE),
            sharedRoomIds = currentState.planRoomId?.let { listOf(it) } ?: emptyList(),
            isRecurring = currentState.originalTask?.isRecurring ?: false,
            recurrenceRule = currentState.originalTask?.recurrenceRule,
            isFlexible = currentState.originalTask?.isFlexible ?: true,
            isOptional = currentState.originalTask?.isOptional ?: true,
            isPostponable = currentState.originalTask?.isPostponable ?: false,
            isAllDay = currentState.originalTask?.isAllDay ?: false,
            parentId = currentState.parentId,
            reminders = currentState.originalTask?.reminders ?: emptyList(),
            specificDetails = ItemDetails.Note(
                content = currentState.content,
                attachments = (currentState.originalTask?.specificDetails as? ItemDetails.Note)?.attachments ?: emptyList(),
                checklist = currentState.checklist
            ),
            tags = currentState.originalTask?.tags ?: emptyList(),
            color = currentState.originalTask?.color,
            isPinned = currentState.originalTask?.isPinned ?: false
        )

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _state.update { it.copy(isLoading = true) }
            val isSuccess = if (currentState.id != null) {
                Napier.d { "NoteEditorViewModel.saveNote: calling updateTask for ${currentState.id}" }
                planRepository.updateTask(currentState.id, request, triggerSync = true).isSuccess
            } else {
                Napier.d { "NoteEditorViewModel.saveNote: calling createTask" }
                planRepository.createTask(request, triggerSync = true).isSuccess
            }
            _state.update { it.copy(isLoading = false) }
            
            if (isSuccess) {
                Napier.d { "NoteEditorViewModel.saveNote SUCCESS, navigating back" }
                setEffect(NoteEditorEffect.NavigateBack)
            } else {
                Napier.e { "NoteEditorViewModel.saveNote FAILED" }
                setEffect(NoteEditorEffect.ShowToast("Not güncellenemedi, lütfen tekrar deneyin."))
            }
        }
    }

    private fun deleteNote() {
        val noteId = _state.value.id ?: return
        Napier.d { "NoteEditorViewModel.deleteNote: noteId=$noteId" }
        isDeleted = true
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            // Local and remote delete
            val result = planRepository.deleteTask(noteId)
            
            _state.update { it.copy(isLoading = false) }
            if (result.isSuccess) {
                Napier.d { "NoteEditorViewModel.deleteNote SUCCESS, navigating back" }
                setEffect(NoteEditorEffect.NavigateBack)
            } else {
                val error = result.exceptionOrNull()
                Napier.e(error) { "NoteEditorViewModel.deleteNote FAILED: ${error?.message}" }
                setEffect(NoteEditorEffect.ShowToast("Not silinemedi, lütfen tekrar deneyin."))
            }
        }
    }

    private fun setEffect(effect: NoteEditorEffect) {
        viewModelScope.launch {
            _effect.emit(effect)
        }
    }
}
