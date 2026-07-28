package com.yusufteker.planora.feature.home.presentation.home

import com.yusufteker.planora.core.base.UiEffect

/**
 * Side effects for the Home screen.
 */
sealed interface HomeEffect : UiEffect {
    data object NavigateToProfile : HomeEffect
    data object NavigateToSettings : HomeEffect
    data class NavigateToCreateTask(val initialDateMs: Long? = null) : HomeEffect
    data class NavigateToCreateEvent(val initialDateMs: Long? = null) : HomeEffect
    data class NavigateToTaskDetail(val taskId: String) : HomeEffect
    data class NavigateToTaskEditor(val taskId: String) : HomeEffect
    data class NavigateToEventDetail(val eventId: String) : HomeEffect
    data class NavigateToEventEditor(val eventId: String) : HomeEffect
    data class NavigateToNoteEditor(val noteId: String) : HomeEffect
}
