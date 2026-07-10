package com.yusufteker.pulse.feature.home.presentation.follow_list

import com.yusufteker.pulse.core.base.UiEffect

sealed interface FollowListEffect : UiEffect {
    data object NavigateBack : FollowListEffect
}
