package com.yusufteker.pulse.feature.home.presentation.plan_room_detail

import com.yusufteker.pulse.shared.api.UserProfileResponse

enum class RoomDetailTab {
    FEED,
    CALENDAR
}

enum class RoomTaskFilter {
    ALL,
    TASKS,
    EVENTS,
    NOTES
}

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
    
    // Rename & Dialog & BottomSheet States
    val isEditRoomBottomSheetOpen: Boolean = false,
    val isRenameDialogOpen: Boolean = false,
    val renameRoomName: String = "",
    val isRoomCreator: Boolean = false,
    val isDeleteConfirmationOpen: Boolean = false,
    val isLeaveConfirmationOpen: Boolean = false,
    
    // View Tab & Filtering
    val selectedTab: RoomDetailTab = RoomDetailTab.FEED,
    val selectedFilter: RoomTaskFilter = RoomTaskFilter.ALL,
    val taskSearchQuery: String = "",
    
    // Calendar State
    val calendarCurrentMonth: kotlinx.datetime.LocalDate? = null,
    val calendarSelectedDate: kotlinx.datetime.LocalDate? = null,
    
    // Room Members State
    val memberProfiles: Map<Int, UserProfileResponse> = emptyMap(),
    val isMembersLoading: Boolean = true,
    val selectedMemberUserIdsFilter: Set<Int> = emptySet(),
    val roomColor: String? = null,
    
    // Room Details & Image
    val roomImageUrl: String? = null,
    val isUploadingImage: Boolean = false,
    
    // Member Removal
    val memberToRemove: UserProfileResponse? = null,
    val isRemoveMemberDialogOpen: Boolean = false,
    
    // Room Tasks State
    val roomTasks: List<com.yusufteker.pulse.shared.api.TaskDto> = emptyList(),
    val myUserId: String = ""
) : com.yusufteker.pulse.core.base.UiState

