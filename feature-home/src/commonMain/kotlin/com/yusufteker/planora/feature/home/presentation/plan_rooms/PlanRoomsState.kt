package com.yusufteker.planora.feature.home.presentation.plan_rooms

import com.yusufteker.planora.core.base.UiEffect
import com.yusufteker.planora.core.base.UiEvent
import com.yusufteker.planora.core.base.UiState
import com.yusufteker.planora.shared.api.PlanRoomDto

data class PlanRoomsState(
    val isLoading: Boolean = false,
    val rooms: List<PlanRoomDto> = emptyList(), // Local DB'den veya API'den çekilecek odalar
    val memberProfiles: Map<Int, com.yusufteker.planora.shared.api.UserProfileResponse> = emptyMap(),
    val pendingInvitations: List<PlanRoomDto> = emptyList(),
    val processingInviteIds: Set<String> = emptySet(),
    
    // UI State for Dialogs/BottomSheets
    val isCreateRoomDialogVisible: Boolean = false,
    val isInviteDialogVisible: Boolean = false,
    val isInvitationsDialogVisible: Boolean = false,
    
    val selectedRoomIdForInvite: String? = null,
    val createRoomNameInput: String = "",
    val inviteUserIdInput: String = "", // İleride arama ile çalışır, şimdilik basit ID girebilir
    
    val isFabExpanded: Boolean = false
) : UiState

sealed interface PlanRoomsEvent : UiEvent {
    data object LoadRooms : PlanRoomsEvent
    data object LoadPendingInvitations : PlanRoomsEvent
    
    data class OnCreateRoomClick(val isVisible: Boolean) : PlanRoomsEvent
    data class OnCreateRoomNameChanged(val name: String) : PlanRoomsEvent
    data object SubmitCreateRoom : PlanRoomsEvent
    
    data class OnInviteClick(val roomId: String, val isVisible: Boolean) : PlanRoomsEvent
    data class OnInviteUserIdChanged(val userIdStr: String) : PlanRoomsEvent
    data object SubmitInvite : PlanRoomsEvent
    
    data class OnInvitationsClick(val isVisible: Boolean) : PlanRoomsEvent
    data class RespondToInvite(val roomId: String, val accept: Boolean) : PlanRoomsEvent
    
    data class OnRoomClick(val roomId: String) : PlanRoomsEvent // Odanın detayına/takvimine gitmek için
    
    data object ToggleFab : PlanRoomsEvent
    data object OnCreateTaskClick : PlanRoomsEvent
    data object OnCreateEventClick : PlanRoomsEvent
}

sealed interface PlanRoomsEffect : UiEffect {
    data class ShowToast(val message: String) : PlanRoomsEffect
    data class NavigateToRoomDetail(val roomId: String) : PlanRoomsEffect
    data object NavigateToCreateTask : PlanRoomsEffect
    data object NavigateToCreateEvent : PlanRoomsEffect
}
