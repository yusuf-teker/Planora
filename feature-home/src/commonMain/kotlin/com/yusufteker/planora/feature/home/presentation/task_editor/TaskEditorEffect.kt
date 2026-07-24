package com.yusufteker.planora.feature.home.presentation.task_editor

import com.yusufteker.planora.core.base.UiEffect

sealed interface TaskEditorEffect : UiEffect {
    data object NavigateBack : TaskEditorEffect
    data class ShowSnackbar(val message: String) : TaskEditorEffect
    data class ShareItem(val url: String) : TaskEditorEffect
}
