package com.yusufteker.planora.feature.home.presentation.task_detail

sealed interface TaskDetailEffect {
    data object NavigateBack : TaskDetailEffect
    data class NavigateToEditTask(val taskId: String, val planRoomId: String?) : TaskDetailEffect
    data class NavigateToFocus(val taskId: String) : TaskDetailEffect
    data class NavigateToCopyTask(val taskId: String, val planRoomId: String?) : TaskDetailEffect
    data class ShowSnackbar(val message: String) : TaskDetailEffect
    data class ShareItem(val url: String) : TaskDetailEffect
}
