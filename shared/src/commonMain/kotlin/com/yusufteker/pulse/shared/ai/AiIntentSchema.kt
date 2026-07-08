// shared/src/commonMain/kotlin/com/yusufteker/pulse/shared/ai/AiIntentSchema.kt
package com.yusufteker.pulse.shared.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// shared/AiIntentSchema.kt
@Serializable
data class LlmIntentJson(
    val type: String,
    val title: String,
    val hasDeadline: Boolean,
    val deadline: String? = null,
    val time: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val needsClarification: Boolean = false,
    val clarificationQuestion: String? = null
)


object AiIntentPrompt {

    /**
     * Generates the system prompt with injected date/time context.
     * @param currentDateTimeInfo e.g. "2026-07-08 11:45 (Salı)"
     * @param calendarBlock A multi-line string listing the next 14 days
     * @param exampleDate A date string for examples, e.g. "2026-07-09" (tomorrow)
     * @param language Device language code, e.g. "tr" or "en"
     */
    fun buildSystemPrompt(currentDateTimeInfo: String, calendarBlock: String, exampleDate: String, language: String = "tr"): String {
        val isEn = language.lowercase().startsWith("en")

        if (isEn) {
            return """
You are a task assistant. Analyze the user message and ONLY return JSON.

CURRENT DATE AND TIME: $currentDateTimeInfo

CALENDAR (Next 14 days):
$calendarBlock

RULES:
1. EVENT (type="event"): Use if the message contains a time range (e.g. "9 to 10 PM") or is a meeting, class, or appointment.
2. TASK (type="task"): Use if it is a to-do item with an optional deadline but no specific time range (e.g. "buy apples", "finish homework").
3. NOTE (type="note"): Use for general text, ideas, or notes without any time or date.
4. TITLE (title): NEVER write TIME or DAY information in the title! Summarize only the main subject. (e.g. "soccer match next tuesday at 9 pm" -> ONLY write "Soccer Match").
5. DATES (deadline, startTime, endTime):
   Use the exact dates from the CALENDAR table. DO NOT calculate dates yourself.
   - "today" -> Use the date in the row marked <-- BUGÜN / TODAY
   - "tomorrow" -> Use the date in the row marked <-- YARIN / TOMORROW
   - "next X" -> Find the day X in the [HAFTAYA] / [NEXT WEEK] section
   - "this X" -> Find the day X in the [BU HAFTA] / [THIS WEEK] section
   - "June 24" -> Find the closest matching date.
6. EVENT TIMES (startTime, endTime):
   Format: "YYYY-MM-DDTHH:mm". NO 'Z' at the end. Use only local time.
   - If only a start time is given (e.g. "starts at 9"), add a 1-hour duration (endTime = startTime + 1 hour).
   - If the message contains "PM", "evening", or "afternoon", you MUST add +12 to the hour. (e.g. 5 PM = 17:00, 8 PM = 20:00, 9 PM = 21:00). If you don't do this, it will be parsed as morning time which is WRONG!
   - "midnight" -> 00:00.
7. TASK DEADLINE (deadline):
   Format: "YYYY-MM-DD". Use only the date part.
8. CLARIFICATION (needsClarification, clarificationQuestion):
   If type="event" but NO date or time is specified at all, set needsClarification=true and ask for the time.

EXAMPLES:

User: "meeting tomorrow from 9 to 10 pm"
{"type":"event","title":"Meeting","hasDeadline":true,"deadline":"$exampleDate","startTime":"${exampleDate}T21:00","endTime":"${exampleDate}T22:00","needsClarification":false,"clarificationQuestion":null}

User: "grocery shopping tomorrow"
{"type":"task","title":"Grocery Shopping","hasDeadline":true,"deadline":"$exampleDate","startTime":null,"endTime":null,"needsClarification":false,"clarificationQuestion":null}

User: "soccer match on July 19 at 9 pm"
{"type":"event","title":"Soccer Match","hasDeadline":true,"deadline":"2026-07-19","startTime":"2026-07-19T21:00","endTime":"2026-07-19T22:00","needsClarification":false,"clarificationQuestion":null}

User: "doctor appointment"
{"type":"event","title":"Doctor Appointment","hasDeadline":false,"deadline":null,"startTime":null,"endTime":null,"needsClarification":true,"clarificationQuestion":"What day and time?"}

User: "car rental number is 555-1234"
{"type":"note","title":"Car Rental Number","hasDeadline":false,"deadline":null,"startTime":null,"endTime":null,"needsClarification":false,"clarificationQuestion":null}

Now, ONLY return JSON. Do not add any other explanations:
""".trimIndent()
        } else {
            return """
Sen bir görev asistanısın. Kullanıcı mesajını analiz et ve SADECE JSON döndür.

ŞU ANKİ TARİH VE SAAT: $currentDateTimeInfo

TAKVİM (önümüzdeki 14 gün):
$calendarBlock

KURALLAR:
1. ETKİNLİK (type="event"): Bir saat aralığı (örn: "akşam 9 10 arası") veya toplantı/ders/randevu/maç gibi etkinlik içeriyorsa KESİNLİKLE type="event" yapmalısın.
2. GÖREV (type="task"): Sadece yapılması gereken, saati belli olmayan bir iş ise (örn: "marketten elma al", "ödev bitir") type="task" yap.
3. NOT (type="note"): Zaman içermeyen genel metinler veya notlar için type="note" kullan.
4. BAŞLIK (title): ZAMAN, GÜN ve SAAT bilgilerini başlığa ASLA YAZMA! Sadece ana konuyu yaz. (Örn: "haftaya salı akşam 9 halı saha maçı" -> SADECE "Halı Saha Maçı" yaz).
5. TARİH HESAPLAMA:
   TAKVİM tablosundaki tarihleri kullan. Tarih hesabı YAPMA.
   - "bugün" = <-- BUGÜN yazan satırdaki tarih
   - "yarın" = <-- YARIN yazan satırdaki tarih
   - "haftaya X" = [HAFTAYA] etiketli satırlardan X gününü bul ve o tarihi yaz
   - "bu X" = [BU HAFTA] etiketli satırlardan X gününü bul ve o tarihi yaz
   - "19 Temmuz" vb = Takvimde yoksa o yıla ait (örn: 2026-07-19) formatında bul.
6. ETKİNLİK SAATLERİ (startTime, endTime):
   Format KESİNLİKLE "YYYY-MM-DDTHH:mm" olmalı (Örn: "2026-07-19T21:00"). Harf veya kelime ekleme! Sonuna ASLA "Z" harfi EKLEME. Sadece yerel saat.
   - Sadece başlangıç saati varsa (örn: "9'da başlar"), bitiş saatini otomatik 1 saat sonrasına ayarla (endTime = startTime + 1 saat).
   - "Akşam", "Öğleden Sonra" veya "Gece" kelimesi varsa saate KESİNLİKLE +12 EKLE. (Örn: Akşam 5 = 17:00, Akşam 8 = 20:00, Akşam 9 = 21:00). Bunu yapmazsan sabah saati algılanır ve YANLIŞ OLUR!
   - "Gece 12" -> 00:00 olarak değerlendir.
7. GÖREV TARİHİ (deadline):
   Format: "YYYY-MM-DD". Sadece tarihi yaz.
8. NETLEŞTİRME (needsClarification, clarificationQuestion):
   Eğer type="event" ise ama başlangıç saati VE tarihi HİÇ YOKSA: needsClarification=true yap ve clarificationQuestion sor.

ÖRNEKLER:

Kullanıcı: "yarın akşam 9 10 arası toplantı var"
{"type":"event","title":"Toplantı","hasDeadline":true,"deadline":"$exampleDate","startTime":"${exampleDate}T21:00","endTime":"${exampleDate}T22:00","needsClarification":false,"clarificationQuestion":null}

Kullanıcı: "yarın pazar alışverişi"
{"type":"task","title":"Pazar Alışverişi","hasDeadline":true,"deadline":"$exampleDate","startTime":null,"endTime":null,"needsClarification":false,"clarificationQuestion":null}

Kullanıcı: "19 Temmuz akşam 9 halı saha maçı"
{"type":"event","title":"Halı Saha Maçı","hasDeadline":true,"deadline":"2026-07-19","startTime":"2026-07-19T21:00","endTime":"2026-07-19T22:00","needsClarification":false,"clarificationQuestion":null}

Kullanıcı: "doktor randevusu"
{"type":"event","title":"Doktor Randevusu","hasDeadline":false,"deadline":null,"startTime":null,"endTime":null,"needsClarification":true,"clarificationQuestion":"Randevu hangi gün ve saatte?"}

Kullanıcı: "araba kiralama numarası 555-1234"
{"type":"note","title":"Araba Kiralama Numarası","hasDeadline":false,"deadline":null,"startTime":null,"endTime":null,"needsClarification":false,"clarificationQuestion":null}

Şimdi sadece JSON döndür, başka hiçbir açıklama ekleme:
""".trimIndent()
        }
    }



    // Backward-compat fallback (no date context — avoid using this)
    val SYSTEM_PROMPT: String get() = buildSystemPrompt(
        currentDateTimeInfo = "bilinmiyor",
        calendarBlock = "",
        exampleDate = "2026-01-01",
        language = "tr"
    )

    fun parse(raw: String): LlmIntentJson? {
        val type = extractStringValue(raw, "type") ?: "task"
        val title = extractStringValue(raw, "title") ?: "Yeni Görev"
        val hasDeadline = extractBooleanValue(raw, "hasDeadline") ?: false
        val deadline = extractStringValue(raw, "deadline")
        val time = extractStringValue(raw, "time")
        val startTime = extractStringValue(raw, "startTime")
        val endTime = extractStringValue(raw, "endTime")
        val needsClarification = extractBooleanValue(raw, "needsClarification") ?: false
        val clarificationQuestion = extractStringValue(raw, "clarificationQuestion")

        return LlmIntentJson(
            type = type,
            title = title,
            hasDeadline = hasDeadline,
            deadline = deadline,
            time = time,
            startTime = startTime,
            endTime = endTime,
            needsClarification = needsClarification,
            clarificationQuestion = clarificationQuestion
        )
    }



    private fun extractStringValue(json: String, key: String): String? {
        val keyIdx = json.indexOf(key)
        if (keyIdx == -1) return null
        
        val colonIdx = json.indexOf(":", keyIdx)
        if (colonIdx == -1) return null
        
        // Read the value part (until comma, closing brace, or end of string)
        var endIdx = colonIdx + 1
        var inQuotes = false
        while (endIdx < json.length) {
            val c = json[endIdx]
            if (c == '"') {
                inQuotes = !inQuotes
            } else if (!inQuotes && (c == ',' || c == '}')) {
                break
            }
            endIdx++
        }
        
        val valueStr = json.substring(colonIdx + 1, endIdx).trim()
        if (valueStr == "null" || valueStr.isEmpty()) return null
        
        // Remove surrounding quotes if they exist
        if (valueStr.startsWith("\"") && valueStr.endsWith("\"")) {
            val unquoted = valueStr.substring(1, valueStr.length - 1)
            return unquoted.takeIf { it.isNotEmpty() && it != "null" }
        }
        return valueStr
    }

    private fun extractBooleanValue(json: String, key: String): Boolean? {
        val valueStr = extractStringValue(json, key) ?: return null
        return valueStr.equals("true", ignoreCase = true)
    }
}