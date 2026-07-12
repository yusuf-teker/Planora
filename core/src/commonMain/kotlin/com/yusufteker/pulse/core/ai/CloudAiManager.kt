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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    // Kullanıcıya özel kısıtlama: 2 dakikada maksimum 5 mesaj
    private val requestTimestamps = mutableListOf<Long>()
    private val maxRequestsPerWindow = 5
    private val windowMs = 120_000L
    private val quotaMutex = Mutex()

    private suspend fun hasQuotaAvailable(): Boolean {
        return quotaMutex.withLock {
            val now = com.yusufteker.pulse.core.utils.getCurrentTimeMs()
            requestTimestamps.removeAll { now - it > windowMs }
            requestTimestamps.size < maxRequestsPerWindow
        }
    }

    private suspend fun recordRequest() {
        quotaMutex.withLock {
            requestTimestamps.add(com.yusufteker.pulse.core.utils.getCurrentTimeMs())
        }
    }

    private val systemPrompt: String
        get() {
            val now = Instant.fromEpochMilliseconds(com.yusufteker.pulse.core.utils.getCurrentTimeMs()).toLocalDateTime(TimeZone.currentSystemDefault())
            val tz = TimeZone.currentSystemDefault().id
            return """
        Sen Pulse adlı görev/not asistanısın. Yerel zaman: $now ($tz)
        KURALLAR:
        1. Saat aralığı/toplantı/ders içeriyorsa taskType=EVENT. Yapılması gereken eylem ise TASK. Zamansız genel not ise NOTE.
        2. EVENT ise dateTime ve (varsa) endDateTime doldur. TASK ise dateTime zorunlu (deadline).
        3. Zaman belirtilmemişse intent=CHAT yap, replyText'te ne zaman olduğunu sor.
        4. title: cümleyi kopyalama, max 2-3 kelime özet (örn. "Tenis Dersi").
        5. Tarih/saat "YYYY-MM-DDTHH:mm:ss" formatında, Z harfi OLMADAN, yerel saat olarak dön.
        6. Hatırlatıcı süreleri istenmişse 'reminders' dizisi içinde dakika cinsinden dön (örn 1 saat için 60, 1 gün için 1440).
        7. Tekrar eden bir işlemse 'recurrenceRule' içinde RRULE formatında dön (örn: FREQ=DAILY).
        8. Eğer bir mekan/konum belirtilmişse 'location' alanında dön.
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

            val history = context.recentMessages.takeLast(3).joinToString("\n")

            val fullInput = if (history.isNotEmpty()) {
                "Sohbet Geçmişi:\n$history\n\nYeni Kullanıcı Mesajı: ${input.take(500)}"
            } else {
                input.take(500)
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
                    put("maxOutputTokens", JsonPrimitive(1024)) // replyText'i de sınırlar
                    put("responseSchema", buildJsonObject {
                        put("type", JsonPrimitive("OBJECT"))
                        put("properties", buildJsonObject {
                            put("intent", buildJsonObject {
                                put("type", JsonPrimitive("STRING"))
                                put("enum", JsonArray(listOf(JsonPrimitive("CREATE_TASK"), JsonPrimitive("CHAT"))))
                            })
                            put("replyText", buildJsonObject { put("type", JsonPrimitive("STRING")) })
                            put("extractedEntities", buildJsonObject {
                                put("type", JsonPrimitive("OBJECT"))
                                put("properties", buildJsonObject {
                                    put("title", buildJsonObject { put("type", JsonPrimitive("STRING")) })
                                    put("description", buildJsonObject { put("type", JsonPrimitive("STRING")); put("nullable", JsonPrimitive(true)) })
                                    put("taskType", buildJsonObject {
                                        put("type", JsonPrimitive("STRING"))
                                        put("enum", JsonArray(listOf(JsonPrimitive("TASK"), JsonPrimitive("EVENT"), JsonPrimitive("NOTE"))))
                                    })
                                    put("dateTime", buildJsonObject { put("type", JsonPrimitive("STRING")); put("nullable", JsonPrimitive(true)) })
                                    put("endDateTime", buildJsonObject { put("type", JsonPrimitive("STRING")); put("nullable", JsonPrimitive(true)) })
                                    put("recurrenceRule", buildJsonObject { put("type", JsonPrimitive("STRING")); put("nullable", JsonPrimitive(true)) })
                                    put("reminders", buildJsonObject {
                                        put("type", JsonPrimitive("ARRAY"))
                                        put("items", buildJsonObject { put("type", JsonPrimitive("INTEGER")) })
                                        put("nullable", JsonPrimitive(true))
                                    })
                                    put("location", buildJsonObject { put("type", JsonPrimitive("STRING")); put("nullable", JsonPrimitive(true)) })
                                })
                                put("required", JsonArray(listOf(JsonPrimitive("title"), JsonPrimitive("taskType"))))
                            })
                        })
                        put("required", JsonArray(listOf(JsonPrimitive("intent"), JsonPrimitive("replyText"), JsonPrimitive("extractedEntities"))))
                    })
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

    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    private fun parseJsonResponse(jsonString: String, originalInput: String): AiChatResult {
        val json = Json {
            ignoreUnknownKeys = true
            allowTrailingComma = true
        }
        var fixedJsonString = jsonString.trim()
        
        // Remove markdown formatting if present
        if (fixedJsonString.startsWith("```json")) {
            fixedJsonString = fixedJsonString.removePrefix("```json").trim()
        } else if (fixedJsonString.startsWith("```")) {
            fixedJsonString = fixedJsonString.removePrefix("```").trim()
        }
        if (fixedJsonString.endsWith("```")) {
            fixedJsonString = fixedJsonString.removeSuffix("```").trim()
        }

        if (fixedJsonString.endsWith(",")) {
            fixedJsonString = fixedJsonString.removeSuffix(",")
        }
        val openBraces = fixedJsonString.count { it == '{' }
        val closeBraces = fixedJsonString.count { it == '}' }
        if (openBraces > closeBraces) {
            fixedJsonString += "}".repeat(openBraces - closeBraces)
        }
        val parsed = json.parseToJsonElement(fixedJsonString).jsonObject

        fun getString(jsonObj: JsonObject?, key: String): String? {
            val el = jsonObj?.get(key) ?: return null
            if (el is kotlinx.serialization.json.JsonNull) return null
            return el.jsonPrimitive.content
        }

        val replyText = getString(parsed, "replyText") ?: "Anlaşıldı."
        val entitiesJson = parsed["extractedEntities"]?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.jsonObject

        val title = getString(entitiesJson, "title") ?: originalInput.take(80)
        val description = getString(entitiesJson, "description")
        val intentStr = getString(parsed, "intent") ?: "CHAT"

        val taskTypeStr = getString(entitiesJson, "taskType")
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

        val dateStr = getString(entitiesJson, "dateTime")
        val endDateStr = getString(entitiesJson, "endDateTime")

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
        var endDateTimeMs = parseDate(endDateStr)

        if (taskType == TaskType.EVENT && endDateTimeMs == null && dateTimeMs != null) {
            endDateTimeMs = dateTimeMs + 3600_000L
        }

        val recurrenceRuleStr = getString(entitiesJson, "recurrenceRule")
        val recurrenceRuleVal = if (recurrenceRuleStr == "null" || recurrenceRuleStr.isNullOrEmpty()) null else recurrenceRuleStr

        val locationStr = getString(entitiesJson, "location")
        val locationVal = if (locationStr == "null" || locationStr.isNullOrEmpty()) null else locationStr

        val remindersElement = entitiesJson?.get("reminders")
        val remindersArray = if (remindersElement is JsonArray) remindersElement else null
        val parsedReminders = remindersArray?.mapNotNull { 
            if (it is JsonPrimitive && it !is kotlinx.serialization.json.JsonNull) it.content.toIntOrNull() else null 
        }
        val finalReminders = if (!parsedReminders.isNullOrEmpty()) {
            parsedReminders
        } else {
            listOf(60, 1440)
        }

        val entities = ExtractedEntities(
            title = title,
            description = if (description == "null") null else description,
            type = taskType,
            dateTime = dateTimeMs,
            endDateTime = endDateTimeMs,
            isAllDay = false,
            location = locationVal,
            participants = emptyList(),
            priority = TaskPriority.MEDIUM,
            recurrenceRule = recurrenceRuleVal,
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
                isRecurring = recurrenceRuleVal != null,
                recurrenceRule = recurrenceRuleVal,
                isFlexible = entities.dateTime == null,
                isOptional = true,
                isPostponable = true,
                isAllDay = false,
                aiMetadata = null,
                reminders = finalReminders,
                specificDetails = when (taskType) {
                    TaskType.EVENT -> com.yusufteker.pulse.shared.api.ItemDetails.Event(location = entities.location)
                    TaskType.TASK -> com.yusufteker.pulse.shared.api.ItemDetails.Task(deadline = entities.dateTime)
                    TaskType.NOTE -> com.yusufteker.pulse.shared.api.ItemDetails.Note()
                },
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

        val timeText = if (dateTimeMs != null && taskType != TaskType.NOTE) {
            val local = Instant.fromEpochMilliseconds(dateTimeMs).toLocalDateTime(TimeZone.currentSystemDefault())
            val date = "${local.dayOfMonth.toString().padStart(2, '0')}.${local.monthNumber.toString().padStart(2, '0')}.${local.year}"
            val time = "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
            " ($date $time)"
        } else ""

        val typeName = when (taskType) {
            TaskType.EVENT -> "etkinliği"
            TaskType.TASK -> "görevi"
            TaskType.NOTE -> "notu"
        }

        val finalReplyText = if (shouldCreate) {
            val successSuffix = "\n\n✅ $title $typeName$timeText başarıyla oluşturuldu."
            replyText + successSuffix
        } else {
            replyText
        }

        return AiChatResult(
            intent = intent,
            extractedEntities = entities,
            suggestedTaskRequest = taskRequest,
            shouldCreateTask = shouldCreate,
            aiMetadata = AiMetadata(
                sentiment = null,
                extractedActionItems = emptyList()
            ),
            replyText = finalReplyText
        )
    }
}