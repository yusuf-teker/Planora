package com.yusufteker.planora.feature.home.presentation.pending_posts

import com.yusufteker.planora.core.base.UiState
import com.yusufteker.planora.core.database.PendingPostEntity

data class PendingPostsState(
    val posts: List<PendingPostEntity> = emptyList(),
    val isLoading: Boolean = true
) : UiState
