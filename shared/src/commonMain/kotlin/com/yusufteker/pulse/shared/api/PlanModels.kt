package com.yusufteker.pulse.shared.api

import kotlinx.serialization.Serializable

@Serializable
enum class TaskType {
    NOTE,
    TASK,
    EVENT
}

@Serializable
enum class TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED
}

@Serializable
enum class TaskVisibility {
    PRIVATE,
    ROOM_SHARED
}

@Serializable
enum class RoomMemberStatus {
    PENDING,
    ACCEPTED,
    DECLINED
}

@Serializable
enum class RoomMemberRole {
    ADMIN,
    MEMBER
}

@Serializable
data class TaskDto(
    val id: String,
    val creatorId: Int,
    val title: String,
    val description: String?,
    val startTime: Long,
    val endTime: Long?,
    val type: TaskType,
    val status: TaskStatus,
    val visibility: TaskVisibility,
    val sharedRoomIds: List<String>,
    val isRecurring: Boolean = false,
    val recurrenceRule: String? = null,
    val isFlexible: Boolean = false,
    val isOptional: Boolean = false,
    val isPostponable: Boolean = true,
    val isAllDay: Boolean = false
)

@Serializable
data class PlanRoomDto(
    val id: String,
    val name: String,
    val creatorId: Int,
    val createdAt: Long,
    val members: List<PlanRoomMemberDto> = emptyList()
)

@Serializable
data class PlanRoomMemberDto(
    val roomId: String,
    val userId: Int,
    val status: RoomMemberStatus,
    val role: RoomMemberRole,
    val joinedAt: Long?
)

@Serializable
data class CreatePlanRoomRequest(
    val name: String
)

@Serializable
data class InviteUserRequest(
    val userId: Int
)

@Serializable
data class RespondToInviteRequest(
    val accept: Boolean
)

@Serializable
data class CreateTaskRequest(
    val title: String,
    val description: String? = null,
    val startTime: Long,
    val endTime: Long? = null,
    val type: TaskType,
    val status: TaskStatus = TaskStatus.PENDING,
    val visibility: TaskVisibility = TaskVisibility.PRIVATE,
    val sharedRoomIds: List<String> = emptyList(),
    val isRecurring: Boolean = false,
    val recurrenceRule: String? = null,
    val isFlexible: Boolean = false,
    val isOptional: Boolean = false,
    val isPostponable: Boolean = true,
    val isAllDay: Boolean = false
)
