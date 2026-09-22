package com.yusufteker.planora.feature.home.presentation.profile

import com.yusufteker.planora.core.base.UiState

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
    val pendingRequests: List<com.yusufteker.planora.shared.api.FollowRequestResponse> = emptyList(),
    val isLoadingRequests: Boolean = false,
    val calendarAccessStatus: String? = null,
    val pendingCalendarRequests: List<com.yusufteker.planora.shared.api.CalendarAccessRequestDto> = emptyList(),
    val calendarGrants: List<com.yusufteker.planora.shared.api.CalendarAccessGrantDto> = emptyList(),
    val isPremium: Boolean = false,
    val premiumUntil: String? = null
) : UiState
