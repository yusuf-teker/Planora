package com.yusufteker.pulse.feature.home.presentation.create_task

sealed interface CreateTaskEffect : com.yusufteker.pulse.core.base.UiEffect {
    object NavigateBack : CreateTaskEffect
    data class ShowToast(val message: String) : CreateTaskEffect
}
