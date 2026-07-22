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
    
    // Rename, Delete & Leave Room Events
    object OnEditRoomClick : PlanRoomDetailEvent
    object OnDismissRenameDialog : PlanRoomDetailEvent
    data class OnRenameRoomNameChange(val name: String) : PlanRoomDetailEvent
    object OnRenameRoomSubmit : PlanRoomDetailEvent
    
    object OnDeleteRoomClick : PlanRoomDetailEvent
    object OnConfirmDeleteRoom : PlanRoomDetailEvent
    object OnDismissDeleteDialog : PlanRoomDetailEvent
    
    object OnLeaveRoomClick : PlanRoomDetailEvent
    object OnConfirmLeaveRoom : PlanRoomDetailEvent
    object OnDismissLeaveDialog : PlanRoomDetailEvent
    
    // Tab & Filter & Calendar Events
    data class OnTabSelected(val tab: RoomDetailTab) : PlanRoomDetailEvent
    data class OnFilterSelected(val filter: RoomTaskFilter) : PlanRoomDetailEvent
    data class OnMemberFilterSelected(val userId: Int?) : PlanRoomDetailEvent
    data class OnTaskSearchQueryChange(val query: String) : PlanRoomDetailEvent
    data class OnCalendarDateSelected(val date: LocalDate) : PlanRoomDetailEvent
    object OnCalendarPreviousMonth : PlanRoomDetailEvent
    object OnCalendarNextMonth : PlanRoomDetailEvent
    object OnCopyInviteLinkClick : PlanRoomDetailEvent
}

