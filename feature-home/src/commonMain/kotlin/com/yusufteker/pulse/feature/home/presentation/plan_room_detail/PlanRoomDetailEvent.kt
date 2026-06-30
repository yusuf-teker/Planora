package com.yusufteker.pulse.feature.home.presentation.plan_room_detail

sealed interface PlanRoomDetailEvent {
    data class LoadRoom(val roomId: String) : PlanRoomDetailEvent
    object OnBackClick : PlanRoomDetailEvent
    object OnInviteUserClick : PlanRoomDetailEvent
    object OnDismissInviteDialog : PlanRoomDetailEvent
    data class OnSearchQueryChange(val query: String) : PlanRoomDetailEvent
    data class OnUserSelectToInvite(val userId: Int) : PlanRoomDetailEvent
}
