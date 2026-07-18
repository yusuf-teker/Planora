package com.yusufteker.pulse.feature.home.presentation.note_editor

sealed class NoteEditorEvent {
    data class OnLoadNote(
        val noteId: String?, 
        val planRoomId: String? = null, 
        val parentId: String? = null,
        val sharedNote: String? = null,
        val sharedSender: String? = null
    ) : NoteEditorEvent()
    data class OnTitleChange(val title: String) : NoteEditorEvent()
    data class OnContentChange(val content: String) : NoteEditorEvent()
    object OnSaveClick : NoteEditorEvent()
    object OnDeleteClick : NoteEditorEvent()
    object OnBackClick : NoteEditorEvent()
    data class OnAiActionClick(val prompt: String) : NoteEditorEvent()
    object OnAiCancelClick : NoteEditorEvent()
    object OnAiPreviewAccept : NoteEditorEvent()
    object OnAiPreviewReject : NoteEditorEvent()
    
    data class OnFolderSelected(val folderId: String?) : NoteEditorEvent()
    data class OnCreateFolderClick(val folderName: String) : NoteEditorEvent()
    data class OnAddChecklistItem(val title: String) : NoteEditorEvent()
    data class OnToggleChecklistItem(val itemId: String) : NoteEditorEvent()
    data class OnDeleteChecklistItem(val itemId: String) : NoteEditorEvent()
    data class OnUpdateChecklistItem(val itemId: String, val newTitle: String) : NoteEditorEvent()
    object OnDispose : NoteEditorEvent()
    object OnShareClick : NoteEditorEvent()
}
