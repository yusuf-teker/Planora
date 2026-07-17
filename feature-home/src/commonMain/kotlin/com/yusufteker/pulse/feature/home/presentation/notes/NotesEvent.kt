package com.yusufteker.pulse.feature.home.presentation.notes

import com.yusufteker.pulse.core.base.UiEvent

sealed interface NotesEvent : UiEvent {
    data object RefreshRequested : NotesEvent
    data class NoteClicked(val noteId: String) : NotesEvent
    data class EditNoteClicked(val noteId: String) : NotesEvent
    data object NotePreviewDismissed : NotesEvent
    data object CreateNoteClicked : NotesEvent
    data class FolderSelected(val folderId: String?) : NotesEvent
    data class SearchQueryChanged(val query: String) : NotesEvent
    data object ToggleViewMode : NotesEvent
    data class TogglePin(val noteId: String) : NotesEvent
    data class DeleteNote(val noteId: String) : NotesEvent
}
