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
    data object OnCopyClick : TaskDetailEvent
    data class OnQuickDuplicate(val targetDateMs: Long) : TaskDetailEvent

    /**
     * Triggers adding a quick child note to this task.
     *
     * @param content The text content of the quick note.
     */
    data class OnAddQuickNote(val content: String) : TaskDetailEvent

    /**
     * Triggers deletion of a child note attached to this task.
     *
     * @param noteId The ID of the note to be deleted.
     */
    data class OnDeleteNote(val noteId: String) : TaskDetailEvent
}
