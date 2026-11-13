package com.yusufteker.planora.server.service

import com.yusufteker.planora.server.AppConfig
import kotlinx.serialization.json.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Shared OpenAI-compatible fallback client.
 * - All users: tries OpenRouter's free router (`openrouter/free`) first when OPENROUTER_API_KEY is configured.
 * - Premium users only: if free fallback also fails or is unconfigured, falls back to DeepSeek paid API (when DEEPSEEK_API_KEY is configured).
 */
object AiFallbackClient {

    private const val OPENROUTER_ENDPOINT = "https://openrouter.ai/api/v1/chat/completions"
    private const val DEEPSEEK_ENDPOINT = "https://api.deepseek.com/chat/completions"

    private val openRouterFreeModels = listOf(
        "openrouter/free"
    )

    private val deepseekPremiumModels = listOf(
        "deepseek-flash"
    )

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Attempts to get a valid JSON object string from fallback providers.
     * Returns null if no fallback key is configured or all fallback models fail.
     */
    fun requestJson(
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int = 1024,
        isPremium: Boolean = false
    ): String? {
        // 1. Free-tier fallback (OpenRouter free router) for all users
        val openRouterKey = AppConfig.openRouterApiKey?.takeIf { it.isNotBlank() }
        if (openRouterKey != null) {
            val openRouterResult = requestOpenAiCompatible(
                providerName = "OpenRouter-Free",
                endpoint = OPENROUTER_ENDPOINT,
                apiKey = openRouterKey,
                models = openRouterFreeModels,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                maxTokens = maxTokens
            )
            if (openRouterResult != null) return openRouterResult
        }

        // 2. Paid fallback (DeepSeek) strictly reserved for Premium users
        if (isPremium) {
            val deepseekKey = AppConfig.deepseekApiKey?.takeIf { it.isNotBlank() }
            if (deepseekKey != null) {
                val deepseekResult = requestOpenAiCompatible(
                    providerName = "DeepSeek-Premium",
                    endpoint = DEEPSEEK_ENDPOINT,
                    apiKey = deepseekKey,
                    models = deepseekPremiumModels,
                    systemPrompt = systemPrompt,
                    userPrompt = userPrompt,
                    maxTokens = maxTokens
                )
                if (deepseekResult != null) return deepseekResult
            }
        }

        return null
    }

    private fun requestOpenAiCompatible(
        providerName: String,
        endpoint: String,
        apiKey: String,
        models: List<String>,
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int
    ): String? {
        // json_object response_format requires the word "JSON" in the messages.
        val effectiveSystemPrompt = if (systemPrompt.contains("json", ignoreCase = true)) {
            systemPrompt
        } else {
            "$systemPrompt\nRespond strictly with a valid JSON object."
        }

        for (model in models) {
            try {
                val requestBody = buildJsonObject {
                    put("model", JsonPrimitive(model))
                    put("temperature", JsonPrimitive(0.1))
                    put("max_tokens", JsonPrimitive(maxTokens))
                    put("response_format", buildJsonObject {
                        put("type", JsonPrimitive("json_object"))
                    })
                    put("messages", buildJsonArray {
                        add(buildJsonObject {
                            put("role", JsonPrimitive("system"))
                            put("content", JsonPrimitive(effectiveSystemPrompt))
                        })
                        add(buildJsonObject {
                            put("role", JsonPrimitive("user"))
                            put("content", JsonPrimitive(userPrompt))
                        })
                    })
                }.toString()

                val httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer $apiKey")
                    .timeout(Duration.ofSeconds(25))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build()

                val response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
                println("[AiFallbackClient] $providerName ($model) HTTP ${response.statusCode()}")

                if (response.statusCode() in 200..299) {
                    val content = extractContent(response.body())
                    if (!content.isNullOrBlank()) {
                        val cleaned = stripMarkdownFences(content)
                        val isValidJson = runCatching { json.parseToJsonElement(cleaned) }.isSuccess
                        if (isValidJson) {
                            println("[AiFallbackClient] $providerName ($model) succeeded.")
                            return cleaned
                        }
                    }
                    println("[AiFallbackClient] $providerName ($model) returned invalid/empty JSON, trying next model.")
                } else {
                    println("[AiFallbackClient] $providerName ($model) error ${response.statusCode()}: ${response.body().take(300)}")
                }
            } catch (e: Exception) {
                println("[AiFallbackClient] $providerName ($model) exception: ${e.message}")
            }
        }
        return null
    }

    private fun extractContent(responseBody: String): String? {
        val root = runCatching { json.parseToJsonElement(responseBody) as? JsonObject }.getOrNull() ?: return null
        val firstChoice = (root["choices"] as? JsonArray)?.getOrNull(0) as? JsonObject ?: return null
        val message = firstChoice["message"] as? JsonObject ?: return null
        return (message["content"] as? JsonPrimitive)?.contentOrNull
    }

    private fun stripMarkdownFences(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```json")) text = text.removePrefix("```json").trim()
        else if (text.startsWith("```")) text = text.removePrefix("```").trim()
        if (text.endsWith("```")) text = text.removeSuffix("```").trim()
        val firstBrace = text.indexOf('{')
        val lastBrace = text.lastIndexOf('}')
        return if (firstBrace != -1 && lastBrace > firstBrace) {
            text.substring(firstBrace, lastBrace + 1)
        } else {
            text
        }
    }
}
