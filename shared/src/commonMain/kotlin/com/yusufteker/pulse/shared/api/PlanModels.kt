package com.yusufteker.pulse.shared.api

import kotlinx.serialization.Serializable

@Serializable
enum class TaskType {
    NOTE,
    TASK,
    EVENT,
    FOLDER
}

@Serializable
sealed class RecurrenceRule {
    @Serializable data class Daily(val interval: Int = 1) : RecurrenceRule()
    @Serializable data class Weekly(val daysOfWeek: Set<Int>) : RecurrenceRule()
    @Serializable data class Monthly(val dayOfMonth: Int? = null, val isLastDay: Boolean = false) : RecurrenceRule()
    @Serializable data class Yearly(val month: Int, val dayOfMonth: Int) : RecurrenceRule()
}

@Serializable
data class TaskException(
    val taskId: String,
    val dateMs: Long,
    val isCompleted: Boolean
)

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
enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}

@Serializable
enum class RsvpState {
    ATTENDING,
    MAYBE,
    DECLINED,
    PENDING
}

@Serializable
data class SubTask(
    val id: String,
    val title: String,
    val isDone: Boolean
)

@Serializable
data class AiMetadata(
    val summary: String? = null,
    val extractedActionItems: List<String> = emptyList(),
    val semanticEmbedding: List<Float> = emptyList(),
    val autoScheduledConfidence: Float? = null,
    val sentiment: String? = null
)

@Serializable
sealed class ItemDetails {
    @Serializable
    data class Note(
        val content: String = "",
        val attachments: List<String> = emptyList(),
        val checklist: List<SubTask> = emptyList()
    ) : ItemDetails()

    @Serializable
    data class Task(
        val priority: TaskPriority = TaskPriority.MEDIUM,
        val subtasks: List<SubTask> = emptyList(),
        val deadline: Long? = null,
        val estimatedMinutes: Int? = null
    ) : ItemDetails()

    @Serializable
    data class Event(
        val location: String? = null,
        val meetingUrl: String? = null,
        val rsvpStatus: Map<Int, RsvpState> = emptyMap()
    ) : ItemDetails()
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
    val isAllDay: Boolean = false,
    
    // YENİ EKLENEN ESNEK ALANLAR
    val parentId: String? = null,
    val aiMetadata: AiMetadata? = null,
    val reminders: List<Int> = emptyList(),
    val specificDetails: ItemDetails? = null,
    val tags: List<String> = emptyList(),
    val color: String? = null,
    val participants: List<TaskParticipantDto> = emptyList(),
    val isSynced: Boolean = true
)

@Serializable
data class TaskParticipantDto(
    val userId: Int,
    val name: String,
    val avatarId: String,
    val profileImageUrl: String? = null
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
data class RenamePlanRoomRequest(
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
    val isAllDay: Boolean = false,
    
    // YENİ EKLENEN ESNEK ALANLAR
    val parentId: String? = null,
    val aiMetadata: AiMetadata? = null,
    val reminders: List<Int> = emptyList(),
    val specificDetails: ItemDetails? = null,
    val tags: List<String> = emptyList(),
    val color: String? = null,
    val participants: Map<Int, String> = emptyMap(),
    val isSynced: Boolean = true,
    val localId: String? = null
)

@Serializable
data class AutoScheduleRequest(
    val taskIds: List<String>
)
