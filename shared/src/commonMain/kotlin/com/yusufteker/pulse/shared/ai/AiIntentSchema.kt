// shared/src/commonMain/kotlin/com/yusufteker/pulse/shared/ai/AiIntentSchema.kt
package com.yusufteker.pulse.shared.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Cloud, Android Local LLM ve iOS Local LLM'in ürettiği/beklediği ortak JSON şeması. */
@Serializable
data class LlmIntentJson(
    val type: String,          // "task" | "note" | "event"
    val title: String,
    val hasDeadline: Boolean,
    val deadline: String? = null, // "YYYY-MM-DD"
    val time: String? = null      // "HH:mm"
)

object AiIntentPrompt {
    /** Local LLM'lere (llama.cpp / MediaPipe) verilecek sistem talimatı + şema. */
    val SYSTEM_PROMPT = """
        Sen bir görev asistanısın. Kullanıcı mesajını analiz et:
        1. Eğer bir saat aralığı (örn: '8 9 arası') veya etkinlik (toplantı, ders) içeriyorsa 'event' olarak sınıflandır.
        2. Eğer yapılması/yetişilmesi gereken bir iş ise (örn: 'ödev yap', 'elma al') 'task' olarak sınıflandır.
        3. Zaman içermeyen genel metinleri 'note' olarak sınıflandır.
        BAŞLIK: Kullanıcının cümlesini kopyalama, maksimum 3 kelimeyle özetle.
        ZAMAN: Tarih/saat ifadesi varsa 'deadline' ve 'time' alanlarını doldur, 'hasDeadline'ı true yap. Yoksa false yap.
        SADECE aşağıdaki şemada JSON döndür, başka hiçbir açıklama/metin ekleme:
        {"type": "task|note|event", "title": "string", "hasDeadline": true|false, "deadline": "YYYY-MM-DD"|null, "time": "HH:mm"|null}
    """.trimIndent()

    private val json = Json { ignoreUnknownKeys = true }

    /** Model bazen ```json fence'i içinde döndürebiliyor, önce onu temizliyoruz. */
    fun parse(raw: String): LlmIntentJson? {
        val cleaned = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return try {
            json.decodeFromString<LlmIntentJson>(cleaned)
        } catch (e: Exception) {
            null
        }
    }
}