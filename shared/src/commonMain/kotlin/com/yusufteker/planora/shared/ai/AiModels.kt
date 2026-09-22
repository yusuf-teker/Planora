package com.yusufteker.planora.shared.ai

import com.yusufteker.planora.shared.api.AiMetadata
import com.yusufteker.planora.shared.api.CreateTaskRequest
import com.yusufteker.planora.shared.api.TaskPriority
import com.yusufteker.planora.shared.api.TaskType
import kotlinx.serialization.Serializable

/**
 * Yapay zekanın kullanıcı mesajından çıkardığı niyet türü.
 */
@Serializable
enum class AiIntent {
    CREATE_TASK,
    CREATE_EVENT,
    CREATE_NOTE,
    CREATE_PLAN_ROOM,
    INVITE_TO_ROOM,
    QUERY,
    CHAT,
    REJECTED,
    UNKNOWN
}

/**
 * Yapay zekanın kullanıcı mesajından çıkardığı varlıklar (tarih, kişi, konum, oda vb.).
 */
@Serializable
data class ExtractedEntities(
    val title: String,
    val description: String? = null,
    val type: TaskType = TaskType.TASK,
    val dateTime: Long? = null,
    val endDateTime: Long? = null,
    val isAllDay: Boolean = false,
    val location: String? = null,
    val participants: List<String> = emptyList(),
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val recurrenceRule: String? = null,
    val tags: List<String> = emptyList(),
    val checklist: List<String> = emptyList(),
    val estimatedMinutes: Int? = null,
    val confidence: Float = 0.5f,
    val sharedRoomId: String? = null,
    val participantUserIds: List<Int> = emptyList(),
    // Plan Odası ve Davet için alanlar
    val targetRoomName: String? = null,
    val targetUsername: String? = null,
    val targetUserId: Int? = null
)

/**
 * AI işlem sonucu — ViewModel'e ve istemciye dönen komple yanıt.
 */
@Serializable
data class AiChatResult(
    val intent: AiIntent,
    val replyText: String,
    val extractedEntities: ExtractedEntities? = null,
    val aiMetadata: AiMetadata = AiMetadata(),
    val shouldCreateTask: Boolean = false,
    val suggestedTaskRequest: CreateTaskRequest? = null,
    val needsClarification: Boolean = false,
    // Plan Odası ve Davet Eylemleri
    val shouldCreateRoom: Boolean = false,
    val roomNameToCreate: String? = null,
    val shouldInviteUser: Boolean = false,
    val inviteRoomId: String? = null,
    val inviteUserId: Int? = null
)

/**
 * AI'ın kullanıcı ve oturum hakkında ihtiyaç duyduğu bağlam.
 */
@Serializable
data class AiChatContext(
    val currentUserId: Int,
    val followedUsers: List<Pair<Int, String>> = emptyList(), // (userId, username)
    val recentMessages: List<String> = emptyList(),
    val sharedRooms: List<SharedRoomInfo> = emptyList(),
    val myTasks: List<MyTaskInfo> = emptyList(),
    val accessibleUsers: List<Pair<Int, String>> = emptyList()
)

/**
 * AI'ın ortak oda hakkında bilmesi gereken bilgiler.
 */
@Serializable
data class SharedRoomInfo(
    val roomId: String,
    val roomName: String,
    val memberNames: List<String> = emptyList()
)

/**
 * AI'ın mevcut görevler hakkında bilmesi gereken bilgiler.
 */
@Serializable
data class MyTaskInfo(
    val title: String,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val type: TaskType = TaskType.TASK
)

/**
 * Kullanıcının AI kullanım hakları / kota durumu.
 *
 * Normal (Free) Kullanıcı: Günde 1, Haftada 3
 * Premium Kullanıcı: Günde 50, Haftada 300
 */
@Serializable
data class AiQuotaDto(
    val isPremium: Boolean = false,
    val dailyRemaining: Int = 1,
    val dailyLimit: Int = 1,
    val weeklyRemaining: Int = 3,
    val weeklyLimit: Int = 3
)

/**
 * İstemciden Planora sunucusuna gönderilen AI sohbet isteği.
 */
@Serializable
data class AiChatServerRequest(
    val message: String,
    val context: AiChatContext
)

/**
 * Sunucudan dönen AI yanıtı ve güncel kota bilgisi.
 */
@Serializable
data class AiChatServerResponse(
    val result: AiChatResult? = null,
    val quota: AiQuotaDto,
    val quotaExceeded: Boolean = false,
    val errorMessage: String? = null
)