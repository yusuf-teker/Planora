package com.yusufteker.pulse.feature.home.presentation.search

import com.yusufteker.pulse.core.base.UiState
import com.yusufteker.pulse.shared.api.UserProfileResponse

data class SearchUsersState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<UserProfileResponse> = emptyList(),
    val error: String? = null
) : UiState
