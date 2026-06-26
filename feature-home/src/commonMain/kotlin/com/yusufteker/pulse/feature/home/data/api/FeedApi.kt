package com.yusufteker.pulse.feature.home.data.api

import com.yusufteker.pulse.shared.api.FeedResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class FeedApi(private val httpClient: HttpClient) {
    suspend fun getPosts(page: Int, limit: Int): Result<FeedResponse> {
        return try {
            val response: FeedResponse = httpClient.get("posts") {
                parameter("page", page)
                parameter("limit", limit)
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
