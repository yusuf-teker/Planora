package com.yusufteker.pulse.feature.home.presentation.task_editor

import com.yusufteker.pulse.core.base.UiEvent
import com.yusufteker.pulse.shared.api.RecurrenceRule

sealed interface TaskEditorEvent : UiEvent {
    data class TitleChanged(val title: String) : TaskEditorEvent
    data class DescriptionChanged(val description: String) : TaskEditorEvent
    data class OnDeadlinePickerVisibilityChanged(val isVisible: Boolean) : TaskEditorEvent
    data class OnDeadlineSelected(val dateMs: Long?) : TaskEditorEvent
    data class StatusChanged(val isCompleted: Boolean) : TaskEditorEvent
    data object SaveClicked : TaskEditorEvent
    data object DeleteClicked : TaskEditorEvent
    data object OnBackClick : TaskEditorEvent
    data class OnIsRecurringChanged(val isRecurring: Boolean) : TaskEditorEvent
    data class OnRecurrenceRuleChanged(val rule: RecurrenceRule?) : TaskEditorEvent
    data class OnRepeatPickerVisibilityChanged(val isVisible: Boolean) : TaskEditorEvent
    data class OnLoadTask(
        val taskId: String?, 
        val planRoomId: String? = null, 
        val parentId: String? = null,
        val sharedTitle: String? = null,
        val sharedNote: String? = null,
        val sharedDate: Long? = null,
        val sharedSender: String? = null
    ) : TaskEditorEvent
    data class OnIsOptionalChanged(val isOptional: Boolean) : TaskEditorEvent
    data class OnReminderPickerVisibilityChanged(val isVisible: Boolean) : TaskEditorEvent
    data class OnReminderToggled(val minutes: Int) : TaskEditorEvent
    data class OnParticipantPickerVisibilityChanged(val isVisible: Boolean) : TaskEditorEvent
    data class OnParticipantToggled(val userId: Int) : TaskEditorEvent
    data object OnDispose : TaskEditorEvent
    data object OnShareClick : TaskEditorEvent
}
