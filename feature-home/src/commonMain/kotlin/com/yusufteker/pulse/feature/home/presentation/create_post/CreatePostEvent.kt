package com.yusufteker.pulse.feature.home.presentation.create_post

import com.yusufteker.pulse.core.base.UiEvent

sealed interface CreatePostEvent : UiEvent {
    data class OnPost(val content: String) : CreatePostEvent
    data class OnSaveDraft(val content: String) : CreatePostEvent
    data class OnTopicSelected(val topic: String) : CreatePostEvent
}
