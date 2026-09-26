package com.yusufteker.planora.server.service

import com.yusufteker.planora.server.AppConfig
import com.yusufteker.planora.server.database.DatabaseFactory.dbQuery
import com.yusufteker.planora.server.database.tables.*
import com.yusufteker.planora.shared.ai.*
import com.yusufteker.planora.shared.api.CreateTaskRequest
import com.yusufteker.planora.shared.api.ItemDetails
import com.yusufteker.planora.shared.api.RoomMemberStatus
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

        // Kullanıcının üye olduğu odaları ve o odalardaki tüm üyeleri veritabanından dinamik olarak al
        val enrichedRooms = try {
            dbQuery {
                val userRoomIds = PlanRoomMembersTable.selectAll()
                    .where { (PlanRoomMembersTable.userId eq userId) and (PlanRoomMembersTable.status eq RoomMemberStatus.ACCEPTED) }
                    .map { it[PlanRoomMembersTable.roomId] }

                userRoomIds.mapNotNull { rId ->
                    val roomRow = PlanRoomsTable.selectAll().where { PlanRoomsTable.id eq rId }.firstOrNull() ?: return@mapNotNull null
                    val roomName = roomRow[PlanRoomsTable.name]

                    val memberUserIds = PlanRoomMembersTable.selectAll()
                        .where { (PlanRoomMembersTable.roomId eq rId) and (PlanRoomMembersTable.status eq RoomMemberStatus.ACCEPTED) }
                        .map { it[PlanRoomMembersTable.userId] }

                    val memberUsers = UsersTable.selectAll().where { UsersTable.id inList memberUserIds }
                        .map { row ->
                            val name = row[UsersTable.name]
                            val username = row[UsersTable.username]
                            val id = row[UsersTable.id].value
                            val displayName = if (name.isNotBlank() && name != username) "$name (@$username)" else "@$username"
                            "$displayName (id:$id)"
                        }

                    SharedRoomInfo(
                        roomId = rId,
                        roomName = roomName,
                        memberNames = memberUsers
                    )
                }
            }
        } catch (e: Exception) {
            emptyList()
        }

        val effectiveContext = if (enrichedRooms.isNotEmpty()) {
            request.context.copy(sharedRooms = enrichedRooms)
        } else {
            request.context
        }

        // Kompakt prompt hazırla
        val systemPrompt = buildSystemPrompt(effectiveContext)
        val fullInput = request.message.trim().take(400)

        val requestBody = buildGeminiRequestBody(systemPrompt, fullInput)
        val candidateModels = listOf(
            "gemini-2.5-flash",
            "gemini-2.0-flash",
            "gemini-1.5-flash"
        )

        var lastError: String? = null

        for (model in candidateModels) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            try {
                val httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build()

                val httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
                println("[AiService] Gemini ($model) HTTP ${httpResponse.statusCode()} received.")

                if (httpResponse.statusCode() in 200..299) {
                    println("[AiService] Gemini ($model) Raw Body: ${httpResponse.body().take(500)}...")
                    val parsedResult = parseGeminiResponse(httpResponse.body(), effectiveContext, fullInput)
                    println("[AiService] Gemini ($model) Parsed Successfully: intent=${parsedResult.intent}, replyText=${parsedResult.replyText}")
                    // Başarılı kullanım kaydet
                    recordUsage(userId, parsedResult.intent.name)

                    // Güncellenmiş kotayı hesapla
                    val updatedQuota = quota.copy(
                        dailyRemaining = (quota.dailyRemaining - 1).coerceAtLeast(0),
                        weeklyRemaining = (quota.weeklyRemaining - 1).coerceAtLeast(0)
                    )

                    return AiChatServerResponse(
                        result = parsedResult,
                        quota = updatedQuota,
                        quotaExceeded = false
                    )
                } else {
                    val errorBody = httpResponse.body()
                    println("[AiService] Gemini ($model) Error HTTP ${httpResponse.statusCode()}: $errorBody")
                    lastError = "Model $model (${httpResponse.statusCode()}): $errorBody"
                    // 503 (High demand), 429 (Rate limit) veya 5xx sunucu hatalarında diğer modele geç
                    if (httpResponse.statusCode() == 503 || httpResponse.statusCode() == 429 || httpResponse.statusCode() >= 500) {
                        println("[AiService] Gemini ($model) aşırı yoğun veya kullanılamıyor, sıradaki yedek modele geçiliyor...")
                        continue
                    } else {
                        continue
                    }
                }
            } catch (e: Exception) {
                println("[AiService] Exception during Gemini ($model) processing: ${e.message}")
                lastError = e.message
                continue
            }
        }

        return AiChatServerResponse(
            result = null,
            quota = quota,
            quotaExceeded = false,
            errorMessage = "Yapay zeka modelleri yoğunluk nedeniyle geçici olarak yanıt veremedi. ($lastError)"
        )
    }

    private fun getUserZoneId(context: AiChatContext): ZoneId {
        return try {
            if (!context.timeZoneId.isNullOrBlank()) {
                ZoneId.of(context.timeZoneId)
            } else {
                ZoneId.of("Europe/Istanbul")
            }
        } catch (e: Exception) {
            ZoneId.of("Europe/Istanbul")
        }
    }

    /**
     * Ultra-kompakt sistem talimatı.
     */
    private fun buildSystemPrompt(context: AiChatContext): String {
        val userZone = getUserZoneId(context)
        val now = java.time.ZonedDateTime.now(userZone).toLocalDateTime()

        val roomsStr = if (context.sharedRooms.isNotEmpty()) {
            context.sharedRooms.joinToString("\n") { room ->
                val members = if (room.memberNames.isNotEmpty()) room.memberNames.joinToString(", ") else "bilinmiyor"
                "- Oda: '${room.roomName}' (id: ${room.roomId}), Üyeler: $members"
            }
        } else "Yok"

        val usersStr = if (context.followedUsers.isNotEmpty() || context.accessibleUsers.isNotEmpty()) {
            (context.followedUsers + context.accessibleUsers).distinctBy { it.first }
                .joinToString(", ") { "${it.second} (id:${it.first})" }
        } else "Yok"

        return """
        Sen Planora kişisel planlama asistanısın. SADECE JSON formatında yanıt ver.
        Zaman: $now
        Kullanıcı ID: ${context.currentUserId}

        ORTAK ODALAR VE ÜYELERİ:
        $roomsStr

        ERİŞİLEBİLİR KULLANICILAR:
        $usersStr

        KURALLAR:
        1. KISITLAMA (GUARDRAIL): Yalnızca Planora içinde görev, etkinlik, plan odası, üye daveti ve not işlemleri yapabilirsin. Alakasız tüm genel sohbet, fıkra, kodlama vb. soruları KESİNLİKLE reddet (intent="REJECTED", replyText="Ben sadece Planora asistanıyım; görev, etkinlik, plan odası ve notlarınızı düzenlemenize yardımcı olabilirim.").
        2. KİŞİLER VE ORTAK PLANLAR (ÇOK ÖNEMLİ):
           - Kullanıcı bir kişiyle (örn: "Dilberle...", "Ahmet ile buluşma", "Ayşe ile saat 5'e ekle") veya bir odada bir şey yapacağını, buluşacağını söylediğinde:
             a) Bu kişinin hangi ortak odada olduğunu ORTAK ODALAR listesinden ara (Türkçe ekleri ve büyük/küçük harfleri esnek algıla: örn. "Dilberle", "Dilber'le" -> "Dilber").
             b) Bulunan odanın id'sini 'sharedRoomId' alanına yaz.
             c) O kişinin userId'sini 'participantUserIds' listesine ekle.
             d) intent olarak CREATE_EVENT (saat/zaman varsa) veya CREATE_TASK seç. KESİNLİKLE INVITE_TO_ROOM yapma! (INVITE_TO_ROOM sadece açıkça "X kişisini odaya davet et / odaya ekle" dendiğinde kullanılır).
        3. INTENTLER:
           - CREATE_EVENT: Saat aralığı, belirli bir saatteki buluşma, ders, randevu veya etkinlikler (örn: "dilberle akşam 5e ekle" -> 17:00 CREATE_EVENT).
           - CREATE_TASK: Yapılacak veya tamamlanmış iş / to-do (tarihli veya tarihsiz).
           - CREATE_NOTE: Zamansız genel not / fikir / anımsatma.
           - CREATE_PLAN_ROOM: Yeni plan odası oluşturma (örn: "X adında oda aç").
           - INVITE_TO_ROOM: Mevcut bir odaya yeni üye davet etme (örn: "Ahmet'i X odasına davet et").
           - REJECTED: Kapsam dışı istekler.
        4. YAZIM HATALARI VE TÜRKÇE ESNEKLİĞİ:
           - Kullanıcının yazım hatalarını ("akam" -> "akşam", "dilberle" -> "Dilber ile", "yarın aksam 5e" -> 17:00) hoşgör ve doğru zamanı anla.
        5. GEÇMİŞ ZAMAN & TAMAMLANMA (isCompleted):
           - Kullanıcı eylemi zaten yaptığını/bitirdiğini söylüyorsa 'isCompleted' true yap. Gelecek veya henüz yapılmamışsa false yap.
        6. TARİH/SAAT: "YYYY-MM-DDTHH:mm:ss" yerel formatta dön. Saat aralığı yoksa başlangıçtan 1 saat sonrasını endDateTime yap.
        7. BAŞLIK (title): 2-4 kelimelik şık özet (örn: "Dilber ile Buluşma", "Tenis Dersi").
        8. YANIT METNİ (replyText): Yapılan işlemi kullanıcıya samimi ve net bildir (örn: "Dilber ile etkinliği bugün saat 17:00'ye ortak odaya ekledim.").
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
        val rootJson = try {
            json.parseToJsonElement(rawResponseBody) as? JsonObject ?: JsonObject(emptyMap())
        } catch (e: Exception) {
            JsonObject(emptyMap())
        }
        val candidates = (rootJson["candidates"] as? JsonArray)?.getOrNull(0) as? JsonObject
        val content = candidates?.get("content") as? JsonObject
        val parts = content?.get("parts") as? JsonArray
        val textPart = ((parts?.getOrNull(0) as? JsonObject)?.get("text") as? JsonPrimitive)?.contentOrNull
            ?: ""

        var fixed = textPart.trim()
        if (fixed.startsWith("```json")) fixed = fixed.removePrefix("```json").trim()
        if (fixed.startsWith("```")) fixed = fixed.removePrefix("```").trim()
        if (fixed.endsWith("```")) fixed = fixed.removeSuffix("```").trim()

        val parsed = try {
            json.parseToJsonElement(fixed) as? JsonObject ?: JsonObject(emptyMap())
        } catch (e: Exception) {
            JsonObject(emptyMap())
        }

        fun str(key: String): String? = (parsed[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() && it != "null" }

        val intentStr = str("intent") ?: "REJECTED"
        val replyText = str("replyText") ?: "İşlem tamamlandı."
        val title = str("title") ?: userInput.take(50)
        val description = str("description")
        val location = str("location")
        val roomName = str("roomName")
        val targetUsername = str("targetUsername")
        val sharedRoomId = str("sharedRoomId")
        val isCompleted = (parsed["isCompleted"] as? JsonPrimitive)?.booleanOrNull ?: false

        val participantIds = (parsed["participantUserIds"] as? JsonArray)?.mapNotNull {
            (it as? JsonPrimitive)?.intOrNull
        } ?: emptyList()

        val reminders = (parsed["reminders"] as? JsonArray)?.mapNotNull {
            (it as? JsonPrimitive)?.intOrNull
        } ?: listOf(60)

        val userZone = getUserZoneId(context)

        fun parseDate(dateStr: String?): Long? {
            if (dateStr.isNullOrBlank()) return null
            return try {
                if (dateStr.endsWith("Z")) {
                    Instant.parse(dateStr).toEpochMilli()
                } else {
                    LocalDateTime.parse(dateStr)
                        .atZone(userZone)
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

        // ── Akıllı Ortak Oda & Katılımcı Çözümleme (Kişi İsminden Otomatik Odayı Çözme) ──
        var finalSharedRoomId = sharedRoomId
        val finalParticipantIds = participantIds.toMutableList()
        val roomMatchScores = mutableMapOf<String, Int>()

        // Kullanıcının mesajında geçen TÜM oda üyelerini dinamik olarak bul ve katılımcı listesine ekle
        context.sharedRooms.forEach { room ->
            room.memberNames.forEach { memberStr ->
                // memberStr formatı: "İsim (@kullaniciadi) (id:123)"
                val idMatch = Regex("id:(\\d+)").find(memberStr)?.groupValues?.get(1)?.toIntOrNull()
                val cleanParts = memberStr.split("(", ")", "@", " ")
                    .map { it.trim() }
                    .filter { it.length >= 2 && !it.startsWith("id:") }

                val isMentioned = cleanParts.any { namePart ->
                    userInput.contains(namePart, ignoreCase = true) || title.contains(namePart, ignoreCase = true)
                }

                if (isMentioned) {
                    roomMatchScores[room.roomId] = (roomMatchScores[room.roomId] ?: 0) + 1
                    if (idMatch != null && !finalParticipantIds.contains(idMatch)) {
                        finalParticipantIds.add(idMatch)
                    }
                }
            }
        }

        // Eğer doğrudan bir oda belirtilmediyse, en çok üyesi eşleşen odayı seç
        if (finalSharedRoomId.isNullOrBlank() && roomMatchScores.isNotEmpty()) {
            finalSharedRoomId = roomMatchScores.maxByOrNull { it.value }?.key
        }

        // Görevi oluşturan kullanıcının kendisini de her zaman katılımcı/sorumlu olarak ekle
        val allParticipants = if (context.currentUserId > 0) {
            (listOf(context.currentUserId) + finalParticipantIds).distinct()
        } else finalParticipantIds

        val participantsMap = allParticipants.associateWith { "ACCEPTED" }

        return when (intentStr) {
            "CREATE_TASK" -> {
                val isShared = !finalSharedRoomId.isNullOrBlank()
                val req = CreateTaskRequest(
                    title = title,
                    description = description,
                    startTime = startMs ?: nowMs,
                    endTime = null,
                    type = TaskType.TASK,
                    status = taskStatus,
                    visibility = if (isShared) TaskVisibility.ROOM_SHARED else TaskVisibility.PRIVATE,
                    sharedRoomIds = if (isShared) listOf(finalSharedRoomId) else emptyList(),
                    participants = participantsMap,
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
                        tags = emptyList(),
                        sharedRoomId = finalSharedRoomId,
                        participantUserIds = allParticipants
                    )
                )
            }
            "CREATE_EVENT" -> {
                val isShared = !finalSharedRoomId.isNullOrBlank()
                val req = CreateTaskRequest(
                    title = title,
                    description = description,
                    startTime = startMs ?: nowMs,
                    endTime = endMs ?: (nowMs + 3600_000L),
                    type = TaskType.EVENT,
                    status = taskStatus,
                    visibility = if (isShared) TaskVisibility.ROOM_SHARED else TaskVisibility.PRIVATE,
                    sharedRoomIds = if (isShared) listOf(finalSharedRoomId) else emptyList(),
                    participants = participantsMap,
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
                        sharedRoomId = finalSharedRoomId,
                        participantUserIds = allParticipants
                    )
                )
            }
            "CREATE_NOTE" -> {
                val isShared = !finalSharedRoomId.isNullOrBlank()
                val req = CreateTaskRequest(
                    title = title,
                    description = description,
                    startTime = nowMs,
                    type = TaskType.NOTE,
                    status = TaskStatus.COMPLETED,
                    visibility = if (isShared) TaskVisibility.ROOM_SHARED else TaskVisibility.PRIVATE,
                    sharedRoomIds = if (isShared) listOf(finalSharedRoomId) else emptyList(),
                    participants = participantsMap,
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

                if (targetRoom != null && targetUser != null) {
                    AiChatResult(
                        intent = AiIntent.INVITE_TO_ROOM,
                        replyText = replyText,
                        shouldInviteUser = true,
                        inviteRoomId = targetRoom.roomId,
                        inviteUserId = targetUser.first,
                        extractedEntities = ExtractedEntities(
                            title = "Odaya Davet",
                            targetRoomName = roomName,
                            targetUsername = targetUsername,
                            targetUserId = targetUser.first
                        )
                    )
                } else {
                    // Kullanıcı odaya üye davet etmek yerine kişiyle plan/etkinlik eklemek istemiş olabilir (örn: "dilberle saat 5e ekle")
                    val isShared = !finalSharedRoomId.isNullOrBlank()
                    val isEvent = startMs != null
                    val req = CreateTaskRequest(
                        title = title,
                        description = description,
                        startTime = startMs ?: nowMs,
                        endTime = endMs ?: (if (startMs != null) startMs + 3600_000L else null),
                        type = if (isEvent) TaskType.EVENT else TaskType.TASK,
                        status = taskStatus,
                        visibility = if (isShared) TaskVisibility.ROOM_SHARED else TaskVisibility.PRIVATE,
                        sharedRoomIds = if (isShared) listOf(finalSharedRoomId) else emptyList(),
                        reminders = reminders,
                        specificDetails = if (isEvent) ItemDetails.Event(location = location) else ItemDetails.Task(deadline = startMs),
                        color = if (isEvent) "#2196F3" else "#4CAF50"
                    )
                    AiChatResult(
                        intent = if (isEvent) AiIntent.CREATE_EVENT else AiIntent.CREATE_TASK,
                        replyText = replyText,
                        shouldCreateTask = true,
                        suggestedTaskRequest = req,
                        extractedEntities = ExtractedEntities(
                            title = title,
                            description = description,
                            type = if (isEvent) TaskType.EVENT else TaskType.TASK,
                            dateTime = startMs,
                            endDateTime = endMs,
                            location = location,
                            sharedRoomId = finalSharedRoomId,
                            participantUserIds = allParticipants
                        )
                    )
                }
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
