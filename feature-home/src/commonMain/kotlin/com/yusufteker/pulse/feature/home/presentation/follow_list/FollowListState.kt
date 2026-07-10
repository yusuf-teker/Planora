package com.yusufteker.pulse.feature.home.presentation.follow_list

import com.yusufteker.pulse.core.base.UiState
import com.yusufteker.pulse.shared.api.FollowRequestResponse
import com.yusufteker.pulse.shared.api.UserProfileResponse

data class FollowListState(
    val selectedTab: Int = 0,
    val followers: List<UserProfileResponse> = emptyList(),
    val following: List<UserProfileResponse> = emptyList(),
    val requests: List<FollowRequestResponse> = emptyList(),
    val isLoadingFollowers: Boolean = false,
    val isLoadingFollowing: Boolean = false,
    val isLoadingRequests: Boolean = false
) : UiState
