package com.yusufteker.pulse.feature.home.presentation.search

import com.yusufteker.pulse.core.base.UiEvent

sealed interface SearchUsersEvent : UiEvent {
    data class OnQueryChanged(val query: String) : SearchUsersEvent
    data class OnToggleFollow(val userId: Int) : SearchUsersEvent
    object OnBackClicked : SearchUsersEvent
    data class OnUserClicked(val userId: Int) : SearchUsersEvent
}
