package com.yusufteker.pulse.core.ai

import com.yusufteker.pulse.core.utils.getCurrentTimeMs
import com.yusufteker.pulse.shared.ai.AiChatContext
import com.yusufteker.pulse.shared.ai.AiChatResult
import com.yusufteker.pulse.shared.ai.AiIntent
import com.yusufteker.pulse.shared.ai.ExtractedEntities
import com.yusufteker.pulse.shared.api.AiMetadata
import com.yusufteker.pulse.shared.api.CreateTaskRequest
import com.yusufteker.pulse.shared.api.TaskPriority
import com.yusufteker.pulse.shared.api.TaskType
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Kural tabanlı NLP motoru — sıfır bağımlılık, her zaman çalışır.
 *
 * Türkçe (öncelikli) ve İngilizce doğal dil girdilerini anlamak için
 * anahtar kelime ve regex tabanlı çıkarım yapar.
 *
 * Yetenekler:
 * - Niyet sınıflandırma (görev/etkinlik/not/sorgu/sohbet)
 * - Türkçe tarih/saat ayrıştırma (mutlak ve göreceli)
 * - Kişi, öncelik, etiket çıkarımı
 * - Türkçe şablon tabanlı yanıt üretimi
 * - Duygu analizi (olumlu/olumsuz/nötr)
 */
class RuleBasedNlpEngine {

    // ── Türkçe Gün Adları ──
    private val turkishDays = mapOf(
        "pazartesi" to 1, "salı" to 2, "çarşamba" to 3, "çarşamba" to 3,
        "perşembe" to 4, "cuma" to 5, "cumartesi" to 6, "pazar" to 7
    )

    // ── Türkçe Ay Adları ──
    private val turkishMonths = mapOf(
        "ocak" to 1, "şubat" to 2, "mart" to 3, "nisan" to 4,
        "mayıs" to 5, "haziran" to 6, "temmuz" to 7, "ağustos" to 8,
        "eylül" to 9, "ekim" to 10, "kasım" to 11, "aralık" to 12
    )

    // ── Türkçe Günün Zaman Dilimleri (24 saat formatında varsayılan saat) ──
    private val timeOfDayMap = mapOf(
        "sabah" to 9, "sabahleyin" to 9,
        "öğle" to 12, "öğlen" to 12, "öğleyin" to 12,
        "akşam" to 20, "akşamleyin" to 20, "akşam üstü" to 18, "akşamüstü" to 18,
        "gece" to 22, "geceleyin" to 22,
        "ikindi" to 16,
        "kuşluk" to 10
    )

    // ── Niyet Anahtar Kelimeleri ──
    private val eventKeywords = listOf(
        "toplantı", "randevu", "buluş", "etkinlik", "davet", "organizasyon",
        "meeting", "appointment", "event", "gathering", "plan",
        "buluşalım", "görüşelim", "toplanalım"
    )

    private val taskKeywords = listOf(
        "görev", "yap", "hatırlat", "yapmam lazım", "unutmadan", "yapılacak",
        "task", "todo", "to-do", "remind me", "remind",
        "hallet", "tamamla", "bitir", "görevi"
    )

    private val noteKeywords = listOf(
        "not al", "not:", "kaydet", "not et", "yaz",
        "note", "save this", "write down", "not alayım",
        "aklımda kalsın", "kenara yaz", "şunu yaz"
    )

    private val queryKeywords = listOf(
        "ne zaman", "neler var", "program", "takvim", "göster",
        "listele", "planlarım", "görevlerim", "etkinliklerim",
        "what do I have", "show", "list", "schedule", "calendar"
    )

    // ── Öncelik Anahtar Kelimeleri ──
    private val urgentKeywords = listOf("acil", "hemen", "şimdi", "urgent", "asap", "ivedi", "çok acil")
    private val highKeywords = listOf("önemli", "kritik", "important", "critical", "high")
    private val lowKeywords = listOf("düşük", "önemsiz", "low", "minor", "basit")

    // ── Duygu (Sentiment) Anahtar Kelimeleri ──
    private val positiveWords = listOf(
        "harika", "süper", "güzel", "iyi", "muhteşem", "severim", "seviyorum",
        "great", "good", "nice", "awesome", "love", "like"
    )
    private val negativeWords = listOf(
        "kötü", "berbat", "istemiyorum", "sıkıcı", "zor", "problem",
        "bad", "terrible", "hate", "boring", "hard", "difficult"
    )

    // ── Etiket Anahtar Kelimeleri ──
    private val tagMappings = mapOf(
        "iş" to "iş", "work" to "iş",
        "özel" to "özel", "personal" to "özel",
        "spor" to "spor", "egzersiz" to "spor", "exercise" to "spor", "gym" to "spor",
        "okul" to "okul", "ders" to "okul", "school" to "okul", "study" to "okul",
        "alışveriş" to "alışveriş", "market" to "alışveriş", "shopping" to "alışveriş",
        "sağlık" to "sağlık", "doktor" to "sağlık", "health" to "sağlık",
        "aile" to "aile", "family" to "aile",
        "arkadaş" to "sosyal", "friend" to "sosyal"
    )

    /**
     * Tam mesaj işleme: niyet + varlık + yanıt + görev isteği oluşturma.
     */
    fun processMessage(input: String, context: AiChatContext): AiChatResult {
        val trimmed = input.trim()
        val lower = trimmed.lowercase()

        // 1. Niyet sınıflandırma
        val intent = classifyIntent(lower)

        // 2. Varlık çıkarımı
        val entities = extractEntities(trimmed, lower, context)

        // 3. Yanıt üret
        val replyText = generateResponse(intent, entities)

        // 4. AI metadata oluştur
        val sentiment = detectSentiment(lower)
        val aiMetadata = AiMetadata(
            summary = entities.title.take(200),
            extractedActionItems = if (entities.description != null) {
                splitActionItems(entities.description?:"")
            } else emptyList(),
            autoScheduledConfidence = entities.confidence,
            sentiment = sentiment
        )

        // 5. Görev oluşturma isteği (CREATE_TASK, CREATE_EVENT, CREATE_NOTE)
        val shouldCreate = intent == AiIntent.CREATE_TASK ||
                intent == AiIntent.CREATE_EVENT ||
                intent == AiIntent.CREATE_NOTE

        val taskRequest = if (shouldCreate) {
            buildCreateTaskRequest(trimmed, intent, entities, context, aiMetadata)
        } else null

        return AiChatResult(
            intent = intent,
            replyText = replyText,
            extractedEntities = entities,
            aiMetadata = aiMetadata,
            shouldCreateTask = shouldCreate,
            suggestedTaskRequest = taskRequest
        )
    }

    // ═══════════════════════════════════════════════════════════
    // NİYET SINIFLANDIRMA
    // ═══════════════════════════════════════════════════════════

    fun classifyIntent(lowerInput: String): AiIntent {
        // @mention varsa → EVENT ağırlıklı
        val hasMention = lowerInput.contains("@")

        // Anahtar kelime puanlaması
        var eventScore = 0
        var taskScore = 0
        var noteScore = 0
        var queryScore = 0

        for (kw in eventKeywords) {
            if (lowerInput.contains(kw)) eventScore += 2
        }
        for (kw in taskKeywords) {
            if (lowerInput.contains(kw)) taskScore += 2
        }
        for (kw in noteKeywords) {
            if (lowerInput.contains(kw)) noteScore += 3 // not anahtarları daha spesifik
        }
        for (kw in queryKeywords) {
            if (lowerInput.contains(kw)) queryScore += 2
        }

        // Zaman ifadeleri varsa → görev/etkinlik olasılığı artar
        val hasTimeExpression = extractDateTime(lowerInput) != null
        if (hasTimeExpression) {
            eventScore += 1
            taskScore += 1
        }

        // @mention + zaman → büyük olasılıkla etkinlik
        if (hasMention && hasTimeExpression) {
            eventScore += 2
        }
        if (hasMention) {
            eventScore += 1
        }

        // En yüksek skoru bul
        val maxScore = maxOf(eventScore, taskScore, noteScore)

        return when {
            noteScore == maxScore && noteScore > 0 -> AiIntent.CREATE_NOTE
            eventScore == maxScore && eventScore > 0 -> AiIntent.CREATE_EVENT
            else -> AiIntent.CREATE_TASK // Varsayılan olarak TASK ekle, CHAT kaldırıldı
        }
    }

    // ═══════════════════════════════════════════════════════════
    // VARLIK ÇIKARIMI
    // ═══════════════════════════════════════════════════════════

    fun extractEntities(
        original: String,
        lowerInput: String,
        context: AiChatContext
    ): ExtractedEntities {
        val dateTime = extractDateTime(lowerInput)
        val location = extractLocation(original)
        val participants = extractParticipants(original, context)
        val priority = extractPriority(lowerInput)
        val tags = extractTags(lowerInput)
        val isAllDay = lowerInput.contains("tüm gün") || lowerInput.contains("all day")

        // Başlık: zaman/niyet kelimelerinden arındırılmış hali
        val title = extractCleanTitle(original)
        // Açıklama: başlıktan sonrası varsa
        val description = if (original.length > 80) original.substring(80).trim() else null
        
        // Confidence Hesaplaması
        var conf = 0.6f
        if (title.isNotBlank() && title != "Yeni Görev") {
            conf += 0.2f
        }
        
        val timeWords = listOf(
            "yarın", "bugün", "saat", "haftaya", "sonra", "gün", 
            "akşam", "sabah", "öğle", "gece", "dakika", 
            "tomorrow", "today", "next", "at ", "in "
        )
        val hasTimeWord = timeWords.any { lowerInput.contains(it) }
        
        if (hasTimeWord) {
            if (dateTime != null) {
                conf += 0.2f // Zamanı başarıyla bulduk
            } else {
                conf -= 0.4f // Zaman kelimesi var ama ayrıştıramadık, Cloud'a düşsün
            }
        }

        return ExtractedEntities(
            title = title,
            description = description,
            type = inferType(lowerInput),
            dateTime = dateTime,
            endDateTime = if (isAllDay && dateTime != null) dateTime + 86_400_000 else null,
            isAllDay = isAllDay,
            location = location,
            participants = participants,
            priority = priority,
            recurrenceRule = extractRecurrence(lowerInput),
            tags = tags,
            estimatedMinutes = extractDuration(lowerInput),
            confidence = conf
        )
    }

    private fun extractCleanTitle(input: String): String {
        var clean = input.lowercase()
        val removeWords = listOf(
            "yarın", "yarin", "bugün", "bugun", "haftaya", 
            "yapmalıyım", "yapmaliyim", "lazım", "lazim", 
            "hatırlat", "hatirlat", "not al", "ekle", "planla",
            "gerek", "istiyorum", "lütfen", "lutfen"
        )
        for (word in removeWords) {
            clean = clean.replace(Regex("\\b$word\\b"), "")
        }
        // Zaman kelimelerini temizle (örn: saat 5'te)
        clean = clean.replace(Regex("""(?:saat\s*)?\d{1,2}[.:]?\d{0,2}\s*['']?(te|de|da|ta)\b"""), "")
        
        val trimmed = clean.trim().replace(Regex("\\s+"), " ")
        if (trimmed.isEmpty()) return "Yeni Görev"
        
        return trimmed.take(80).replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    // ── Tür Sezimi ──
    private fun inferType(lowerInput: String): TaskType {
        for (kw in eventKeywords) {
            if (lowerInput.contains(kw)) return TaskType.EVENT
        }
        if (lowerInput.contains("@")) return TaskType.EVENT
        for (kw in noteKeywords) {
            if (lowerInput.contains(kw)) return TaskType.NOTE
        }
        if (lowerInput.contains("not al") || lowerInput.startsWith("not:")) return TaskType.NOTE
        return TaskType.TASK
    }

    // ── Tarih/Saat Çıkarımı ──
    fun extractDateTime(lowerInput: String): Long? {
        val nowMs = getCurrentTimeMs()

        // "yarın" → yarın 09:00
        if (Regex("\\byar[ıi]n\\b").containsMatchIn(lowerInput)) {
            return parseTimeWithDayOffset(lowerInput, nowMs, 1)
        }

        // "bugün" → bugün (varsayılan: şimdiki saat + 1)
        if (Regex("\\bbug[üu]n\\b").containsMatchIn(lowerInput)) {
            return parseTimeWithDayOffset(lowerInput, nowMs, 0)
        }

        // "yarın saat 3'te", "yarın 15:00'de", "yarın 15.00'te"
        val yarinSaatPattern = Regex("""yar[ıi]n\s*(saat\s*)?(\d{1,2})['.:]?(\d{2})?\s*['']?(te|de|da|ta)?""")
        val yarinMatch = yarinSaatPattern.find(lowerInput)
        if (yarinMatch != null) {
            val hour = yarinMatch.groupValues[2].toIntOrNull() ?: return null
            val minute = yarinMatch.groupValues[3].toIntOrNull() ?: 0
            return setTimeOnDay(nowMs, 1, hour, minute)
        }

        // "bugün saat 3'te", "bugün 15:00'de"
        val bugunSaatPattern = Regex("""bug[üu]n\s*(saat\s*)?(\d{1,2})['.:]?(\d{2})?\s*['']?(te|de|da|ta)?""")
        val bugunMatch = bugunSaatPattern.find(lowerInput)
        if (bugunMatch != null) {
            val hour = bugunMatch.groupValues[2].toIntOrNull() ?: return null
            val minute = bugunMatch.groupValues[3].toIntOrNull() ?: 0
            return setTimeOnDay(nowMs, 0, hour, minute)
        }

        // "X gün sonra"
        val gunSonra = Regex("""(\d+)\s*gün\s*sonra""").find(lowerInput)
        if (gunSonra != null) {
            val days = gunSonra.groupValues[1].toIntOrNull() ?: return null
            return setTimeOnDay(nowMs, days, 9, 0)
        }

        // "X saat sonra"
        val saatSonra = Regex("""(\d+)\s*saat\s*sonra""").find(lowerInput)
        if (saatSonra != null) {
            val hours = saatSonra.groupValues[1].toIntOrNull() ?: return null
            return nowMs + hours * 3_600_000L
        }

        // "haftaya salı", "haftaya çarşamba"
        val haftayaPattern = Regex("""haftaya?\s*(${turkishDays.keys.joinToString("|")})""")
        val haftayaMatch = haftayaPattern.find(lowerInput)
        if (haftayaMatch != null) {
            val dayName = haftayaMatch.groupValues[1]
            val targetDay = turkishDays[dayName] ?: return null
            return getNextWeekday(lowerInput, nowMs, targetDay, 1)
        }

        // "gelecek hafta pazartesi", "önümüzdeki hafta salı"
        val gelecekHaftaPattern = Regex("""(?:gelecek|önümüzdeki)\s*hafta\s*(${turkishDays.keys.joinToString("|")})""")
        val gelecekMatch = gelecekHaftaPattern.find(lowerInput)
        if (gelecekMatch != null) {
            val dayName = gelecekMatch.groupValues[1]
            val targetDay = turkishDays[dayName] ?: return null
            return getNextWeekday(lowerInput, nowMs, targetDay, 1)
        }

        // "salı günü", "çarşamba" (tek başına gün adı) → bu hafta veya gelecek hafta
        for ((dayName, dayNum) in turkishDays) {
            val dayPattern = Regex("""\b$dayName\b""")
            if (dayPattern.containsMatchIn(lowerInput) &&
                !lowerInput.contains("hafta") &&
                !lowerInput.contains("gelecek")
            ) {
                return getNextWeekday(lowerInput, nowMs, dayNum, 0)
            }
        }

        // İngilizce: "tomorrow", "today"
        if (lowerInput.contains("tomorrow")) {
            return parseEnglishTimeWithDayOffset(lowerInput, nowMs, 1)
        }
        if (lowerInput.contains("today")) {
            return parseEnglishTimeWithDayOffset(lowerInput, nowMs, 0)
        }

        // İngilizce: "next week", "in X days", "in X hours"
        val inDays = Regex("""in\s+(\d+)\s*days?""").find(lowerInput)
        if (inDays != null) {
            val days = inDays.groupValues[1].toIntOrNull() ?: return null
            return setTimeOnDay(nowMs, days, 9, 0)
        }
        val inHours = Regex("""in\s+(\d+)\s*hours?""").find(lowerInput)
        if (inHours != null) {
            val hours = inHours.groupValues[1].toIntOrNull() ?: return null
            return nowMs + hours * 3_600_000L
        }

        // "next monday", "next tuesday" etc.
        val englishDays = mapOf(
            "monday" to 1, "tuesday" to 2, "wednesday" to 3, "thursday" to 4,
            "friday" to 5, "saturday" to 6, "sunday" to 7
        )
        for ((enDay, dayNum) in englishDays) {
            if (lowerInput.contains("next $enDay")) {
                return getNextWeekday(lowerInput, nowMs, dayNum, 1)
            }
            if (lowerInput.contains(enDay) && !lowerInput.contains("next")) {
                return getNextWeekday(lowerInput, nowMs, dayNum, 0)
            }
        }

        // Standalone time patterns (if no day is specified, assume today/tomorrow)
        // "saat 15:00'te", "saat 3'te", "15:00'de", "15.00'te"
        val saatPattern = Regex("""(?:saat\s*)?(\d{1,2})[.:](\d{2})?\s*['']?(te|de|da)?""")
        val saatMatch = saatPattern.find(lowerInput)
        if (saatMatch != null && !lowerInput.contains("gün") && !lowerInput.contains("hafta")) {
            var hour = saatMatch.groupValues[1].toIntOrNull() ?: return null
            val minute = saatMatch.groupValues[2].toIntOrNull() ?: 0
            
            if (hour < 12) {
                if (lowerInput.contains("akşam") || lowerInput.contains("ikindi") || lowerInput.contains("öğle") || (lowerInput.contains("gece") && hour > 5)) {
                    hour += 12
                }
            } else if (hour == 12 && (lowerInput.contains("gece") || lowerInput.contains("sabah") || lowerInput.contains("akşam"))) {
                hour = 0
            }
            
            // Verilen saat şimdiden geçtiyse yarın olarak ayarla
            return setTimeOnDay(nowMs, 0, hour, minute).let { result ->
                if (result <= nowMs) result + 86_400_000 else result
            }
        }

        // Günün zaman dilimi: "sabah 9'da", "akşam 8'de"
        for ((period, defaultHour) in timeOfDayMap) {
            if (lowerInput.contains(period)) {
                // Belirli bir saat de verilmiş olabilir: "sabah 10'da"
                val periodHourPattern = Regex("""$period\s*(?:saat\s*)?(\d{1,2})""")
                val periodMatch = periodHourPattern.find(lowerInput)
                var hour = periodMatch?.groupValues?.get(1)?.toIntOrNull() ?: defaultHour
                
                if (hour < 12 && defaultHour >= 12) {
                    if (!(period.contains("gece") && hour < 6)) {
                        hour += 12
                    }
                } else if (hour == 12 && (period.contains("gece") || period.contains("sabah") || period.contains("akşam"))) {
                    hour = 0
                }
                
                return setTimeOnDay(nowMs, 0, hour, 0).let { result ->
                    if (result <= nowMs) result + 86_400_000 else result
                }
            }
        }

        return null
    }

    // ── Yardımcı: Gün ofsetiyle saati ayarla ──
    private fun parseTimeWithDayOffset(input: String, nowMs: Long, dayOffset: Int): Long {
        // Önce belirli bir saat aranır: "yarın saat 15:00'te", "bugün 3'te"
        val timePattern = Regex("""(?:saat\s*)?(\d{1,2})[.:](\d{2})?\s*['']?(te|de|da)?""")
        val timeMatch = timePattern.find(input)
        if (timeMatch != null) {
            var hour = timeMatch.groupValues[1].toIntOrNull() ?: 9
            val minute = timeMatch.groupValues[2].toIntOrNull() ?: 0
            
            if (hour < 12) {
                if (input.contains("akşam") || input.contains("ikindi") || input.contains("öğle") || (input.contains("gece") && hour > 5)) {
                    hour += 12
                }
            } else if (hour == 12 && (input.contains("gece") || input.contains("sabah") || input.contains("akşam"))) {
                hour = 0
            }
            
            return setTimeOnDay(nowMs, dayOffset, hour, minute)
        }

        // Günün zaman dilimi: "yarın akşam", "bugün sabah"
        for ((period, defaultHour) in timeOfDayMap) {
            if (input.contains(period)) {
                return setTimeOnDay(nowMs, dayOffset, defaultHour, 0)
            }
        }

        // Varsayılan: 09:00
        return setTimeOnDay(nowMs, dayOffset, 9, 0)
    }

    private fun parseEnglishTimeWithDayOffset(input: String, nowMs: Long, dayOffset: Int): Long {
        val timePattern = Regex("""at\s+(\d{1,2})(?::(\d{2}))?\s*(am|pm)?""")
        val timeMatch = timePattern.find(input)
        if (timeMatch != null) {
            var hour = timeMatch.groupValues[1].toIntOrNull() ?: 9
            val minute = timeMatch.groupValues[2].toIntOrNull() ?: 0
            val amPm = timeMatch.groupValues[3]
            if (amPm == "pm" && hour < 12) hour += 12
            if (amPm == "am" && hour == 12) hour = 0
            return setTimeOnDay(nowMs, dayOffset, hour, minute)
        }
        return setTimeOnDay(nowMs, dayOffset, 9, 0)
    }

    /**
     * Şimdiki zamandan itibaren [dayOffset] gün sonrasının [hour]:[minute] anını döner.
     */
    private fun setTimeOnDay(nowMs: Long, dayOffset: Int, hour: Int, minute: Int): Long {
        val timeZone = TimeZone.currentSystemDefault()
        val currentLocal = kotlinx.datetime.Instant.fromEpochMilliseconds(getCurrentTimeMs()).toLocalDateTime(timeZone)
        
        val targetDate = currentLocal.date.plus(dayOffset, DateTimeUnit.DAY)
        val targetTime = LocalTime(hour, minute)
        
        return LocalDateTime(targetDate, targetTime).toInstant(timeZone).toEpochMilliseconds()
    }

    /**
     * [weekOffset] hafta sonrasındaki [targetDayOfWeek] gününü bulur.
     * weekOffset=0 → bu hafta (bugün geçtiyse gelecek hafta)
     * weekOffset=1 → gelecek hafta
     */
    private fun getNextWeekday(input: String, nowMs: Long, targetDayOfWeek: Int, weekOffset: Int): Long {
        val timeZone = TimeZone.currentSystemDefault()
        val currentLocal = kotlinx.datetime.Instant.fromEpochMilliseconds(getCurrentTimeMs()).toLocalDateTime(timeZone)
        
        val currentDayOfWeek = currentLocal.dayOfWeek.isoDayNumber // 1=Pzt, 7=Pzr
        
        var daysUntil = targetDayOfWeek - currentDayOfWeek
        if (daysUntil < 0) daysUntil += 7
        
        daysUntil += weekOffset * 7
        
        return parseTimeWithDayOffset(input, nowMs, daysUntil)
    }

    // ── Konum Çıkarımı ──
    private fun extractLocation(input: String): String? {
        // "X'te", "X'de", "X'da" kalıpları → konum olabilir
        // "ofiste", "evde", "kafede", "lokantada"
        val locationPatterns = listOf(
            Regex("""(?:konum[:\s]*|yer[:\s]*|location[:\s]*|at\s+)(\S+)"""),
            Regex("""(\w+(?:' ?)?(?:te|de|ta|da|nde|nda))\b""")
        )
        // Basit: yaygın konum kelimelerini tara
        val knownLocations = listOf(
            "ofis", "ev", "kafe", "restoran", "lokanta", "okul", "hastane",
            "spor salonu", "park", "sinema", "AVM", "alışveriş merkezi",
            "office", "home", "cafe", "restaurant", "school", "hospital", "gym", "park"
        )
        for (loc in knownLocations) {
            if (input.lowercase().contains(loc)) return loc
        }
        return null
    }

    // ── Katılımcı Çıkarımı ──
    private fun extractParticipants(input: String, context: AiChatContext): List<String> {
        val participants = mutableListOf<String>()
        // @username kalıbı
        val mentionPattern = Regex("""@(\w+)""")
        val mentions = mentionPattern.findAll(input).map { it.groupValues[1] }.toList()

        for (mention in mentions) {
            // Takip edilen kullanıcılarla eşleştir
            val matched = context.followedUsers.find {
                it.second.equals(mention, ignoreCase = true)
            }
            if (matched != null) {
                participants.add(matched.second)
            } else {
                participants.add(mention)
            }
        }

        // "X ile", "X'le", "X ve Y ile" kalıpları
        val ilePattern = Regex("""(\w+)\s*(?:ile|'le|yle)\s*(?:buluş|görüş|toplantı)""")
        val ileMatches = ilePattern.findAll(input)
        for (match in ileMatches) {
            val name = match.groupValues[1]
            if (name !in participants && name.length > 2) {
                participants.add(name)
            }
        }

        return participants
    }

    // ── Öncelik Çıkarımı ──
    private fun extractPriority(lowerInput: String): TaskPriority {
        for (kw in urgentKeywords) {
            if (lowerInput.contains(kw)) return TaskPriority.URGENT
        }
        for (kw in highKeywords) {
            if (lowerInput.contains(kw)) return TaskPriority.HIGH
        }
        for (kw in lowKeywords) {
            if (lowerInput.contains(kw)) return TaskPriority.LOW
        }
        return TaskPriority.MEDIUM
    }

    // ── Etiket Çıkarımı ──
    private fun extractTags(lowerInput: String): List<String> {
        val tags = mutableListOf<String>()
        // #etiket kalıbı
        val hashtagPattern = Regex("""#(\w+)""")
        tags.addAll(hashtagPattern.findAll(lowerInput).map { it.groupValues[1] })

        // Anahtar kelime eşleştirme
        for ((keyword, tag) in tagMappings) {
            if (lowerInput.contains(keyword) && tag !in tags) {
                tags.add(tag)
            }
        }

        return tags.distinct().take(5)
    }

    // ── Tekrarlama Kuralı Çıkarımı ──
    private fun extractRecurrence(lowerInput: String): String? {
        // "her gün", "her gun"
        if (Regex("""her\s*gün""").containsMatchIn(lowerInput)) return "RRULE:FREQ=DAILY"
        // "her hafta", "her hafta pazartesi"
        val herHaftaPattern = Regex("""her\s*hafta\s*(${turkishDays.keys.joinToString("|")})?""")
        val herHaftaMatch = herHaftaPattern.find(lowerInput)
        if (herHaftaMatch != null) {
            val day = herHaftaMatch.groupValues.getOrNull(1)
            return if (day != null) "RRULE:FREQ=WEEKLY;BYDAY=${day.take(2).uppercase()}" else "RRULE:FREQ=WEEKLY"
        }
        // "her ay", "her ayın 15'i"
        if (Regex("""her\s*ay""").containsMatchIn(lowerInput)) return "RRULE:FREQ=MONTHLY"
        // "her yıl", "her sene"
        if (Regex("""her\s*(yıl|sene)""").containsMatchIn(lowerInput)) return "RRULE:FREQ=YEARLY"

        // İngilizce
        if (Regex("""every\s*day""").containsMatchIn(lowerInput)) return "RRULE:FREQ=DAILY"
        if (Regex("""every\s*week""").containsMatchIn(lowerInput)) return "RRULE:FREQ=WEEKLY"
        if (Regex("""every\s*month""").containsMatchIn(lowerInput)) return "RRULE:FREQ=MONTHLY"
        if (Regex("""every\s*year""").containsMatchIn(lowerInput)) return "RRULE:FREQ=YEARLY"

        return null
    }

    // ── Süre Çıkarımı ──
    private fun extractDuration(lowerInput: String): Int? {
        // "30 dakika", "1 saat", "2 saat"
        val dakikaPattern = Regex("""(\d+)\s*dakika""")
        val saatPattern = Regex("""(\d+)\s*saat""")

        dakikaPattern.find(lowerInput)?.let {
            return it.groupValues[1].toIntOrNull()
        }
        saatPattern.find(lowerInput)?.let {
            return (it.groupValues[1].toIntOrNull() ?: 1) * 60
        }
        return null
    }

    // ═══════════════════════════════════════════════════════════
    // YANIT ÜRETİMİ
    // ═══════════════════════════════════════════════════════════

    fun generateResponse(intent: AiIntent, entities: ExtractedEntities): String {
        val title = entities.title
        val timeStr = entities.dateTime?.let { formatDateTimeForResponse(it) }

        return when (intent) {
            AiIntent.CREATE_TASK -> {
                val templates = listOf(
                    "Harika, '${title}' görevini listene ekledim.",
                    "Tamamdır! '${title}' görevini oluşturdum.",
                    "Görev başarıyla eklendi: '${title}'."
                )
                val base = templates.random()
                if (timeStr != null) {
                    "$base $timeStr için planlandı."
                } else {
                    "$base Zaman belirtmediğin için şimdilik plansız olarak kaydettim."
                }
            }
            AiIntent.CREATE_EVENT -> {
                val locStr = entities.location?.let { " ($it konumunda)" } ?: ""
                val participantStr = if (entities.participants.isNotEmpty()) {
                    " Katılımcılar: ${entities.participants.joinToString(", ")}."
                } else ""
                
                val templates = listOf(
                    "Takvimine ekledim: '${title}'.",
                    "Etkinlik oluşturuldu: '${title}'.",
                    "'${title}' etkinliğini planladım."
                )
                val base = templates.random()
                
                if (timeStr != null) {
                    "$base Zamanı $timeStr olarak ayarlandı.$locStr$participantStr"
                } else {
                    "$base$locStr$participantStr"
                }
            }
            AiIntent.CREATE_NOTE -> {
                val templates = listOf(
                    "Notunu güvenle kaydettim: '${title}'",
                    "Aklında kalmasın, ben not aldım: '${title}'",
                    "Tamamdır, bu notu saklıyorum: '${title}'"
                )
                templates.random()
            }
            AiIntent.QUERY, AiIntent.CHAT, AiIntent.UNKNOWN -> {
                // CHAT veya QUERY gelirse artık CREATE_TASK gibi davranıyoruz, ancak
                // tip güvenliği için yine de metinleri tutabiliriz.
                val templates = listOf(
                    "Bunu senin için bir görev olarak kaydettim: '${title}'.",
                    "Anlaşıldı, bunu yapacaklar listene ekliyorum: '${title}'."
                )
                templates.random()
            }
        }
    }

    // ── Yanıt için tarih formatlama (Türkçe, göreceli) ──
    private fun formatDateTimeForResponse(epochMs: Long): String {
        val timeZone = TimeZone.currentSystemDefault()
        val targetLocal = kotlinx.datetime.Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(timeZone)
        val currentLocal = kotlinx.datetime.Instant.fromEpochMilliseconds(getCurrentTimeMs()).toLocalDateTime(timeZone)
        
        val diffDays = (targetLocal.date.toEpochDays() - currentLocal.date.toEpochDays()).toLong()

        val hour = targetLocal.hour
        val minute = targetLocal.minute
        val timeStr = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

        return when {
            diffDays < 0L -> "geçmiş bir zaman"
            diffDays == 0L -> "bugün saat $timeStr"
            diffDays == 1L -> "yarın saat $timeStr"
            diffDays < 7L -> "$diffDays gün sonra, saat $timeStr"
            else -> "belirtilen zamanda"
        }
    }

    // ═══════════════════════════════════════════════════════════
    // DUYGU ANALİZİ
    // ═══════════════════════════════════════════════════════════

    fun detectSentiment(lowerInput: String): String? {
        var positiveCount = 0
        var negativeCount = 0

        for (word in positiveWords) {
            if (lowerInput.contains(word)) positiveCount++
        }
        for (word in negativeWords) {
            if (lowerInput.contains(word)) negativeCount++
        }

        return when {
            positiveCount > negativeCount -> "positive"
            negativeCount > positiveCount -> "negative"
            else -> "neutral"
        }
    }

    // ═══════════════════════════════════════════════════════════
    // İŞLEM MADDELERİ AYRIŞTIRMA
    // ═══════════════════════════════════════════════════════════

    private fun splitActionItems(text: String): List<String> {
        // Cümleleri veya tireli maddeleri ayır
        val lines = text.split(Regex("""[.;\n•\-*]"""))
            .map { it.trim() }
            .filter { it.length > 3 }
        return if (lines.size > 1) lines.take(5) else listOf(text.take(200))
    }

    // ═══════════════════════════════════════════════════════════
    // CREATE TASK REQUEST OLUŞTURMA
    // ═══════════════════════════════════════════════════════════

    private fun buildCreateTaskRequest(
        originalInput: String,
        intent: AiIntent,
        entities: ExtractedEntities,
        context: AiChatContext,
        aiMetadata: AiMetadata
    ): CreateTaskRequest {
        val nowMs = getCurrentTimeMs()
        val taskType = when (intent) {
            AiIntent.CREATE_EVENT -> TaskType.EVENT
            AiIntent.CREATE_NOTE -> TaskType.NOTE
            else -> entities.type
        }

        // Katılımcı eşleştirme (isim → userId)
        val participantMap = mutableMapOf<Int, String>()
        for (participantName in entities.participants) {
            val match = context.followedUsers.find {
                it.second.equals(participantName, ignoreCase = true)
            }
            if (match != null) {
                participantMap[match.first] = "PENDING"
            }
        }

        // Renk seçimi (türe göre)
        val color = when (taskType) {
            TaskType.TASK -> "#4CAF50"
            TaskType.EVENT -> "#2196F3"
            TaskType.NOTE -> "#FF9800"
        }

        return CreateTaskRequest(
            title = entities.title,
            description = entities.description ?: originalInput.take(500),
            startTime = entities.dateTime ?: nowMs,
            endTime = entities.endDateTime,
            type = taskType,
            status = com.yusufteker.pulse.shared.api.TaskStatus.PENDING,
            visibility = com.yusufteker.pulse.shared.api.TaskVisibility.PRIVATE,
            isRecurring = entities.recurrenceRule != null,
            recurrenceRule = entities.recurrenceRule,
            isFlexible = entities.dateTime == null,
            isOptional = true,
            isPostponable = true,
            isAllDay = entities.isAllDay,
            aiMetadata = aiMetadata,
            reminders = if (entities.dateTime != null) listOf(30) else emptyList(),
            specificDetails = null,
            tags = entities.tags,
            color = color,
            participants = participantMap,
            isSynced = false
        )
    }
}
