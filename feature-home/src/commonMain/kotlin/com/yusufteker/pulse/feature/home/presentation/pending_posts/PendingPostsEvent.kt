package com.yusufteker.pulse.feature.home.presentation.pending_posts

import com.yusufteker.pulse.core.base.UiEvent

sealed interface PendingPostsEvent : UiEvent {
    data class OnPostClicked(val postId: String) : PendingPostsEvent
    data class OnDeleteClicked(val postId: String) : PendingPostsEvent
}
