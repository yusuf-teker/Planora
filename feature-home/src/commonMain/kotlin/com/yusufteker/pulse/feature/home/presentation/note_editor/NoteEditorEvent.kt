package com.yusufteker.pulse.feature.home.presentation.note_editor

sealed class NoteEditorEvent {
    data class OnLoadNote(val noteId: String?) : NoteEditorEvent()
    data class OnTitleChange(val title: String) : NoteEditorEvent()
    data class OnContentChange(val content: String) : NoteEditorEvent()
    object OnSaveClick : NoteEditorEvent()
    object OnDeleteClick : NoteEditorEvent()
    object OnBackClick : NoteEditorEvent()
}
