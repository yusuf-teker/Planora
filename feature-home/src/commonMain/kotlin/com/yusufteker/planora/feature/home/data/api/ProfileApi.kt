package com.yusufteker.planora.feature.home.data.api

import com.yusufteker.planora.shared.api.UserProfileResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.parameter
import com.yusufteker.planora.shared.api.SearchUsersResponse

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

    suspend fun searchUsers(query: String): Result<List<UserProfileResponse>> {
        return try {
            val response: SearchUsersResponse = httpClient.get("users/search") {
                parameter("q", query)
            }.body()
            Result.success(response.users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFollowingUsers(): Result<List<UserProfileResponse>> {
        return try {
            val response: List<UserProfileResponse> = httpClient.get("users/following").body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFollowers(): Result<List<UserProfileResponse>> {
        return try {
            val response: List<UserProfileResponse> = httpClient.get("users/followers").body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFollowRequests(): Result<List<com.yusufteker.planora.shared.api.FollowRequestResponse>> {
        return try {
            val response: List<com.yusufteker.planora.shared.api.FollowRequestResponse> = httpClient.get("users/follow-requests").body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptFollowRequest(requestId: Int): Result<Unit> {
        return try {
            httpClient.post("users/follow-requests/$requestId/accept")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectFollowRequest(requestId: Int): Result<Unit> {
        return try {
            httpClient.post("users/follow-requests/$requestId/reject")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFollower(userId: Int): Result<Unit> {
        return try {
            httpClient.post("users/$userId/remove-follower")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
