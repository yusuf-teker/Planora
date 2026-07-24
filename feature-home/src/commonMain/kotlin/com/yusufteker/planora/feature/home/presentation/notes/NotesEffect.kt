package com.yusufteker.planora.feature.home.presentation.notes

import com.yusufteker.planora.core.base.UiEffect

sealed interface NotesEffect : UiEffect {
    data class NavigateToTaskEditor(val noteId: String) : NotesEffect
    data object NavigateToCreateTask : NotesEffect
}
