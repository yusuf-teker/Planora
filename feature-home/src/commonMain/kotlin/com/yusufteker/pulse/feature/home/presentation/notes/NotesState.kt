package com.yusufteker.pulse.feature.home.presentation.notes

import com.yusufteker.pulse.core.base.UiState
import com.yusufteker.pulse.shared.api.TaskDto

data class NotesState(
    val isLoading: Boolean = false,
    val notes: List<TaskDto> = emptyList(),
    val selectedNoteForPreview: TaskDto? = null,
    val error: String? = null
) : UiState
