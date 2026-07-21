package com.yusufteker.pulse.feature.home.presentation.plan_room_detail

import kotlinx.datetime.LocalDate

sealed interface PlanRoomDetailEvent : com.yusufteker.pulse.core.base.UiEvent {
    data class LoadRoom(val roomId: String) : PlanRoomDetailEvent
    object OnBackClick : PlanRoomDetailEvent
    object OnInviteUserClick : PlanRoomDetailEvent
    object OnDismissInviteDialog : PlanRoomDetailEvent
    object OnCreateTaskClick : PlanRoomDetailEvent
    object OnCreateEventClick : PlanRoomDetailEvent
    data class OnSearchQueryChange(val query: String) : PlanRoomDetailEvent
    data class OnUserSelectToInvite(val userId: Int) : PlanRoomDetailEvent
    data class OnTaskClick(val task: com.yusufteker.pulse.shared.api.TaskDto) : PlanRoomDetailEvent
    
    // Rename & Delete Room Events
    object OnEditRoomClick : PlanRoomDetailEvent
    object OnDismissRenameDialog : PlanRoomDetailEvent
    data class OnRenameRoomNameChange(val name: String) : PlanRoomDetailEvent
    object OnRenameRoomSubmit : PlanRoomDetailEvent
    object OnDeleteRoomClick : PlanRoomDetailEvent
}
