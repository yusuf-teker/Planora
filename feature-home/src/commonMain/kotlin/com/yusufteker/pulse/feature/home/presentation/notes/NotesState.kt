package com.yusufteker.pulse.feature.home.presentation.notes

import com.yusufteker.pulse.core.base.UiState
import com.yusufteker.pulse.shared.api.TaskDto

data class NotesState(
    val isLoading: Boolean = false,
    val notes: List<TaskDto> = emptyList(),
    val pinnedNotes: List<TaskDto> = emptyList(),
    val unpinnedNotes: List<TaskDto> = emptyList(),
    val folders: List<TaskDto> = emptyList(),
    val selectedFolderId: String? = null,
    val selectedNoteForPreview: TaskDto? = null,
    val error: String? = null,
    val searchQuery: String = "",
    val isGridView: Boolean = true
) : UiState
