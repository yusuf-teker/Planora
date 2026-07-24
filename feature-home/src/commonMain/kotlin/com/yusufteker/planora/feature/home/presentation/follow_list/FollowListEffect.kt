package com.yusufteker.planora.feature.home.presentation.follow_list

import com.yusufteker.planora.core.base.UiEffect

sealed interface FollowListEffect : UiEffect {
    data object NavigateBack : FollowListEffect
}
