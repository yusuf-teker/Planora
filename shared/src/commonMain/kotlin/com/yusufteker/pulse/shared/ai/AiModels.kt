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

/**
 * AI motorunun cihazdaki kullanılabilirlik durumu.
 *
 * NOT: Bu artık "tek bir motorun" durumu değil, 4 kademeli fallback zincirinin
 * o an hangi kademede olduğunu UI'a bildiren genel bir durumdur:
 *
 * ADIM 1 (Nano / Apple Intelligence) → AVAILABLE
 * ADIM 2 (İndirilebilir Local LLM)   → PROMPT_DOWNLOAD / DOWNLOADING
 * ADIM 3 (Cloud Gemini API)          → UI'a ayrıca yansıtılmaz, sessizce dener
 * ADIM 4 (Rule-Based)                → BASIC_ONLY
 */
enum class AiAvailabilityState {
    /** Cihaz üstü LLM hazır (Gemini Nano / Apple Intelligence) */
    AVAILABLE,
    /** Sadece kural tabanlı NLP çalışıyor (ya da geçici olarak Cloud API deneniyor) */
    BASIC_ONLY,
    /** LLM modeli indiriliyor */
    DOWNLOADING,
    /** Kullanıcıya indirme sorusu sorulmalı */
    PROMPT_DOWNLOAD,
    /** Beklenmeyen hata */
    ERROR
}

/**
 * Kullanıcının "küçük local LLM'i indirmek ister misin?" teklifine verdiği cevap.
 * Bu karar, uygulama açık kaldığı sürece hafızada tutulur; her mesajda tekrar
 * sorulmaması için kullanılır. Kalıcı hale getirmek istersen (DataStore/UserDefaults
 * ile) bunu tekrar uygulama açılışında oku.
 */
enum class LocalLlmDecision {
    /** Henüz hiç sorulmadı */
    NOT_ASKED,
    /** Kullanıcı indirmeyi kabul etti */
    ACCEPTED,
    /** Kullanıcı indirmeyi reddetti — bir daha sorma */
    DECLINED
}