package com.yusufteker.pulse.feature.home.presentation.create_task

import com.yusufteker.pulse.shared.api.PlanRoomDto
import com.yusufteker.pulse.shared.api.TaskType
import com.yusufteker.pulse.shared.api.TaskVisibility

data class CreateTaskState(
    val title: String = "",
    val description: String = "",
    
    // Dates (nullable timestamps in milliseconds)
    val startTime: Long? = null,
    val endTime: Long? = null,
    
    val isAllDay: Boolean = false,
    val isRecurring: Boolean = false,
    val selectedDaysOfWeek: Set<Int> = emptySet(), // 1=Mon, 7=Sun
    
    
    val taskType: TaskType = TaskType.TASK,
    val visibility: TaskVisibility = TaskVisibility.PRIVATE,
    
    // Available rooms for sharing
    val availableRooms: List<PlanRoomDto> = emptyList(),
    // Set of selected room IDs
    val selectedRoomIds: Set<String> = emptySet(),
    
    val isLoading: Boolean = false,
    val error: String? = null
) : com.yusufteker.pulse.core.base.UiState
