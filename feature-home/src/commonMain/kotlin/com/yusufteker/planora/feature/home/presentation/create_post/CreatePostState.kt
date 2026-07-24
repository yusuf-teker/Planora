package com.yusufteker.planora.feature.home.presentation.create_post

import com.yusufteker.planora.core.base.UiState

data class CreatePostState(
    val content: String = "",
    val isSaving: Boolean = false,
    val isEditing: Boolean = false,
    val originalIsDraft: Boolean = false,
    val selectedTopic: String = "GENERAL"
) : UiState
