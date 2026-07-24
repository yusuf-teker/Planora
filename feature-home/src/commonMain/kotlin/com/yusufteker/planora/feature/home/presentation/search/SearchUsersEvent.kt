package com.yusufteker.planora.feature.home.presentation.search

import com.yusufteker.planora.core.base.UiEvent

sealed interface SearchUsersEvent : UiEvent {
    data class OnQueryChanged(val query: String) : SearchUsersEvent
    data class OnToggleFollow(val userId: Int) : SearchUsersEvent
    object OnBackClicked : SearchUsersEvent
    data class OnUserClicked(val userId: Int) : SearchUsersEvent
}
