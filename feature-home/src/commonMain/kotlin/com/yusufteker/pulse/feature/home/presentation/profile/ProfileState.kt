package com.yusufteker.pulse.feature.home.presentation.profile

import com.yusufteker.pulse.core.base.UiState

data class ProfileState(
    val isLoggedIn: Boolean = false,
    val name: String = "Kullanıcı Adı",
    val username: String = "",
    val avatarId: String = "avatar_1",
    val profileImageUrl: String? = null,
    val isUploadingImage: Boolean = false,
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val postsCount: Int = 0,
    val isFollowedByMe: Boolean = false,
    val isMyProfile: Boolean = true,
    val profileId: Int? = null,
    val followRequestStatus: String? = null,
    val pendingRequests: List<com.yusufteker.pulse.shared.api.FollowRequestResponse> = emptyList(),
    val isLoadingRequests: Boolean = false,
    val calendarAccessStatus: String? = null,
    val pendingCalendarRequests: List<com.yusufteker.pulse.shared.api.CalendarAccessRequestDto> = emptyList(),
    val calendarGrants: List<com.yusufteker.pulse.shared.api.CalendarAccessGrantDto> = emptyList()
) : UiState
