package com.yusufteker.pulse.feature.home.presentation.pending_posts

import com.yusufteker.pulse.core.base.UiState
import com.yusufteker.pulse.core.database.PendingPostEntity

data class PendingPostsState(
    val posts: List<PendingPostEntity> = emptyList(),
    val isLoading: Boolean = true
) : UiState
