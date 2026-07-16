package com.yusufteker.pulse.shared.ai

import com.yusufteker.pulse.shared.api.AiMetadata
import com.yusufteker.pulse.shared.api.CreateTaskRequest
import com.yusufteker.pulse.shared.api.TaskPriority
import com.yusufteker.pulse.shared.api.TaskType

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
    val confidence: Float = 0.5f
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
    val recentMessages: List<String> = emptyList()
)

// File cleaned up