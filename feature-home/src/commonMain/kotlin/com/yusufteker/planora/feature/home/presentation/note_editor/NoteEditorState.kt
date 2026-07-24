package com.yusufteker.planora.feature.home.presentation.note_editor

data class NoteEditorState(
    val id: String? = null,
    val originalTask: com.yusufteker.planora.shared.api.TaskDto? = null,
    val title: String = "",
    val content: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val dateText: String = "",
    val planRoomId: String? = null,
    val parentId: String? = null,
    val isAiLoading: Boolean = false,
    val aiPreviewTitle: String? = null,
    val aiPreviewContent: String? = null,
    val folders: List<com.yusufteker.planora.shared.api.TaskDto> = emptyList(),
    val checklist: List<com.yusufteker.planora.shared.api.SubTask> = emptyList()
)
