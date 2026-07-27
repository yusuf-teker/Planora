package com.yusufteker.planora.feature.home.presentation.task_editor

import com.yusufteker.planora.shared.api.UserProfileResponse
import com.yusufteker.planora.core.base.UiState
import com.yusufteker.planora.shared.api.RecurrenceRule
import com.yusufteker.planora.shared.api.TaskPriority
import com.yusufteker.planora.shared.api.TaskStatus
import com.yusufteker.planora.shared.api.TaskType

data class TaskEditorState(
    val id: String? = null,
    val originalTask: com.yusufteker.planora.shared.api.TaskDto? = null,
    val title: String = "",
    val description: String = "",
    val originalStartTime: Long? = null,
    val deadlineDateMs: Long? = null,
    val planRoomId: String? = null,
    val planRoomName: String? = null,
    val parentId: String? = null,
    val status: TaskStatus = TaskStatus.PENDING,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val isRecurring: Boolean = false,
    val recurrenceRule: RecurrenceRule? = null,
    val participants: Map<Int, String> = emptyMap(),
    val isOptional: Boolean = false,
    val reminders: List<Int> = emptyList(),
    val isDeadlinePickerVisible: Boolean = false,
    val isRepeatPickerVisible: Boolean = false,
    val isReminderPickerVisible: Boolean = false,
    val isParticipantPickerVisible: Boolean = false,
    val isPriorityPickerVisible: Boolean = false,
    val roomMembers: List<UserProfileResponse> = emptyList(),
    val isRoomMembersLoading: Boolean = false,
    val subItems: List<com.yusufteker.planora.shared.api.TaskDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isDeleted: Boolean = false

) : UiState
