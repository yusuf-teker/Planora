package com.yusufteker.planora.shared.ai

import com.yusufteker.planora.shared.api.AiMetadata
import com.yusufteker.planora.shared.api.CreateTaskRequest
import com.yusufteker.planora.shared.api.TaskPriority
import com.yusufteker.planora.shared.api.TaskType

/**
 * Yapay zekanın kullanıcı mesajından çıkardığı niyet türü.
 */
enum class AiIntent {
    CREATE_TASK,
    CREATE_EVENT,
    CREATE_NOTE,
    QUERY,
    CHAT,
    UNKNOWN
}

/**
 * Yapay zekanın kullanıcı mesajından çıkardığı varlıklar (tarih, kişi, konum vb.).
 */
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
    // YENİ: Ortak oda ve katılımcı ID'leri
    val sharedRoomId: String? = null,
    val participantUserIds: List<Int> = emptyList()
)

/**
 * AI işlem sonucu — ViewModel'e dönen komple yanıt.
 */
data class AiChatResult(
    val intent: AiIntent,
    val replyText: String,
    val extractedEntities: ExtractedEntities? = null,
    val aiMetadata: AiMetadata = AiMetadata(),
    val shouldCreateTask: Boolean = false,
    val suggestedTaskRequest: CreateTaskRequest? = null,
    val needsClarification: Boolean = false // deadline gibi eksik bilgi sorulacaksa true
)

/**
 * AI'ın kullanıcı ve oturum hakkında ihtiyaç duyduğu bağlam.
 */
data class AiChatContext(
    val currentUserId: Int,
    val followedUsers: List<Pair<Int, String>> = emptyList(), // (userId, username)
    val recentMessages: List<String> = emptyList(),
    // YENİ: Ortak odalar (oda adı + üye isimleri)
    val sharedRooms: List<SharedRoomInfo> = emptyList(),
    // YENİ: Kullanıcının mevcut görevleri (çakışma tespiti için)
    val myTasks: List<MyTaskInfo> = emptyList(),
    // YENİ: Takvim erişimi olan kullanıcılar (userId, isim)
    val accessibleUsers: List<Pair<Int, String>> = emptyList()
)

/**
 * AI'ın ortak oda hakkında bilmesi gereken bilgiler.
 */
data class SharedRoomInfo(
    val roomId: String,
    val roomName: String,
    val memberNames: List<String> = emptyList() // Üye isimleri (kullanıcı adları)
)

/**
 * AI'ın mevcut görevler hakkında bilmesi gereken bilgiler.
 */
data class MyTaskInfo(
    val title: String,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val type: TaskType = TaskType.TASK
)

// File cleaned up