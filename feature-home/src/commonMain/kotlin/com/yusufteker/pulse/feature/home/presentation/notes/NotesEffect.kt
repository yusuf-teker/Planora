package com.yusufteker.pulse.feature.home.presentation.notes

import com.yusufteker.pulse.core.base.UiEffect

sealed interface NotesEffect : UiEffect {
    data class NavigateToTaskEditor(val noteId: String) : NotesEffect
    data object NavigateToCreateTask : NotesEffect
}
