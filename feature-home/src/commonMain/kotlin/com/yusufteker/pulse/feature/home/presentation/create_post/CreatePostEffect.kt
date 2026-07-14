package com.yusufteker.pulse.feature.home.presentation.create_post

import com.yusufteker.pulse.core.base.UiEffect

sealed interface CreatePostEffect : UiEffect {
    data object NavigateBack : CreatePostEffect
    data class ShowError(val message: String) : CreatePostEffect
}
