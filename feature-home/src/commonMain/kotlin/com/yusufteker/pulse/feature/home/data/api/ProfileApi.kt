package com.yusufteker.pulse.feature.home.data.api

import com.yusufteker.pulse.shared.api.UserProfileResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post

class ProfileApi(private val httpClient: HttpClient) {
    suspend fun getProfile(userId: String): Result<UserProfileResponse> {
        return try {
            val response: UserProfileResponse = httpClient.get("users/$userId/profile").body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleFollow(userId: Int): Result<Unit> {
        return try {
            httpClient.post("users/$userId/follow")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
