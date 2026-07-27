package com.yusufteker.planora.feature.home.presentation.task_detail

sealed interface TaskDetailEvent {
    data class OnLoadTask(
        val taskId: String? = null,
        val planRoomId: String? = null,
        val sharedTitle: String? = null,
        val sharedNote: String? = null,
        val sharedDate: Long? = null,
        val sharedSender: String? = null
    ) : TaskDetailEvent

    data object OnToggleStatus : TaskDetailEvent
    data class OnToggleSubtask(val subtaskId: String) : TaskDetailEvent
    data object OnEditClick : TaskDetailEvent
    data object OnFocusClick : TaskDetailEvent
    data object OnDeleteClick : TaskDetailEvent
    data object OnBackClick : TaskDetailEvent
    data object OnShareClick : TaskDetailEvent
}
