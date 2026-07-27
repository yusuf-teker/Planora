package com.yusufteker.planora.feature.home.presentation.task_detail

import com.yusufteker.planora.shared.api.RecurrenceRule
import com.yusufteker.planora.shared.api.TaskDto
import com.yusufteker.planora.shared.api.TaskParticipantDto
import com.yusufteker.planora.shared.api.TaskPriority
import com.yusufteker.planora.shared.api.TaskStatus

data class TaskDetailState(
    val taskId: String? = null,
    val title: String = "",
    val description: String = "",
    val status: TaskStatus = TaskStatus.PENDING,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val dueDateMs: Long? = null,
    val startTimeMs: Long? = null,
    val endTimeMs: Long? = null,
    val isAllDay: Boolean = false,
    val isRecurring: Boolean = false,
    val recurrenceRule: RecurrenceRule? = null,
    val reminders: List<Int> = emptyList(),
    val planRoomId: String? = null,
    val planRoomName: String? = null,
    val planRoomImageUrl: String? = null,
    val participants: List<TaskParticipantDto> = emptyList(),
    val subtasks: List<TaskDto> = emptyList(),
    val notes: List<TaskDto> = emptyList(),
    val sharedSender: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val task: TaskDto? = null
)
