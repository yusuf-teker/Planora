package com.yusufteker.pulse.feature.home.presentation.note_editor

sealed interface NoteEditorEffect {
    object NavigateBack : NoteEditorEffect
    data class ShowToast(val message: String) : NoteEditorEffect
    data class ShareItem(val url: String) : NoteEditorEffect
}
