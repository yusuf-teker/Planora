package com.yusufteker.pulse.feature.home.presentation.create_task

import com.yusufteker.pulse.shared.api.TaskType
import com.yusufteker.pulse.shared.api.TaskVisibility

sealed interface CreateTaskEvent : com.yusufteker.pulse.core.base.UiEvent {
    data class OnTitleChange(val title: String) : CreateTaskEvent
    data class OnDescriptionChange(val description: String) : CreateTaskEvent
    data class OnStartTimeChange(val time: Long) : CreateTaskEvent
    data class OnEndTimeChange(val time: Long) : CreateTaskEvent
    
    data class OnStartTimeUpdate(val hour: Int, val minute: Int) : CreateTaskEvent
    data class OnEndTimeUpdate(val hour: Int, val minute: Int) : CreateTaskEvent
    data class OnTaskTypeChange(val type: TaskType) : CreateTaskEvent
    data class OnVisibilityChange(val visibility: TaskVisibility) : CreateTaskEvent
    data class OnRoomToggle(val roomId: String, val isSelected: Boolean) : CreateTaskEvent
    
    data class OnAllDayToggle(val isAllDay: Boolean) : CreateTaskEvent
    data class OnRecurringToggle(val isRecurring: Boolean) : CreateTaskEvent
    data class OnDayOfWeekToggle(val day: Int) : CreateTaskEvent
    
    object LoadRooms : CreateTaskEvent
    object Submit : CreateTaskEvent
    object OnBackClick : CreateTaskEvent
    object OnClearState : CreateTaskEvent
}
