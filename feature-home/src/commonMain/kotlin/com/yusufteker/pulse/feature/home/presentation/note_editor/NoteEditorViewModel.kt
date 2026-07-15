package com.yusufteker.pulse.feature.home.presentation.note_editor

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
import kotlinx.datetime.Clock

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

    init {
        loadNote(noteId, planRoomId, parentId)
    }

    fun onEvent(event: NoteEditorEvent) {
        when (event) {
            is NoteEditorEvent.OnLoadNote -> loadNote(event.noteId, event.planRoomId, event.parentId)
            is NoteEditorEvent.OnTitleChange -> _state.update { it.copy(title = event.title) }
            is NoteEditorEvent.OnContentChange -> _state.update { it.copy(content = event.content) }
            NoteEditorEvent.OnSaveClick -> saveNote()
            NoteEditorEvent.OnDeleteClick -> deleteNote()
            NoteEditorEvent.OnBackClick -> {
                saveNote() // auto-save on back
            }
            is NoteEditorEvent.OnAiActionClick -> processAiPrompt(event.prompt)
            NoteEditorEvent.OnAiCancelClick -> cancelAiProcessing()
            NoteEditorEvent.OnAiPreviewAccept -> acceptAiPreview()
            NoteEditorEvent.OnAiPreviewReject -> rejectAiPreview()
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
            _state.update { 
                it.copy(
                    title = previewTitle,
                    content = previewContent,
                    aiPreviewTitle = null,
                    aiPreviewContent = null
                )
            }
        }
    }

    private fun rejectAiPreview() {
        _state.update { 
            it.copy(
                aiPreviewTitle = null,
                aiPreviewContent = null
            )
        }
    }

    private fun loadNote(noteId: String?, planRoomId: String? = null, parentId: String? = null) {
        if (noteId == null) {
            // New note: Clear previous state entirely (in case ViewModel is reused)
            val formattedDate = com.yusufteker.pulse.core.utils.formatFullDate(com.yusufteker.pulse.core.utils.getCurrentTimeMs())
            _state.value = NoteEditorState(dateText = formattedDate, planRoomId = planRoomId, parentId = parentId)
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, id = noteId, planRoomId = planRoomId, parentId = parentId) }
            planRepository.observeAllTasks().collect { tasks ->
                val note = tasks.find { it.id == noteId && it.type == TaskType.NOTE }
                if (note != null) {
                    val formattedDate = com.yusufteker.pulse.core.utils.formatFullDate(note.startTime)
                    _state.update { 
                        it.copy(
                            title = note.title,
                            content = note.description ?: "",
                            isLoading = false,
                            dateText = formattedDate,
                            parentId = note.parentId
                        ) 
                    }
                } else {
                    _state.update { it.copy(isLoading = false, error = "Note not found") }
                }
            }
        }
    }

    private fun saveNote() {
        val currentState = _state.value
        if (currentState.title.isBlank() && currentState.content.isBlank()) {
            setEffect(NoteEditorEffect.NavigateBack) // Empty note, just close
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            val now = com.yusufteker.pulse.core.utils.getCurrentTimeMs()

            val request = com.yusufteker.pulse.shared.api.CreateTaskRequest(
                title = currentState.title.ifBlank { "İsimsiz Not" },
                description = currentState.content,
                startTime = now,
                endTime = now,
                type = TaskType.NOTE,
                status = TaskStatus.PENDING,
                visibility = if (currentState.planRoomId != null) TaskVisibility.ROOM_SHARED else TaskVisibility.PRIVATE,
                sharedRoomIds = currentState.planRoomId?.let { listOf(it) } ?: emptyList(),
                isRecurring = false,
                recurrenceRule = null,
                isFlexible = true,
                isOptional = true,
                isPostponable = false,
                isAllDay = false,
                parentId = currentState.parentId,
                reminders = emptyList(),
                specificDetails = ItemDetails.Note(content = currentState.content, attachments = emptyList()),
                tags = emptyList(),
                color = null
            )

            // Save to local DB or remote
            val result = if (currentState.id != null) {
                planRepository.updateTask(currentState.id, request)
            } else {
                planRepository.createTask(request)
            }

            if (result.isFailure) {
                println("Note save failed: ${result.exceptionOrNull()?.message}")
                result.exceptionOrNull()?.printStackTrace()
            }
            
            _state.update { it.copy(isLoading = false) }
            setEffect(NoteEditorEffect.NavigateBack)
        }
    }

    private fun deleteNote() {
        val noteId = _state.value.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            // Local and remote delete
            planRepository.deleteTask(noteId)
            
            _state.update { it.copy(isLoading = false) }
            setEffect(NoteEditorEffect.NavigateBack)
        }
    }

    private fun setEffect(effect: NoteEditorEffect) {
        viewModelScope.launch {
            _effect.emit(effect)
        }
    }
}
