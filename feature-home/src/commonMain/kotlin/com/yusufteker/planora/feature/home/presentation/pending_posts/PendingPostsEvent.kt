package com.yusufteker.planora.feature.home.presentation.pending_posts

import com.yusufteker.planora.core.base.UiEvent

sealed interface PendingPostsEvent : UiEvent {
    data class OnPostClicked(val postId: String) : PendingPostsEvent
    data class OnDeleteClicked(val postId: String) : PendingPostsEvent
}
