package com.yusufteker.pulse.feature.home.data.api

import com.yusufteker.pulse.shared.api.FeedResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import com.yusufteker.pulse.shared.api.CreatePostRequest

class FeedApi(private val httpClient: HttpClient) {
    suspend fun getPosts(page: Int, limit: Int, topic: String?): Result<FeedResponse> {
        return try {
            val response: FeedResponse = httpClient.get("posts") {
                parameter("page", page)
                parameter("limit", limit)
                topic?.let { parameter("topic", it) }
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPost(request: CreatePostRequest): Result<Unit> {
        return try {
            httpClient.post("posts") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleBookmark(postId: String): Result<Unit> {
        return try {
            httpClient.post("posts/$postId/bookmark")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
