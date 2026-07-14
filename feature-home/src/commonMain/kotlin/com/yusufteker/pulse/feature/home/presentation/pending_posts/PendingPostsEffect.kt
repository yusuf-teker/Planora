package com.yusufteker.pulse.feature.home.presentation.pending_posts

import com.yusufteker.pulse.core.base.UiEffect

sealed interface PendingPostsEffect : UiEffect {
    data class NavigateToEditPost(val postId: String) : PendingPostsEffect
}
