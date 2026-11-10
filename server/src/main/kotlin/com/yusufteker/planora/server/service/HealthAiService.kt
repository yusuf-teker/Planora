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
    val name: String = "",
    val amount: Double = 0.0,
    val unit: String = ""
)

@Serializable
data class FoodItemDto(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val formattedPortion: String = "",
    val calories: Int = 0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val fiberG: Double = 0.0,
    val micronutrients: List<MicronutrientDto> = emptyList()
)

@Serializable
data class FoodAiAnalysisResponse(
    val summary: String = "",
    val totalCalories: Int = 0,
    val items: List<FoodItemDto> = emptyList(),
    val disclaimer: String = "Besin değerleri tahmini yaklaşık değerlerdir."
)

@Serializable
data class WorkoutAiAnalysisResponse(
    val summary: String = "",
    val caloriesBurned: Int = 0,
    val durationMinutes: Int = 0,
    val disclaimer: String = "Yakılan kalori kullanıcının kilosu ve aktivite türüne göre tahmini olarak hesaplanmıştır."
)

object HealthAiService {

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    // gemini-3.5-flash-lite ve 3.1-flash-lite son derece hızlı, kota sorunu olmayan modellerdir.
    private val candidateModels = listOf(
        "gemini-3.5-flash-lite",
        "gemini-3.1-flash-lite",
        "gemini-2.5-flash",
        "gemini-3.5-flash"
    )

    suspend fun analyzeFood(
        rawText: String,
        date: String,
        imageBase64: String? = null,
        imageMimeType: String? = "image/jpeg"
    ): FoodAiAnalysisResponse {
        val apiKey = AppConfig.healthGeminiApiKey
        if (apiKey.isBlank()) {
            throw IllegalStateException("HEALTH_GEMINI_API_KEY (or GEMINI_API_KEY) is not configured on server.")
        }

        val systemPrompt = """
        Sen uzman bir diyetisyen ve besin analizi yapay zekasısın.
        Kullanıcının Türkçe serbest metin olarak veya yüklediği yemek/tabak/besin etiketi fotoğrafından yemek ve öğünleri ayrıştırıp analiz edeceksin.
        Tarih: $date
        
        GÖREV:
        1. Eğer bir fotoğraf gönderildiyse (tabak veya besin etiketi), fotoğraftaki yiyecekleri, porsiyonları ve kalori/makro değerlerini doğrudan tespit et.
        2. Kullanıcının metnindeki veya fotoğraftaki her bir yiyecek ve içeceği porsiyonuyla birlikte ayrıştır.
        3. Her öğe için:
           - name: Yiyeceğin düzgün Türkçe adı (Örn: "Yumurta", "Tam Buğday Ekmeği", "Zeytin")
           - formattedPortion: Miktar ve birimi (Örn: "2 Adet", "1 Dilim", "5 Adet")
           - calories: Kalori (kcal, tam sayı)
           - proteinG: Protein (gram, ondalıklı sayı)
           - carbsG: Karbonhidrat (gram, ondalıklı sayı)
           - fatG: Yağ (gram, ondalıklı sayı)
           - fiberG: Lif (gram, ondalıklı sayı)
           - micronutrients: Temel vitamin ve mineraller listesi (name, amount, unit: "mg" veya "µg"). Özellikle C vitamini, D vitamini, B12, Demir, Kalsiyum, Magnezyum belirt.
        4. totalCalories: Tüm öğelerin kalori toplamı (tam sayı).
        5. summary: Günün beslenmesine dair 1-2 cümlelik samimi ve motive edici Türkçe özet.
        6. disclaimer: "Besin değerleri tahmini yaklaşık değerlerdir."
        
        SADECE VE SADECE aşağıdaki JSON şemasına uygun geçerli bir JSON objesi döndür:
        {
          "summary": "Örnek özet",
          "totalCalories": 650,
          "items": [
            {
              "name": "Yumurta",
              "formattedPortion": "2 Adet",
              "calories": 140,
              "proteinG": 12.0,
              "carbsG": 1.0,
              "fatG": 10.0,
              "fiberG": 0.0,
              "micronutrients": [
                { "name": "Demir", "amount": 1.8, "unit": "mg" }
              ]
            }
          ],
          "disclaimer": "Besin değerleri tahmini yaklaşık değerlerdir."
        }
        """.trimIndent()

        val promptText = rawText.ifBlank { "Lütfen fotoğraftaki tabağı/besin etiketini analiz et ve besin değerlerini çıkar." }
        val requestBody = buildFoodRequestBody(systemPrompt, promptText, imageBase64, imageMimeType)
        val responseText = executeGeminiRequest(apiKey, requestBody)
        return parseFoodResponse(responseText)
    }

    suspend fun analyzeWorkout(rawText: String, userWeightKg: Double?, date: String): WorkoutAiAnalysisResponse {
        val apiKey = AppConfig.healthGeminiApiKey
        if (apiKey.isBlank()) {
            throw IllegalStateException("HEALTH_GEMINI_API_KEY (or GEMINI_API_KEY) is not configured on server.")
        }

        val weightStr = if (userWeightKg != null && userWeightKg > 0) "$userWeightKg kg" else "70 kg (varsayılan)"
        val systemPrompt = """
        Sen uzman bir spor fizyoloğu ve antrenör yapay zekasısın.
        Kullanıcının Türkçe serbest metinle girdiği spor/egzersiz aktivitesini analiz edeceksin.
        Kullanıcı kilosu: $weightStr
        Tarih: $date
        
        GÖREV:
        1. Egzersiz türlerini ve sürelerini metinden çıkar (Örn: "30 dk koşu, 20 dk ağırlık" -> 50 dakika).
        2. Kullanıcının kilosuna ve MET değerlerine göre yakılan tahmini toplam kaloriyi (kcal) hesapla.
        3. durationMinutes: Toplam süre (dakika, tam sayı).
        4. summary: Yapılan sporu özetleyen ve performansı tebrik eden 1-2 cümlelik Türkçe metin.
        5. disclaimer: "Yakılan kalori kullanıcının kilosuna ve aktivite yoğunluğuna göre tahmini olarak hesaplanmıştır."
        
        SADECE VE SADECE aşağıdaki JSON şemasına uygun geçerli bir JSON objesi döndür:
        {
          "summary": "Tebrikler, harika bir antrenman!",
          "caloriesBurned": 450,
          "durationMinutes": 60,
          "disclaimer": "Yakılan kalori kullanıcının kilosuna ve aktivite yoğunluğuna göre tahmini olarak hesaplanmıştır."
        }
        """.trimIndent()

        val requestBody = buildWorkoutRequestBody(systemPrompt, rawText)
        val responseText = executeGeminiRequest(apiKey, requestBody)
        return parseWorkoutResponse(responseText)
    }

    private fun buildFoodRequestBody(
        systemPrompt: String,
        userInput: String,
        imageBase64: String? = null,
        imageMimeType: String? = "image/jpeg"
    ): String {
        val root = buildJsonObject {
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("parts", buildJsonArray {
                        if (!imageBase64.isNullOrBlank()) {
                            add(buildJsonObject {
                                put("inlineData", buildJsonObject {
                                    put("mimeType", JsonPrimitive(imageMimeType ?: "image/jpeg"))
                                    put("data", JsonPrimitive(imageBase64))
                                })
                            })
                        }
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
                put("maxOutputTokens", JsonPrimitive(4096))
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
                put("maxOutputTokens", JsonPrimitive(1024))
            })
        }
        return root.toString()
    }

    /**
     * thinkingConfig yalnızca gemini-2.5 modellerinde desteklenir;
     * 3.x modeller bu alanla 400 INVALID_ARGUMENT dönebilir.
     */
    private fun bodyForModel(model: String, requestBody: String): String {
        if (!model.startsWith("gemini-2.5")) return requestBody
        val root = json.parseToJsonElement(requestBody).jsonObject
        val genConfig = root["generationConfig"]?.jsonObject ?: return requestBody
        val newGenConfig = JsonObject(genConfig + ("thinkingConfig" to buildJsonObject {
            put("thinkingBudget", JsonPrimitive(0))
        }))
        return JsonObject(root + ("generationConfig" to newGenConfig)).toString()
    }

    private fun executeGeminiRequest(apiKey: String, requestBody: String): String {
        var lastError: String? = null
        for (model in candidateModels) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            try {
                println("[HealthAiService] Calling Gemini model '$model'...")
                val httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(25))
                    .POST(HttpRequest.BodyPublishers.ofString(bodyForModel(model, requestBody)))
                    .build()

                val httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
                println("[HealthAiService] Gemini ($model) status: ${httpResponse.statusCode()}")

                if (httpResponse.statusCode() in 200..299) {
                    val rootJson = json.parseToJsonElement(httpResponse.body()) as? JsonObject
                    val candidates = (rootJson?.get("candidates") as? JsonArray)?.getOrNull(0) as? JsonObject
                    val content = candidates?.get("content") as? JsonObject
                    val parts = content?.get("parts") as? JsonArray
                    val textPart = ((parts?.getOrNull(0) as? JsonObject)?.get("text") as? JsonPrimitive)?.contentOrNull
                    val finishReason = (candidates?.get("finishReason") as? JsonPrimitive)?.contentOrNull

                    if (!textPart.isNullOrBlank() && finishReason != "MAX_TOKENS") {
                        val cleaned = cleanJson(textPart)
                        val isValid = runCatching { json.parseToJsonElement(cleaned) }.isSuccess
                        if (isValid) {
                            println("[HealthAiService] Gemini ($model) response parsed successfully.")
                            return textPart
                        }
                    }
                    println("[HealthAiService] Gemini ($model) returned unusable output (finishReason=$finishReason), trying next model.")
                    lastError = "Model $model yarım/bozuk yanıt döndürdü (finishReason=$finishReason)"
                } else {
                    val bodySnippet = httpResponse.body().take(300)
                    println("[HealthAiService] Gemini ($model) failed with HTTP ${httpResponse.statusCode()}: $bodySnippet")
                    lastError = "HTTP ${httpResponse.statusCode()}: $bodySnippet"
                }
            } catch (e: Exception) {
                println("[HealthAiService] Gemini ($model) exception: ${e.message}")
                lastError = e.message
            }
        }
        throw RuntimeException("Gemini API çağrısı başarısız oldu: $lastError")
    }

    private fun cleanJson(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```json")) {
            text = text.removePrefix("```json").trim()
        } else if (text.startsWith("```")) {
            text = text.removePrefix("```").trim()
        }
        if (text.endsWith("```")) {
            text = text.removeSuffix("```").trim()
        }
        val firstBrace = text.indexOf('{')
        val lastBrace = text.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return text.substring(firstBrace, lastBrace + 1)
        }
        return text
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
