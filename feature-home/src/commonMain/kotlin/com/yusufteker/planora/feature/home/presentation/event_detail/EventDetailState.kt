package com.yusufteker.planora.feature.home.presentation.event_detail

import com.yusufteker.planora.shared.api.RecurrenceRule
import com.yusufteker.planora.shared.api.TaskDto
import com.yusufteker.planora.shared.api.TaskParticipantDto
import com.yusufteker.planora.shared.api.UserProfileResponse

data class EventDetailState(
    val eventId: String? = null,
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val startDateTimeMs: Long = 0L,
    val endDateTimeMs: Long = 0L,
    val isRecurring: Boolean = false,
    val recurrenceRule: RecurrenceRule? = null,
    val reminders: List<Int> = emptyList(),
    val planRoomId: String? = null,
    val planRoomName: String? = null,
    val planRoomImageUrl: String? = null,
    val participants: List<TaskParticipantDto> = emptyList(),
    val roomMembers: List<UserProfileResponse> = emptyList(),
    val subItems: List<TaskDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val event: TaskDto? = null
)
