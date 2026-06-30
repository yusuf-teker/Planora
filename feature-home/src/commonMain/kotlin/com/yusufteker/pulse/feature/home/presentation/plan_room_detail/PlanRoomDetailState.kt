package com.yusufteker.pulse.feature.home.presentation.plan_room_detail

import com.yusufteker.pulse.shared.api.UserProfileResponse

data class PlanRoomDetailState(
    val roomId: String = "",
    val roomName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    
    // Invite Dialog State
    val isInviteDialogOpen: Boolean = false,
    val followingUsers: List<UserProfileResponse> = emptyList(),
    val isFollowingLoading: Boolean = false,
    val searchQuery: String = "",
    val inviteError: String? = null
)
