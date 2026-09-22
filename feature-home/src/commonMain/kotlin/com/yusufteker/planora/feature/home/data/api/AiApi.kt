package com.yusufteker.planora.feature.home.data.api

import com.yusufteker.planora.shared.ai.AiChatServerRequest
import com.yusufteker.planora.shared.ai.AiChatServerResponse
import com.yusufteker.planora.shared.ai.AiQuotaDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Planora Sunucu Yapay Zeka (AI) ve Kota API İstemcisi.
 */
open class AiApi(private val httpClient: HttpClient? = null) {

    /**
     * Sunucuya AI sohbet isteği gönderir.
     * Kota kontrolü ve Gemini API işlemi sunucu tarafında yürütülür.
     */
    open suspend fun sendChatMessage(request: AiChatServerRequest): Result<AiChatServerResponse> = runCatching {
        val client = httpClient ?: error("HttpClient is null")
        client.post("ai/chat") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    /**
     * Kullanıcının güncel günlük ve haftalık AI kullanım kotasını çeker.
     */
    open suspend fun getQuota(): Result<AiQuotaDto> = runCatching {
        val client = httpClient ?: error("HttpClient is null")
        client.get("ai/quota").body()
    }
}
