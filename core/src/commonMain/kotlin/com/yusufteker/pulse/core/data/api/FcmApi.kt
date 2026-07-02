package com.yusufteker.pulse.core.data.api

import com.yusufteker.pulse.shared.api.RegisterFcmTokenRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class FcmApi(private val httpClient: HttpClient) {
    suspend fun registerToken(token: String, platform: String): Result<Unit> {
        return try {
            httpClient.post("fcm/register") {
                contentType(ContentType.Application.Json)
                setBody(RegisterFcmTokenRequest(token = token, platform = platform))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
