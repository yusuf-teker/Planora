package com.yusufteker.planora.feature.home.presentation.create_post

import com.yusufteker.planora.core.base.UiEvent

sealed interface CreatePostEvent : UiEvent {
    data class OnPost(val content: String) : CreatePostEvent
    data class OnSaveDraft(val content: String) : CreatePostEvent
    data class OnTopicSelected(val topic: String) : CreatePostEvent
}
