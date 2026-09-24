package com.yusufteker.planora.server.service

import com.yusufteker.planora.server.AppConfig
import com.yusufteker.planora.server.database.DatabaseFactory.dbQuery
import com.yusufteker.planora.server.database.tables.*
import com.yusufteker.planora.shared.ai.*
import com.yusufteker.planora.shared.api.CreateTaskRequest
import com.yusufteker.planora.shared.api.ItemDetails
import com.yusufteker.planora.shared.api.TaskStatus
import com.yusufteker.planora.shared.api.TaskType
import com.yusufteker.planora.shared.api.TaskVisibility
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.insert
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Planora Sunucu Yapay Zeka Servisi.
 *
 * Sorumlulukları:
 * 1. Kullanıcının günlük ve haftalık AI kullanım kotalarını kontrol etmek (Free: 1 gün / 3 hafta, Premium: 50 gün / 300 hafta).
 * 2. Ultra-kompakt prompt ve şema ile Google Gemini API'ye istek göndermek.
 * 3. Kapsam dışı istekleri filtrelemek (Guardrail).
 * 4. Başarılı AI çağrılarını ai_usage_logs tablosuna kaydetmek.
 */
object AiService {

    // ─────────────────────────────────────────────────────────────
    // AI KOTA LİMİTLERİ (KODDAN DEĞİŞTİRİLEBİLİR SABİTLER)
    // ─────────────────────────────────────────────────────────────
    const val AI_DAILY_LIMIT_FREE = 1
    const val AI_DAILY_LIMIT_PREMIUM = 50
    const val AI_WEEKLY_LIMIT_FREE = 3
    const val AI_WEEKLY_LIMIT_PREMIUM = 300

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Kullanıcının güncel kota durumunu hesaplar.
     */
    suspend fun getUserQuota(userId: Int): AiQuotaDto = dbQuery {
        val user = UserEntity.findById(userId) ?: return@dbQuery AiQuotaDto()
        val now = java.time.Instant.now()

        // Premium süresi dolmuş mu kontrol et
        val isPremium = if (user.isPremium) {
            val until = user.premiumUntil
            if (until != null && until.isBefore(now)) {
                user.isPremium = false
                user.premiumUntil = null
                false
            } else {
                true
            }
        } else {
            false
        }

        val dailyLimit = if (isPremium) AI_DAILY_LIMIT_PREMIUM else AI_DAILY_LIMIT_FREE
        val weeklyLimit = if (isPremium) AI_WEEKLY_LIMIT_PREMIUM else AI_WEEKLY_LIMIT_FREE

        val oneDayAgo = now.minus(24, ChronoUnit.HOURS)
        val oneWeekAgo = now.minus(7, ChronoUnit.DAYS)

        val dailyCount = AiUsageLogsTable
            .selectAll()
            .where { (AiUsageLogsTable.userId eq userId) and (AiUsageLogsTable.createdAt greaterEq oneDayAgo) }
            .count()
            .toInt()

        val weeklyCount = AiUsageLogsTable
            .selectAll()
            .where { (AiUsageLogsTable.userId eq userId) and (AiUsageLogsTable.createdAt greaterEq oneWeekAgo) }
            .count()
            .toInt()

        val dailyRemaining = (dailyLimit - dailyCount).coerceAtLeast(0)
        val weeklyRemaining = (weeklyLimit - weeklyCount).coerceAtLeast(0)

        AiQuotaDto(
            isPremium = isPremium,
            dailyRemaining = dailyRemaining,
            dailyLimit = dailyLimit,
            weeklyRemaining = weeklyRemaining,
            weeklyLimit = weeklyLimit
        )
    }

    /**
     * Kullanıcının AI kullanımını ai_usage_logs tablosuna kaydeder.
     */
    private suspend fun recordUsage(userId: Int, actionType: String) = dbQuery {
        AiUsageLogsTable.insert {
            it[AiUsageLogsTable.userId] = userId
            it[AiUsageLogsTable.actionType] = actionType
            it[AiUsageLogsTable.createdAt] = java.time.Instant.now()
        }
    }

    /**
     * AI sohbet mesajını işler.
     */
    suspend fun processChat(userId: Int, request: AiChatServerRequest): AiChatServerResponse {
        val quota = getUserQuota(userId)

        // Kota kontrolü
        if (quota.dailyRemaining <= 0 || quota.weeklyRemaining <= 0) {
            val message = if (quota.dailyRemaining <= 0) {
                if (quota.isPremium) {
                    "Günlük Premium AI limitinize (50/50) ulaştınız. Yarın tekrar deneyebilirsiniz."
                } else {
                    "Günlük ücretsiz AI hakkınız (1/1) doldu. Premium'a geçerek günde 50 hakka sahip olabilirsiniz."
                }
            } else {
                if (quota.isPremium) {
                    "Haftalık Premium AI limitinize (300/300) ulaştınız."
                } else {
                    "Haftalık ücretsiz AI limitinize (3/3) ulaştınız. Premium'a geçerek haftada 300 hakka sahip olabilirsiniz."
                }
            }
            return AiChatServerResponse(
                result = null,
                quota = quota,
                quotaExceeded = true,
                errorMessage = message
            )
        }

        val apiKey = AppConfig.geminiApiKey
        if (apiKey.isBlank()) {
            return AiChatServerResponse(
                result = null,
                quota = quota,
                quotaExceeded = false,
                errorMessage = "Sunucuda Gemini API anahtarı yapılandırılmamış."
            )
        }

        // Kompakt prompt hazırla
        val systemPrompt = buildSystemPrompt(request.context)
        val fullInput = request.message.trim().take(400)

        val requestBody = buildGeminiRequestBody(systemPrompt, fullInput)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        return try {
            val httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build()

            val httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())

            if (httpResponse.statusCode() in 200..299) {
                val parsedResult = parseGeminiResponse(httpResponse.body(), request.context, fullInput)
                // Başarılı kullanım kaydet
                recordUsage(userId, parsedResult.intent.name)

                // Güncellenmiş kotayı hesapla
                val updatedQuota = quota.copy(
                    dailyRemaining = (quota.dailyRemaining - 1).coerceAtLeast(0),
                    weeklyRemaining = (quota.weeklyRemaining - 1).coerceAtLeast(0)
                )

                AiChatServerResponse(
                    result = parsedResult,
                    quota = updatedQuota,
                    quotaExceeded = false
                )
            } else {
                println("AiService Gemini Error HTTP ${httpResponse.statusCode()}: ${httpResponse.body()}")
                AiChatServerResponse(
                    result = null,
                    quota = quota,
                    quotaExceeded = false,
                    errorMessage = "Yapay zeka servisi geçici olarak yanıt veremedi."
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            AiChatServerResponse(
                result = null,
                quota = quota,
                quotaExceeded = false,
                errorMessage = "Bağlantı hatası: ${e.message}"
            )
        }
    }

    /**
     * Ultra-kompakt sistem talimatı.
     */
    private fun buildSystemPrompt(context: AiChatContext): String {
        val now = java.time.LocalDateTime.now()

        val roomsStr = if (context.sharedRooms.isNotEmpty()) {
            context.sharedRooms.joinToString(", ") { "${it.roomId}:${it.roomName}" }
        } else "Yok"

        val usersStr = if (context.followedUsers.isNotEmpty() || context.accessibleUsers.isNotEmpty()) {
            (context.followedUsers + context.accessibleUsers).distinctBy { it.first }
                .joinToString(", ") { "${it.first}:${it.second}" }
        } else "Yok"

        return """
        Sen Planora kişisel planlama asistanısın. SADECE JSON formatında yanıt ver.
        Zaman: $now
        Kullanıcı Odaları: [$roomsStr]
        Kullanıcılar: [$usersStr]

        KURALLAR:
        1. KISITLAMA (GUARDRAIL): Yalnızca Planora içinde görev, etkinlik, plan odası, üye daveti ve not işlemleri yapabilirsin. Alakasız tüm genel sohbet, fıkra, kodlama, hava durumu vb. soruları KESİNLİKLE reddet (intent="REJECTED", replyText="Ben sadece Planora asistanıyım; görev, etkinlik, plan odası ve notlarınızı düzenlemenize yardımcı olabilirim.").
        2. INTENTLER:
           - CREATE_TASK: Yapılacak veya tamamlanmış iş / to-do (tarihli veya tarihsiz). Kullanıcı "şunu yaptım", "spora gittim", "faturayı ödedim" gibi geçmiş zaman bildirse bile bunu bir görev (veya etkinlik) olarak algıla ve ekle.
           - CREATE_EVENT: Saat aralığı, toplantı, maç, ders veya randevu. Geçmişte gerçekleşmiş veya gelecekte planlanan etkinlikler.
           - CREATE_NOTE: Zamansız not / fikir / anımsatma.
           - CREATE_PLAN_ROOM: Yeni plan odası oluşturma (örn: "X adında oda aç").
           - INVITE_TO_ROOM: Odaya üye davet etme (örn: "Ahmet'i X odasına ekle").
           - REJECTED: Kapsam dışı istekler.
        3. GEÇMİŞ ZAMAN & TAMAMLANMA (isCompleted):
           - Kullanıcı eylemi zaten yaptığını/bitirdiğini söylüyorsa (örn: "bugün 3'te spora gittim", "faturayı ödedim", "Ahmet'le görüştüm"), 'isCompleted' değerini true yap. Gelecek veya henüz yapılmamışsa false yap.
        4. TARİH/SAAT: "YYYY-MM-DDTHH:mm:ss" yerel formatta dön. Geçmiş zaman ifadeleri ("dün", "sabah", "2 saat önce") için geçmiş tarihi hesapla. Saat aralığı varsa dateTime ve endDateTime doldur.
        5. ODA & KATILIMCILAR:
           - Odada etkinlik paylaşılacaksa: sharedRoomId ve participantUserIds doldur.
           - Odaya üye davet edilecekse: roomName ve targetUsername doldur.
           - Yeni oda açılacaksa: roomName doldur.
        6. BAŞLIK (title): 2-3 kelimelik kısa ve net özet (örn: "Spor Seansı", "Fatura Ödeme").
        """.trimIndent()
    }

    private fun buildGeminiRequestBody(systemPrompt: String, userInput: String): String {
        val root = buildJsonObject {
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("parts", buildJsonArray {
                        add(buildJsonObject { put("text", JsonPrimitive(userInput)) })
                    })
                })
            })
            put("systemInstruction", buildJsonObject {
                put("parts", buildJsonArray {
                    add(buildJsonObject { put("text", JsonPrimitive(systemPrompt)) })
                })
            })
            put("generationConfig", buildJsonObject {
                put("responseMimeType", JsonPrimitive("application/json"))
                put("maxOutputTokens", JsonPrimitive(450))
                put("responseSchema", buildJsonObject {
                    put("type", JsonPrimitive("OBJECT"))
                    put("properties", buildJsonObject {
                        put("intent", buildJsonObject {
                            put("type", JsonPrimitive("STRING"))
                            put("enum", buildJsonArray {
                                add(JsonPrimitive("CREATE_TASK"))
                                add(JsonPrimitive("CREATE_EVENT"))
                                add(JsonPrimitive("CREATE_NOTE"))
                                add(JsonPrimitive("CREATE_PLAN_ROOM"))
                                add(JsonPrimitive("INVITE_TO_ROOM"))
                                add(JsonPrimitive("REJECTED"))
                            })
                        })
                        put("replyText", buildJsonObject { put("type", JsonPrimitive("STRING")) })
                        put("title", buildJsonObject { put("type", JsonPrimitive("STRING")); put("nullable", JsonPrimitive(true)) })
                        put("description", buildJsonObject { put("type", JsonPrimitive("STRING")); put("nullable", JsonPrimitive(true)) })
                        put("dateTime", buildJsonObject { put("type", JsonPrimitive("STRING")); put("nullable", JsonPrimitive(true)) })
                        put("endDateTime", buildJsonObject { put("type", JsonPrimitive("STRING")); put("nullable", JsonPrimitive(true)) })
                        put("location", buildJsonObject { put("type", JsonPrimitive("STRING")); put("nullable", JsonPrimitive(true)) })
                        put("roomName", buildJsonObject { put("type", JsonPrimitive("STRING")); put("nullable", JsonPrimitive(true)) })
                        put("targetUsername", buildJsonObject { put("type", JsonPrimitive("STRING")); put("nullable", JsonPrimitive(true)) })
                        put("sharedRoomId", buildJsonObject { put("type", JsonPrimitive("STRING")); put("nullable", JsonPrimitive(true)) })
                        put("isCompleted", buildJsonObject { put("type", JsonPrimitive("BOOLEAN")); put("nullable", JsonPrimitive(true)) })
                        put("participantUserIds", buildJsonObject {
                            put("type", JsonPrimitive("ARRAY"))
                            put("items", buildJsonObject { put("type", JsonPrimitive("INTEGER")) })
                            put("nullable", JsonPrimitive(true))
                        })
                        put("reminders", buildJsonObject {
                            put("type", JsonPrimitive("ARRAY"))
                            put("items", buildJsonObject { put("type", JsonPrimitive("INTEGER")) })
                            put("nullable", JsonPrimitive(true))
                        })
                    })
                    put("required", buildJsonArray {
                        add(JsonPrimitive("intent"))
                        add(JsonPrimitive("replyText"))
                    })
                })
            })
        }
        return root.toString()
    }

    private fun parseGeminiResponse(rawResponseBody: String, context: AiChatContext, userInput: String): AiChatResult {
        val rootJson = json.parseToJsonElement(rawResponseBody).jsonObject
        val textPart = rootJson["candidates"]?.jsonArray?.getOrNull(0)?.jsonObject
            ?.get("content")?.jsonObject
            ?.get("parts")?.jsonArray?.getOrNull(0)?.jsonObject
            ?.get("text")?.jsonPrimitive?.content
            ?: ""

        var fixed = textPart.trim()
        if (fixed.startsWith("```json")) fixed = fixed.removePrefix("```json").trim()
        if (fixed.startsWith("```")) fixed = fixed.removePrefix("```").trim()
        if (fixed.endsWith("```")) fixed = fixed.removeSuffix("```").trim()

        val parsed = try {
            json.parseToJsonElement(fixed).jsonObject
        } catch (e: Exception) {
            JsonObject(emptyMap())
        }

        fun str(key: String): String? = parsed[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() && it != "null" }

        val intentStr = str("intent") ?: "REJECTED"
        val replyText = str("replyText") ?: "İşlem tamamlandı."
        val title = str("title") ?: userInput.take(50)
        val description = str("description")
        val location = str("location")
        val roomName = str("roomName")
        val targetUsername = str("targetUsername")
        val sharedRoomId = str("sharedRoomId")
        val isCompleted = parsed["isCompleted"]?.jsonPrimitive?.booleanOrNull ?: false

        val participantIds = parsed["participantUserIds"]?.jsonArray?.mapNotNull {
            it.jsonPrimitive.intOrNull
        } ?: emptyList()

        val reminders = parsed["reminders"]?.jsonArray?.mapNotNull {
            it.jsonPrimitive.intOrNull
        } ?: listOf(60)

        fun parseDate(dateStr: String?): Long? {
            if (dateStr.isNullOrBlank()) return null
            return try {
                if (dateStr.endsWith("Z")) {
                    Instant.parse(dateStr).toEpochMilli()
                } else {
                    LocalDateTime.parse(dateStr)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                }
            } catch (e: Exception) {
                null
            }
        }

        val startMs = parseDate(str("dateTime"))
        var endMs = parseDate(str("endDateTime"))

        if (intentStr == "CREATE_EVENT" && startMs != null && endMs == null) {
            endMs = startMs + 3600_000L // 1 saat sonra
        }

        val nowMs = System.currentTimeMillis()
        val taskStatus = if (isCompleted) TaskStatus.COMPLETED else TaskStatus.PENDING

        return when (intentStr) {
            "CREATE_TASK" -> {
                val req = CreateTaskRequest(
                    title = title,
                    description = description,
                    startTime = startMs ?: nowMs,
                    endTime = null,
                    type = TaskType.TASK,
                    status = taskStatus,
                    visibility = TaskVisibility.PRIVATE,
                    reminders = reminders,
                    specificDetails = ItemDetails.Task(deadline = startMs),
                    color = "#4CAF50"
                )
                AiChatResult(
                    intent = AiIntent.CREATE_TASK,
                    replyText = replyText,
                    shouldCreateTask = true,
                    suggestedTaskRequest = req,
                    extractedEntities = ExtractedEntities(
                        title = title,
                        description = description,
                        type = TaskType.TASK,
                        dateTime = startMs,
                        tags = emptyList()
                    )
                )
            }
            "CREATE_EVENT" -> {
                val isShared = !sharedRoomId.isNullOrBlank()
                val req = CreateTaskRequest(
                    title = title,
                    description = description,
                    startTime = startMs ?: nowMs,
                    endTime = endMs ?: (nowMs + 3600_000L),
                    type = TaskType.EVENT,
                    status = taskStatus,
                    visibility = if (isShared) TaskVisibility.ROOM_SHARED else TaskVisibility.PRIVATE,
                    sharedRoomIds = if (isShared) listOf(sharedRoomId) else emptyList(),
                    reminders = reminders,
                    specificDetails = ItemDetails.Event(location = location),
                    color = "#2196F3"
                )
                AiChatResult(
                    intent = AiIntent.CREATE_EVENT,
                    replyText = replyText,
                    shouldCreateTask = true,
                    suggestedTaskRequest = req,
                    extractedEntities = ExtractedEntities(
                        title = title,
                        description = description,
                        type = TaskType.EVENT,
                        dateTime = startMs,
                        endDateTime = endMs,
                        location = location,
                        sharedRoomId = sharedRoomId,
                        participantUserIds = participantIds
                    )
                )
            }
            "CREATE_NOTE" -> {
                val req = CreateTaskRequest(
                    title = title,
                    description = description,
                    startTime = nowMs,
                    type = TaskType.NOTE,
                    status = TaskStatus.COMPLETED,
                    visibility = TaskVisibility.PRIVATE,
                    specificDetails = ItemDetails.Note(content = description ?: ""),
                    color = "#FF9800"
                )
                AiChatResult(
                    intent = AiIntent.CREATE_NOTE,
                    replyText = replyText,
                    shouldCreateTask = true,
                    suggestedTaskRequest = req,
                    extractedEntities = ExtractedEntities(
                        title = title,
                        description = description,
                        type = TaskType.NOTE
                    )
                )
            }
            "CREATE_PLAN_ROOM" -> {
                val finalRoomName = roomName ?: title
                AiChatResult(
                    intent = AiIntent.CREATE_PLAN_ROOM,
                    replyText = replyText,
                    shouldCreateRoom = true,
                    roomNameToCreate = finalRoomName,
                    extractedEntities = ExtractedEntities(
                        title = finalRoomName,
                        targetRoomName = finalRoomName
                    )
                )
            }
            "INVITE_TO_ROOM" -> {
                // Hedef odayı context'ten bul
                val targetRoom = if (!roomName.isNullOrBlank()) {
                    context.sharedRooms.find { it.roomName.contains(roomName, ignoreCase = true) }
                } else null

                // Hedef kullanıcıyı context'ten bul
                val targetUser = if (!targetUsername.isNullOrBlank()) {
                    (context.followedUsers + context.accessibleUsers).find {
                        it.second.contains(targetUsername, ignoreCase = true)
                    }
                } else null

                AiChatResult(
                    intent = AiIntent.INVITE_TO_ROOM,
                    replyText = replyText,
                    shouldInviteUser = targetRoom != null && targetUser != null,
                    inviteRoomId = targetRoom?.roomId,
                    inviteUserId = targetUser?.first,
                    extractedEntities = ExtractedEntities(
                        title = "Odaya Davet",
                        targetRoomName = roomName,
                        targetUsername = targetUsername,
                        targetUserId = targetUser?.first
                    )
                )
            }
            else -> {
                AiChatResult(
                    intent = AiIntent.REJECTED,
                    replyText = replyText,
                    shouldCreateTask = false
                )
            }
        }
    }
}
