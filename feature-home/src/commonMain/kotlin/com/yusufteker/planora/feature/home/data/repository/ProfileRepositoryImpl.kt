package com.yusufteker.planora.feature.home.data.repository

import com.yusufteker.planora.core.preferences.SessionPreferences
import com.yusufteker.planora.feature.home.domain.repository.ProfileRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import com.yusufteker.planora.feature.home.data.api.ProfileApi
import com.yusufteker.planora.feature.home.data.api.CalendarApi
import com.yusufteker.planora.shared.api.CalendarAccessGrantDto
import com.yusufteker.planora.shared.api.CalendarAccessRequestDto
import io.ktor.client.request.post
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.client.call.body

class ProfileRepositoryImpl(
    private val httpClient: HttpClient,
    private val sessionPreferences: SessionPreferences
) : ProfileRepository {

    private val profileApi = ProfileApi(httpClient)
    private val calendarApi = CalendarApi(httpClient)

    override suspend fun updateProfile(name: String, avatarId: String): Result<Unit> {
        return try {
            httpClient.put("auth/profile") {
                setBody(com.yusufteker.planora.shared.api.UpdateProfileRequest(name, avatarId))
            }
            // Update local DataStore upon successful server update
            val currentProfileImageUrl = sessionPreferences.getUserProfileImageUrl()
            sessionPreferences.updateProfileData(name, avatarId, currentProfileImageUrl)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadProfileImage(imageBytes: ByteArray): Result<String> {
        return try {
            val response: Map<String, String> = httpClient.post("users/profile-image") {
                setBody(MultiPartFormDataContent(
                    formData {
                        append("image", imageBytes, Headers.build {
                            append(HttpHeaders.ContentType, "image/jpeg")
                            append(HttpHeaders.ContentDisposition, "filename=\"profile.jpg\"")
                        })
                    }
                ))
            }.body()
            
            val secureUrl = response["profileImageUrl"]
            
            // Update local DataStore upon successful upload
            val name = sessionPreferences.getUserName() ?: ""
            val avatarId = sessionPreferences.getUserAvatar() ?: "default"
            sessionPreferences.updateProfileData(name, avatarId, secureUrl)
            
            if (secureUrl != null) {
                Result.success(secureUrl)
            } else {
                Result.failure(Exception("Failed to upload image"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProfile(userId: String): Result<com.yusufteker.planora.shared.api.UserProfileResponse> {
        return profileApi.getProfile(userId)
    }

    override suspend fun toggleFollow(userId: Int): Result<Unit> {
        return profileApi.toggleFollow(userId)
    }

    override suspend fun searchUsers(query: String): Result<List<com.yusufteker.planora.shared.api.UserProfileResponse>> {
        return profileApi.searchUsers(query)
    }

    override suspend fun getFollowingUsers(): Result<List<com.yusufteker.planora.shared.api.UserProfileResponse>> {
        return profileApi.getFollowingUsers()
    }

    override suspend fun getFollowers(): Result<List<com.yusufteker.planora.shared.api.UserProfileResponse>> {
        return profileApi.getFollowers()
    }

    override suspend fun getFollowRequests(): Result<List<com.yusufteker.planora.shared.api.FollowRequestResponse>> {
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
