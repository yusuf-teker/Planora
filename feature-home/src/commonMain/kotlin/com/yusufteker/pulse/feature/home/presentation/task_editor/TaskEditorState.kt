package com.yusufteker.pulse.feature.home.presentation.task_editor

import com.yusufteker.pulse.shared.api.UserProfileResponse
import com.yusufteker.pulse.core.base.UiState
import com.yusufteker.pulse.shared.api.RecurrenceRule
import com.yusufteker.pulse.shared.api.TaskStatus
import com.yusufteker.pulse.shared.api.TaskType

data class TaskEditorState(
    val id: String? = null,
    val title: String = "",
    val description: String = "",
    val originalStartTime: Long? = null,
    val deadlineDateMs: Long? = null,
    val planRoomId: String? = null,
    val status: TaskStatus = TaskStatus.PENDING,
    val isRecurring: Boolean = false,
    val recurrenceRule: RecurrenceRule? = null,
    val participants: Map<Int, String> = emptyMap(),
    val isOptional: Boolean = false,
    val reminders: List<Int> = emptyList(),
    val isDeadlinePickerVisible: Boolean = false,
    val isRepeatPickerVisible: Boolean = false,
    val isReminderPickerVisible: Boolean = false,
    val isParticipantPickerVisible: Boolean = false,
    val roomMembers: List<UserProfileResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) : UiState
