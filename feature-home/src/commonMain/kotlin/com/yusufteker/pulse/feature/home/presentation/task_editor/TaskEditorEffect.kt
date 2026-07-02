package com.yusufteker.pulse.feature.home.presentation.task_editor

import com.yusufteker.pulse.core.base.UiEffect

sealed interface TaskEditorEffect : UiEffect {
    data object NavigateBack : TaskEditorEffect
    data class ShowSnackbar(val message: String) : TaskEditorEffect
}
