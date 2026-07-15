package com.yusufteker.pulse.feature.home.presentation.note_editor

sealed class NoteEditorEvent {
    data class OnLoadNote(val noteId: String?, val planRoomId: String? = null, val parentId: String? = null) : NoteEditorEvent()
    data class OnTitleChange(val title: String) : NoteEditorEvent()
    data class OnContentChange(val content: String) : NoteEditorEvent()
    object OnSaveClick : NoteEditorEvent()
    object OnDeleteClick : NoteEditorEvent()
    object OnBackClick : NoteEditorEvent()
    data class OnAiActionClick(val prompt: String) : NoteEditorEvent()
    object OnAiCancelClick : NoteEditorEvent()
    object OnAiPreviewAccept : NoteEditorEvent()
    object OnAiPreviewReject : NoteEditorEvent()
}
