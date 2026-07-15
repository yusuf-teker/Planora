package com.yusufteker.pulse.feature.home.presentation.notes

import com.yusufteker.pulse.core.base.BaseViewModel
import com.yusufteker.pulse.feature.home.domain.repository.PlanRepository
import com.yusufteker.pulse.shared.api.TaskType

class NotesViewModel(
    private val planRepository: PlanRepository
) : BaseViewModel<NotesState, NotesEvent, NotesEffect>(
    initialState = NotesState()
) {

    init {
        launch {
            planRepository.observeAllTasks().collect { tasks ->
                val notes = tasks.filter { it.type == TaskType.NOTE && it.parentId == null }
                setState { copy(notes = notes) }
            }
        }
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
        }
    }
}
