package com.yusufteker.planora.feature.home.presentation.home

import com.yusufteker.planora.core.base.UiEffect

/**
 * Side effects for the Home screen.
 */
sealed interface HomeEffect : UiEffect {
    data object NavigateToProfile : HomeEffect
    data object NavigateToSettings : HomeEffect
    data object NavigateToCreateTask : HomeEffect
    data object NavigateToCreateEvent : HomeEffect
    data class NavigateToTaskEditor(val taskId: String) : HomeEffect
    data class NavigateToEventDetail(val eventId: String) : HomeEffect
    data class NavigateToNoteEditor(val noteId: String) : HomeEffect
}
