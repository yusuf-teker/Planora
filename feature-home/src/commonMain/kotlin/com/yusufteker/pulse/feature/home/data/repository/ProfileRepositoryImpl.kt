package com.yusufteker.pulse.feature.home.data.repository

import com.yusufteker.pulse.core.preferences.SessionPreferences
import com.yusufteker.pulse.feature.home.domain.repository.ProfileRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import com.yusufteker.pulse.feature.home.data.api.ProfileApi
import com.yusufteker.pulse.feature.home.data.api.CalendarApi
import com.yusufteker.pulse.shared.api.CalendarAccessGrantDto
import com.yusufteker.pulse.shared.api.CalendarAccessRequestDto

class ProfileRepositoryImpl(
    private val httpClient: HttpClient,
    private val sessionPreferences: SessionPreferences
) : ProfileRepository {

    private val profileApi = ProfileApi(httpClient)
    private val calendarApi = CalendarApi(httpClient)

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
        return profileApi.getProfile(userId)
    }

    override suspend fun toggleFollow(userId: Int): Result<Unit> {
        return profileApi.toggleFollow(userId)
    }

    override suspend fun searchUsers(query: String): Result<List<com.yusufteker.pulse.shared.api.UserProfileResponse>> {
        return profileApi.searchUsers(query)
    }

    override suspend fun getFollowingUsers(): Result<List<com.yusufteker.pulse.shared.api.UserProfileResponse>> {
        return profileApi.getFollowingUsers()
    }

    override suspend fun getFollowers(): Result<List<com.yusufteker.pulse.shared.api.UserProfileResponse>> {
        return profileApi.getFollowers()
    }

    override suspend fun getFollowRequests(): Result<List<com.yusufteker.pulse.shared.api.FollowRequestResponse>> {
        return profileApi.getFollowRequests()
    }

    override suspend fun acceptFollowRequest(requestId: Int): Result<Unit> {
        return profileApi.acceptFollowRequest(requestId)
    }

    override suspend fun rejectFollowRequest(requestId: Int): Result<Unit> {
        return profileApi.rejectFollowRequest(requestId)
    }

    override suspend fun removeFollower(userId: Int): Result<Unit> {
        return profileApi.removeFollower(userId)
    }

    override suspend fun requestCalendarAccess(userId: Int): Result<Unit> {
        return calendarApi.requestCalendarAccess(userId)
    }

    override suspend fun getCalendarAccessRequests(): Result<List<CalendarAccessRequestDto>> {
        return calendarApi.getCalendarAccessRequests()
    }

    override suspend fun acceptCalendarRequest(requestId: Int): Result<Unit> {
        return calendarApi.acceptCalendarRequest(requestId)
    }

    override suspend fun rejectCalendarRequest(requestId: Int): Result<Unit> {
        return calendarApi.rejectCalendarRequest(requestId)
    }

    override suspend fun getCalendarGrants(): Result<List<CalendarAccessGrantDto>> {
        return calendarApi.getCalendarGrants()
    }

    override suspend fun revokeCalendarAccess(userId: Int): Result<Unit> {
        return calendarApi.revokeCalendarAccess(userId)
    }
}
