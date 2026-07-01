package com.yusufteker.pulse.feature.home.presentation.plan_room_detail

import com.yusufteker.pulse.shared.api.TaskDto
import com.yusufteker.pulse.shared.api.UserProfileResponse
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

enum class PlanRoomViewMode {
    CALENDAR,
    FEED
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
    
    // Rename Dialog State
    val isRenameDialogOpen: Boolean = false,
    val renameRoomName: String = "",
    val isRoomCreator: Boolean = false,
    
    // Calendar & Tasks State
    val viewMode: PlanRoomViewMode = PlanRoomViewMode.CALENDAR,
    val tasks: List<TaskDto> = emptyList(),
    val memberProfiles: Map<Int, UserProfileResponse> = emptyMap(),
    val currentMonth: LocalDate = getTodayDate(),
    val selectedDate: LocalDate? = null
)

private fun getTodayDate(): LocalDate {
    val millis = com.yusufteker.pulse.core.utils.getCurrentTimeMs()
    return kotlinx.datetime.Instant.fromEpochMilliseconds(millis)
        .toLocalDateTime(TimeZone.currentSystemDefault()).date
}
