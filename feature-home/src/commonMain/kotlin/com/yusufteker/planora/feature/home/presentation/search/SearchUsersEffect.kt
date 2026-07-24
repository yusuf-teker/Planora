package com.yusufteker.planora.feature.home.presentation.search

import com.yusufteker.planora.core.base.UiEffect

sealed interface SearchUsersEffect : UiEffect {
    object NavigateBack : SearchUsersEffect
    data class NavigateToProfile(val userId: Int) : SearchUsersEffect
}
