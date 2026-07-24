package com.yusufteker.planora.feature.home.presentation.profile

import com.yusufteker.planora.core.base.UiEffect

sealed interface ProfileEffect : UiEffect {
    data object NavigateBack : ProfileEffect
    data object NavigateToLogin : ProfileEffect
    data class NavigateToFollowList(val tab: Int) : ProfileEffect
}
