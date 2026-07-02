package com.yusufteker.pulse.feature.home.presentation.plan_room_detail

import com.yusufteker.pulse.shared.api.UserProfileResponse

data class PlanRoomDetailState(
    val roomId: String = "",
    val roomName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    
    val isInviteDialogOpen: Boolean = false,
    val followingUsers: List<UserProfileResponse> = emptyList(),
    val isFollowingLoading: Boolean = false,
    val searchQuery: String = "",
    val inviteError: String? = null,
    
    // Rename Dialog State
    val isRenameDialogOpen: Boolean = false,
    val renameRoomName: String = "",
    val isRoomCreator: Boolean = false,
    
    // Room Members State
    val memberProfiles: Map<Int, UserProfileResponse> = emptyMap()
)
