package com.yusufteker.planora.feature.home.presentation.follow_list

import com.yusufteker.planora.core.base.UiState
import com.yusufteker.planora.shared.api.FollowRequestResponse
import com.yusufteker.planora.shared.api.UserProfileResponse

data class FollowListState(
    val selectedTab: Int = 0,
    val followers: List<UserProfileResponse> = emptyList(),
    val following: List<UserProfileResponse> = emptyList(),
    val requests: List<FollowRequestResponse> = emptyList(),
    val isLoadingFollowers: Boolean = false,
    val isLoadingFollowing: Boolean = false,
    val isLoadingRequests: Boolean = false
) : UiState
