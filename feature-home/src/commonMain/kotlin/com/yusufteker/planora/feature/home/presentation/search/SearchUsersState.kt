package com.yusufteker.planora.feature.home.presentation.search

import com.yusufteker.planora.core.base.UiState
import com.yusufteker.planora.shared.api.UserProfileResponse

data class SearchUsersState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<UserProfileResponse> = emptyList(),
    val error: String? = null
) : UiState
