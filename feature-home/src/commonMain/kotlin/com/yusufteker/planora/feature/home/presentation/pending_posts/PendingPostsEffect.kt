package com.yusufteker.planora.feature.home.presentation.pending_posts

import com.yusufteker.planora.core.base.UiEffect

sealed interface PendingPostsEffect : UiEffect {
    data class NavigateToEditPost(val postId: String) : PendingPostsEffect
}
