package com.yusufteker.pulse.feature.home.presentation.profile

import com.yusufteker.pulse.core.base.UiEffect

sealed interface ProfileEffect : UiEffect {
    data object NavigateBack : ProfileEffect
    data object NavigateToLogin : ProfileEffect
    data class NavigateToFollowList(val tab: Int) : ProfileEffect
}
