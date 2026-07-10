package com.yusufteker.pulse.feature.home.data.repository

import com.yusufteker.pulse.core.preferences.SessionPreferences
import com.yusufteker.pulse.feature.home.domain.repository.ProfileRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.put
import io.ktor.client.request.setBody

class ProfileRepositoryImpl(
    private val httpClient: HttpClient,
    private val sessionPreferences: SessionPreferences
) : ProfileRepository {

    override suspend fun updateProfile(name: String, avatarId: String): Result<Unit> {
        return try {
            httpClient.put("auth/profile") {
                setBody(com.yusufteker.pulse.shared.api.UpdateProfileRequest(name, avatarId))
            }
            // Update local DataStore upon successful server update
            sessionPreferences.updateProfileData(name, avatarId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProfile(userId: String): Result<com.yusufteker.pulse.shared.api.UserProfileResponse> {
        return com.yusufteker.pulse.feature.home.data.api.ProfileApi(httpClient).getProfile(userId)
    }

    override suspend fun toggleFollow(userId: Int): Result<Unit> {
        return com.yusufteker.pulse.feature.home.data.api.ProfileApi(httpClient).toggleFollow(userId)
    }

    override suspend fun searchUsers(query: String): Result<List<com.yusufteker.pulse.shared.api.UserProfileResponse>> {
        return com.yusufteker.pulse.feature.home.data.api.ProfileApi(httpClient).searchUsers(query)
    }

    override suspend fun getFollowingUsers(): Result<List<com.yusufteker.pulse.shared.api.UserProfileResponse>> {
        return com.yusufteker.pulse.feature.home.data.api.ProfileApi(httpClient).getFollowingUsers()
    }

    override suspend fun getFollowers(): Result<List<com.yusufteker.pulse.shared.api.UserProfileResponse>> {
        return com.yusufteker.pulse.feature.home.data.api.ProfileApi(httpClient).getFollowers()
    }

    override suspend fun getFollowRequests(): Result<List<com.yusufteker.pulse.shared.api.FollowRequestResponse>> {
        return com.yusufteker.pulse.feature.home.data.api.ProfileApi(httpClient).getFollowRequests()
    }

    override suspend fun acceptFollowRequest(requestId: Int): Result<Unit> {
        return com.yusufteker.pulse.feature.home.data.api.ProfileApi(httpClient).acceptFollowRequest(requestId)
    }

    override suspend fun rejectFollowRequest(requestId: Int): Result<Unit> {
        return com.yusufteker.pulse.feature.home.data.api.ProfileApi(httpClient).rejectFollowRequest(requestId)
    }

    override suspend fun removeFollower(userId: Int): Result<Unit> {
        return com.yusufteker.pulse.feature.home.data.api.ProfileApi(httpClient).removeFollower(userId)
    }
}
