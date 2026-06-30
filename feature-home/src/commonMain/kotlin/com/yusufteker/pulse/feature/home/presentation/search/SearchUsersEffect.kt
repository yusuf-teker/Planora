package com.yusufteker.pulse.feature.home.presentation.search

import com.yusufteker.pulse.core.base.UiEffect

sealed interface SearchUsersEffect : UiEffect {
    object NavigateBack : SearchUsersEffect
    data class NavigateToProfile(val userId: Int) : SearchUsersEffect
}
