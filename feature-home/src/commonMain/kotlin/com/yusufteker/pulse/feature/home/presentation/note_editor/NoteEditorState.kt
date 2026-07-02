package com.yusufteker.pulse.feature.home.presentation.note_editor

data class NoteEditorState(
    val id: String? = null,
    val title: String = "",
    val content: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val dateText: String = "" // Added dateText to hold formatted date
)
