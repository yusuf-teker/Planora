package com.yusufteker.pulse.feature.home.presentation.follow_list

import com.yusufteker.pulse.core.base.UiEvent

sealed interface FollowListEvent : UiEvent {
    data object LoadAll : FollowListEvent
    data class TabSelected(val index: Int) : FollowListEvent
    data class UnfollowClicked(val userId: Int) : FollowListEvent
    data class RemoveFollowerClicked(val userId: Int) : FollowListEvent
    data class AcceptRequestClicked(val requestId: Int) : FollowListEvent
    data class RejectRequestClicked(val requestId: Int) : FollowListEvent
    data object BackClicked : FollowListEvent
}
