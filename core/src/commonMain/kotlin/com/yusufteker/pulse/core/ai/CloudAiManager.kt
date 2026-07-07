package com.yusufteker.pulse.core.ai

import com.yusufteker.pulse.shared.ai.AiChatContext
import com.yusufteker.pulse.shared.ai.AiChatResult
import com.yusufteker.pulse.shared.ai.AiIntent
import com.yusufteker.pulse.shared.ai.ExtractedEntities
import com.yusufteker.pulse.shared.api.AiMetadata
import com.yusufteker.pulse.shared.api.TaskPriority
import com.yusufteker.pulse.shared.api.TaskType
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * ADIM 3 — Bulut tabanlı (Gemini API) AI Yöneticisi.
 *
 * AndroidAiManager ve IosAiManager tarafından, kendi ADIM 1 (Nano / Apple Intelligence)
 * ve ADIM 2'leri (local LLM indirme) sonuç vermediğinde çağrılır. Bu sınıf internet
 * bağlantısı olduğunda "gerçek" bir LLM deneyimi sunar; başarısız olursa (kota doldu,
 * internet yok, API hata verdi) null döner ki üst katman ADIM 4 (rule-based)'e düşsün.
 *
 * NOT (güvenlik): Şu an API anahtarı doğrudan kod içine gömülü. Bu bir demo/geliştirme
 * kolaylığı; production'a çıkarken bunu BuildConfig / local.properties / remote config
 * gibi bir yere taşımanı öneririm ki anahtar repo'ya sızmasın.
 */
class CloudAiManager(
    private val httpClient: HttpClient
) {
    private val apiKey = "AQ.Ab8RN6LKIQM4eTpJELREuE6re4VTL2nJt5WvSiCxvsHfptebpQ"
    private val model = "gemini-2.5-flash"
    private val apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

    // ── ADIM 3 Kota Koruması ──────────────────────────────────
    // Google AI Studio ücretsiz katman: dakikada 15 istek. Bu listeyi kullanarak
    // son 60 saniyedeki istek sayısını takip ediyoruz; limit dolduysa hiç denemeden
    // null dönüp ADIM 4'e (rule-based) düşüyoruz.
    private val requestTimestamps = mutableListOf<Long>()
    private val maxRequestsPerMinute = 15
    private val windowMs = 60_000L

    private fun hasQuotaAvailable(): Boolean {
        val now = com.yusufteker.pulse.core.utils.getCurrentTimeMs()
        requestTimestamps.removeAll { now - it > windowMs }
        return requestTimestamps.size < maxRequestsPerMinute
    }

    private fun recordRequest() {
        requestTimestamps.add(com.yusufteker.pulse.core.utils.getCurrentTimeMs())
    }

    private val systemPrompt: String
        get() {
            val now = Instant.fromEpochMilliseconds(com.yusufteker.pulse.core.utils.getCurrentTimeMs()).toLocalDateTime(TimeZone.currentSystemDefault())
            val tz = TimeZone.currentSystemDefault().id
            return """
        Sen Pulse adlı bir görev ve not asistanısın.
        Şu anki YEREL zaman: $now (Saat Dilimi: $tz)
        Kullanıcının girişini analiz et ve aşağıdaki JSON formatında kesin ve hatasız bir yanıt dön:
        {
          "intent": "CREATE_TASK" veya "CHAT",
          "replyText": "Kullanıcıya vereceğin samimi ve doğal yanıt",
          "extractedEntities": {
            "title": "Görev başlığı (örn: 'Tenis Dersi', 'Market Alışverişi'). Orijinal cümleyi KOPYALAMA, maksimum 3 kelimeyle özetle!",
            "description": "Detaylı açıklama (varsa)",
            "taskType": "TASK" veya "EVENT" veya "NOTE",
            "dateTime": "YYYY-MM-DDTHH:mm:ss" veya null,
            "endDateTime": "YYYY-MM-DDTHH:mm:ss" veya null
          }
        }
        KURALLAR:
        1. taskType BELİRLEME:
           - Bir saat aralığı (örn: "8 9 arası") veya etkinlik (toplantı, ders) içeriyorsa KESİNLİKLE "EVENT" olmalıdır.
           - Belirli bir şeye yetişilmesi gereken, yapılması gereken bir eylem ise (örn: "ödev bitirmem lazım", "elma al") "TASK" olmalıdır.
           - Zaman içermeyen genel notlar "NOTE" olmalıdır.
        2. ZAMAN:
           - "EVENT" ise, dateTime (başlangıç) ve endDateTime (bitiş) olmalıdır. (Yalnızca başlangıç varsa bitişi 1 saat sonrası yap).
           - "TASK" ise, dateTime (teslim tarihi/deadline) ZORUNLUDUR.
        3. EKSİK ZAMAN (Soru Sorma): Eğer kullanıcı "TASK" veya "EVENT" oluşturmak istiyor ama ZAMAN belirtmemişse (örneğin sadece "marketten elma almam lazım" dediyse), "intent": "CHAT" yap ve replyText ile "Bunu ne zaman yapacaksın/ne zamana hatırlatayım?" diye sor.
        4. BAŞLIK (title): Cümleyi başlık yapma. Sadece ana konuyu 2-3 kelimeyle yaz. Örn: "Yarın saat 8 9 arası tenis dersim var" -> "Tenis Dersi".
        5. TARİH/SAAT FORMATI: Yukarıda verdiğim yerel saate göre hesapla. Sonuçları doğrudan YEREL SAAT olarak "YYYY-MM-DDTHH:mm:ss" formatında (Z harfi OLMADAN) dön ki saat farkı oluşmasın.

        Cevabın sadece JSON formatında olmalı. Markdown kod bloğu KULLANMA.
    """.trimIndent()
        }

    /**
     * ADIM 3'ün giriş noktası. Kota doluysa veya API anahtarı yoksa hiç denemeden
     * null döner (üst katman hemen ADIM 4'e geçsin diye zaman kaybetmeyelim).
     */
    suspend fun processMessage(input: String, context: AiChatContext): AiChatResult? {
        if (apiKey.isEmpty() || apiKey == "YOUR_FREE_GEMINI_API_KEY_HERE") {
            return null
        }
        if (!hasQuotaAvailable()) {
            println("CloudApi: Dakikalık ücretsiz kota doldu, ADIM 4'e (rule-based) düşülüyor.")
            return null
        }

        try {
            recordRequest()

            val history = context.recentMessages.joinToString("\n")
            val fullInput = if (history.isNotEmpty()) {
                "Sohbet Geçmişi:\n$history\n\nYeni Kullanıcı Mesajı: $input"
            } else {
                input
            }

            val requestBody = buildJsonObject {
                put("contents", JsonArray(listOf(
                    buildJsonObject {
                        put("parts", JsonArray(listOf(
                            buildJsonObject { put("text", JsonPrimitive(fullInput)) }
                        )))
                    }
                )))
                put("systemInstruction", buildJsonObject {
                    put("parts", JsonArray(listOf(
                        buildJsonObject { put("text", JsonPrimitive(systemPrompt)) }
                    )))
                })
                put("generationConfig", buildJsonObject {
                    put("responseMimeType", JsonPrimitive("application/json"))
                })
            }

            val response = httpClient.post(apiUrl) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            val responseBody = response.body<JsonObject>()
            println("CloudApi: Raw Response = $responseBody")
            val candidates = responseBody["candidates"]?.jsonArray
            val textResponse = candidates?.get(0)?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("parts")?.jsonArray
                ?.get(0)?.jsonObject
                ?.get("text")?.jsonPrimitive?.content

            if (textResponse != null) {
                println("CloudApi: Extracted Text = $textResponse")
                return parseJsonResponse(textResponse, input)
            } else {
                println("CloudApi: textResponse is null!")
            }
        } catch (e: Exception) {
            println("CloudApi: Exception -> ${e.message}")
            e.printStackTrace()
            return null
        }
        return null
    }

    private fun parseJsonResponse(jsonString: String, originalInput: String): AiChatResult {
        val json = Json { ignoreUnknownKeys = true }
        val parsed = json.parseToJsonElement(jsonString).jsonObject

        val replyText = parsed["replyText"]?.jsonPrimitive?.content ?: "Anlaşıldı."
        val entitiesJson = parsed["extractedEntities"]?.jsonObject

        val title = entitiesJson?.get("title")?.jsonPrimitive?.content ?: originalInput.take(80)
        val description = entitiesJson?.get("description")?.jsonPrimitive?.content
        val intentStr = parsed["intent"]?.jsonPrimitive?.content ?: "CHAT"

        val taskTypeStr = entitiesJson?.get("taskType")?.jsonPrimitive?.content
        val taskType = when (taskTypeStr) {
            "EVENT" -> TaskType.EVENT
            "NOTE" -> TaskType.NOTE
            else -> TaskType.TASK
        }

        val intent = if (intentStr == "CREATE_TASK") {
            when (taskType) {
                TaskType.EVENT -> AiIntent.CREATE_EVENT
                TaskType.NOTE -> AiIntent.CREATE_NOTE
                TaskType.TASK -> AiIntent.CREATE_TASK
            }
        } else {
            AiIntent.CHAT
        }

        val shouldCreate = intent == AiIntent.CREATE_TASK || intent == AiIntent.CREATE_EVENT || intent == AiIntent.CREATE_NOTE

        val dateStr = entitiesJson?.get("dateTime")?.jsonPrimitive?.content
        val endDateStr = entitiesJson?.get("endDateTime")?.jsonPrimitive?.content

        fun parseDate(str: String?): Long? {
            if (str == null || str == "null" || str.isEmpty()) return null
            return try {
                if (str.endsWith("Z")) {
                    Instant.parse(str).toEpochMilliseconds()
                } else {
                    kotlinx.datetime.LocalDateTime.parse(str).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        val dateTimeMs = parseDate(dateStr)
        val endDateTimeMs = parseDate(endDateStr)

        val entities = ExtractedEntities(
            title = title,
            description = if (description == "null") null else description,
            type = taskType,
            dateTime = dateTimeMs,
            endDateTime = endDateTimeMs,
            isAllDay = false,
            location = null,
            participants = emptyList(),
            priority = TaskPriority.MEDIUM,
            recurrenceRule = null,
            tags = emptyList(),
            estimatedMinutes = null,
            confidence = 1.0f
        )

        val nowMs = com.yusufteker.pulse.core.utils.getCurrentTimeMs()
        val taskRequest = if (shouldCreate) {
            com.yusufteker.pulse.shared.api.CreateTaskRequest(
                title = entities.title,
                description = entities.description ?: originalInput.take(500),
                startTime = entities.dateTime ?: nowMs,
                endTime = entities.endDateTime,
                type = taskType,
                status = com.yusufteker.pulse.shared.api.TaskStatus.PENDING,
                visibility = com.yusufteker.pulse.shared.api.TaskVisibility.PRIVATE,
                isRecurring = false,
                recurrenceRule = null,
                isFlexible = entities.dateTime == null,
                isOptional = true,
                isPostponable = true,
                isAllDay = false,
                aiMetadata = null,
                reminders = emptyList(),
                specificDetails = null,
                participants = emptyMap(),
                color = when (taskType) {
                    TaskType.TASK -> "#4CAF50"
                    TaskType.EVENT -> "#2196F3"
                    else -> "#FF9800"
                },
                tags = emptyList(),
                isSynced = false
            )
        } else null

        return AiChatResult(
            intent = intent,
            extractedEntities = entities,
            suggestedTaskRequest = taskRequest,
            shouldCreateTask = shouldCreate,
            aiMetadata = AiMetadata(
                sentiment = null,
                extractedActionItems = emptyList()
            ),
            replyText = replyText
        )
    }
}