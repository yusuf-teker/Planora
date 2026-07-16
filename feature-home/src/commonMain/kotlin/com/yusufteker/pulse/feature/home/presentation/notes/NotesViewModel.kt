package com.yusufteker.pulse.feature.home.presentation.notes

import com.yusufteker.pulse.core.base.BaseViewModel
import com.yusufteker.pulse.feature.home.domain.repository.PlanRepository
import com.yusufteker.pulse.shared.api.TaskType

class NotesViewModel(
    private val planRepository: PlanRepository
) : BaseViewModel<NotesState, NotesEvent, NotesEffect>(
    initialState = NotesState()
) {

    private var allTasks = emptyList<com.yusufteker.pulse.shared.api.TaskDto>()

    init {
        launch {
            planRepository.observeAllTasks().collect { tasks ->
                allTasks = tasks
                updateStateWithTasks()
            }
        }
    }

    private fun updateStateWithTasks() {
        val folders = allTasks.filter { it.type == TaskType.FOLDER }
        val selectedFolderId = state.value.selectedFolderId
        
        val notes = allTasks.filter { 
            it.type == TaskType.NOTE && (selectedFolderId == null || it.parentId == selectedFolderId) 
        }
        
        setState { copy(folders = folders, notes = notes) }
    }

    override fun onEvent(event: NotesEvent) {
        when (event) {
            is NotesEvent.RefreshRequested -> {
                setState { copy(isLoading = true) }
                launch {
                    val now = com.yusufteker.pulse.core.utils.getCurrentTimeMs()
                    planRepository.fetchMyTasks(fromTime = now - 86400000L * 30, toTime = now + 86400000L * 30)
                    setState { copy(isLoading = false) }
                }
            }
            
            is NotesEvent.NoteClicked -> {
                setEffect(NotesEffect.NavigateToTaskEditor(event.noteId))
            }
            
            is NotesEvent.NotePreviewDismissed -> {
                // No-op or remove later
            }
            
            is NotesEvent.EditNoteClicked -> {
                setEffect(NotesEffect.NavigateToTaskEditor(event.noteId))
            }
            
            is NotesEvent.CreateNoteClicked -> {
                setEffect(NotesEffect.NavigateToCreateTask)
            }
            
            is NotesEvent.FolderSelected -> {
                setState { copy(selectedFolderId = event.folderId) }
                updateStateWithTasks()
            }
        }
    }
}
