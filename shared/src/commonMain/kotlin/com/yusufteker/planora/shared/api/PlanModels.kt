package com.yusufteker.planora.shared.api

import kotlinx.serialization.SerialName
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
    @Serializable @SerialName("com.yusufteker.planora.shared.api.RecurrenceRule.Daily") data class Daily(val interval: Int = 1) : RecurrenceRule()
    @Serializable @SerialName("com.yusufteker.planora.shared.api.RecurrenceRule.Weekly") data class Weekly(val daysOfWeek: Set<Int>) : RecurrenceRule()
    @Serializable @SerialName("com.yusufteker.planora.shared.api.RecurrenceRule.Monthly") data class Monthly(val dayOfMonth: Int? = null, val isLastDay: Boolean = false) : RecurrenceRule()
    @Serializable @SerialName("com.yusufteker.planora.shared.api.RecurrenceRule.Yearly") data class Yearly(val month: Int, val dayOfMonth: Int) : RecurrenceRule()
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
    @SerialName("com.yusufteker.planora.shared.api.ItemDetails.Note")
    data class Note(
        val content: String = "",
        val attachments: List<String> = emptyList(),
        val checklist: List<SubTask> = emptyList()
    ) : ItemDetails()

    @Serializable
    @SerialName("com.yusufteker.planora.shared.api.ItemDetails.Task")
    data class Task(
        val priority: TaskPriority = TaskPriority.MEDIUM,
        val subtasks: List<SubTask> = emptyList(),
        val deadline: Long? = null,
        val estimatedMinutes: Int? = null
    ) : ItemDetails()

    @Serializable
    @SerialName("com.yusufteker.planora.shared.api.ItemDetails.Event")
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
    val isPinned: Boolean = false,
    val isSynced: Boolean = true
)

@Serializable
data class TaskParticipantDto(
    val userId: Int,
    val name: String,
    val avatarId: String? = null,
    val profileImageUrl: String? = null
)

@Serializable
data class PlanRoomDto(
    val id: String,
    val name: String,
    val creatorId: Int,
    val createdAt: Long,
    val imageUrl: String? = null,
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
    val isPinned: Boolean = false,
    val isSynced: Boolean = true,
    val localId: String? = null
)

@Serializable
data class AutoScheduleRequest(
    val taskIds: List<String>
)

fun String.extractBaseTaskId(): String {
    val suffix = this.substringAfterLast("_")
    return if (suffix.toLongOrNull() != null) {
        this.substringBeforeLast("_")
    } else {
        this
    }
}
