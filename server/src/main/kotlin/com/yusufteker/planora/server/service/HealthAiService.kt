package com.yusufteker.planora.server.service

import com.yusufteker.planora.server.AppConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Serializable
data class MicronutrientDto(
    val name: String,
    val amount: Double,
    val unit: String
)

@Serializable
data class FoodItemDto(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val formattedPortion: String,
    val calories: Int,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val fiberG: Double = 0.0,
    val micronutrients: List<MicronutrientDto> = emptyList()
)

@Serializable
data class FoodAiAnalysisResponse(
    val summary: String,
    val totalCalories: Int,
    val items: List<FoodItemDto>,
    val disclaimer: String = "Besin değerleri tahmini yaklaşık değerlerdir."
)

@Serializable
data class WorkoutAiAnalysisResponse(
    val summary: String,
    val caloriesBurned: Int,
    val durationMinutes: Int,
    val disclaimer: String = "Yakılan kalori kullanıcının kilosu ve aktivite türüne göre tahmini olarak hesaplanmıştır."
)

object HealthAiService {

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val candidateModels = listOf(
        "gemini-2.5-flash",
        "gemini-2.0-flash",
        "gemini-1.5-flash"
    )

    suspend fun analyzeFood(rawText: String, date: String): FoodAiAnalysisResponse {
        val apiKey = AppConfig.geminiApiKey
        if (apiKey.isBlank()) {
            throw IllegalStateException("GEMINI_API_KEY is not configured on server.")
        }

        val systemPrompt = """
        Sen uzman bir diyetisyen ve besin analizi yapay zekasısın.
        Kullanıcının Türkçe olarak girdiği serbest yemek metnini ayrıştıracaksın.
        Tarih: $date
        
        GÖREV:
        1. Kullanıcının metnindeki her bir yiyecek ve içeceği porsiyonuyla birlikte ayrıştır.
        2. Her öğe için:
           - name: Yiyeceğin düzgün Türkçe adı (Örn: "Yumurta", "Tam Buğday Ekmeği", "İnek Sütü")
           - formattedPortion: Miktar ve birimi (Örn: "2 Adet", "1 Dilim (30g)", "50 ml")
           - calories: Kalori (kcal, tam sayı)
           - proteinG: Protein (gram, ondalıklı)
           - carbsG: Karbonhidrat (gram, ondalıklı)
           - fatG: Yağ (gram, ondalıklı)
           - fiberG: Lif (gram, ondalıklı)
           - micronutrients: İçerdiği temel vitamin ve mineraller listesi (name, amount, unit: "mg" veya "µg"). Özellikle C vitamini, D vitamini, B12, Demir, Kalsiyum, Magnezyum belirt.
        3. totalCalories: Tüm öğelerin kalori toplamı.
        4. summary: Günün beslenmesine dair 1-2 cümlelik samimi ve motive edici Türkçe özet.
        5. disclaimer: "Besin değerleri yaklaşık tahminlerdir."
        
        SADECE JSON FORMATINDA CEVAP VER.
        """.trimIndent()

        val requestBody = buildFoodRequestBody(systemPrompt, rawText)
        val responseText = executeGeminiRequest(apiKey, requestBody)
        return parseFoodResponse(responseText)
    }

    suspend fun analyzeWorkout(rawText: String, userWeightKg: Double?, date: String): WorkoutAiAnalysisResponse {
        val apiKey = AppConfig.geminiApiKey
        if (apiKey.isBlank()) {
            throw IllegalStateException("GEMINI_API_KEY is not configured on server.")
        }

        val weightStr = if (userWeightKg != null && userWeightKg > 0) "$userWeightKg kg" else "70 kg (varsayılan)"
        val systemPrompt = """
        Sen uzman bir spor fizyoloğu ve antrenör yapay zekasısın.
        Kullanıcının Türkçe olarak serbest metinle girdiği spor/egzersiz aktivitesini analiz edeceksin.
        Kullanıcı kilosu: $weightStr
        Tarih: $date
        
        GÖREV:
        1. Egzersiz türlerini ve sürelerini metinden çıkar (Örn: "30 dk koşu, 20 dk ağırlık" -> 50 dakika).
        2. Kullanıcının kilosuna ve MET değerlerine göre yakılan tahmini toplam kaloriyi (kcal) hesapla.
        3. durationMinutes: Toplam süre (dakika).
        4. summary: Yapılan sporu özetleyen ve performansı tebrik eden 1-2 cümlelik Türkçe metin.
        5. disclaimer: "Yakılan kalori kullanıcının kilosuna ve aktivite yoğunluğuna göre tahmini olarak hesaplanmıştır."
        
        SADECE JSON FORMATINDA CEVAP VER.
        """.trimIndent()

        val requestBody = buildWorkoutRequestBody(systemPrompt, rawText)
        val responseText = executeGeminiRequest(apiKey, requestBody)
        return parseWorkoutResponse(responseText)
    }

    private fun buildFoodRequestBody(systemPrompt: String, userInput: String): String {
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
                put("maxOutputTokens", JsonPrimitive(1200))
            })
        }
        return root.toString()
    }

    private fun buildWorkoutRequestBody(systemPrompt: String, userInput: String): String {
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
                put("maxOutputTokens", JsonPrimitive(600))
            })
        }
        return root.toString()
    }

    private fun executeGeminiRequest(apiKey: String, requestBody: String): String {
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
                if (httpResponse.statusCode() in 200..299) {
                    val rootJson = json.parseToJsonElement(httpResponse.body()) as? JsonObject
                    val candidates = (rootJson?.get("candidates") as? JsonArray)?.getOrNull(0) as? JsonObject
                    val content = candidates?.get("content") as? JsonObject
                    val parts = content?.get("parts") as? JsonArray
                    val textPart = ((parts?.getOrNull(0) as? JsonObject)?.get("text") as? JsonPrimitive)?.contentOrNull
                    if (!textPart.isNullOrBlank()) {
                        return textPart
                    }
                } else {
                    lastError = "HTTP ${httpResponse.statusCode()}: ${httpResponse.body().take(200)}"
                }
            } catch (e: Exception) {
                lastError = e.message
            }
        }
        throw RuntimeException("Gemini API çağrısı başarısız oldu: $lastError")
    }

    private fun cleanJson(raw: String): String {
        var fixed = raw.trim()
        if (fixed.startsWith("```json")) fixed = fixed.removePrefix("```json").trim()
        if (fixed.startsWith("```")) fixed = fixed.removePrefix("```").trim()
        if (fixed.endsWith("```")) fixed = fixed.removeSuffix("```").trim()
        return fixed
    }

    private fun parseFoodResponse(raw: String): FoodAiAnalysisResponse {
        val cleaned = cleanJson(raw)
        return json.decodeFromString(cleaned)
    }

    private fun parseWorkoutResponse(raw: String): WorkoutAiAnalysisResponse {
        val cleaned = cleanJson(raw)
        return json.decodeFromString(cleaned)
    }
}
