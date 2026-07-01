package com.yusufteker.pulse.feature.home.presentation.plan_room_detail

import kotlinx.datetime.LocalDate

sealed interface PlanRoomDetailEvent {
    data class LoadRoom(val roomId: String) : PlanRoomDetailEvent
    object OnBackClick : PlanRoomDetailEvent
    object OnInviteUserClick : PlanRoomDetailEvent
    object OnDismissInviteDialog : PlanRoomDetailEvent
    data class OnSearchQueryChange(val query: String) : PlanRoomDetailEvent
    data class OnUserSelectToInvite(val userId: Int) : PlanRoomDetailEvent
    
    // Calendar Events
    data class OnDateSelected(val date: LocalDate) : PlanRoomDetailEvent
    object OnPreviousMonth : PlanRoomDetailEvent
    object OnNextMonth : PlanRoomDetailEvent
    data class OnViewModeChange(val mode: PlanRoomViewMode) : PlanRoomDetailEvent
    
    // Rename & Delete Room Events
    object OnEditRoomClick : PlanRoomDetailEvent
    object OnDismissRenameDialog : PlanRoomDetailEvent
    data class OnRenameRoomNameChange(val name: String) : PlanRoomDetailEvent
    object OnRenameRoomSubmit : PlanRoomDetailEvent
    object OnDeleteRoomClick : PlanRoomDetailEvent
}
