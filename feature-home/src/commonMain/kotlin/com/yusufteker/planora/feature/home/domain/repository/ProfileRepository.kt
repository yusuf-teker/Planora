package com.yusufteker.planora.feature.home.domain.repository

import com.yusufteker.planora.shared.api.UserProfileResponse

/**
 * Domain Layer interface for Profile operations.
 */
interface ProfileRepository {
    /**
     * Updates the user's profile on the server.
     */
    suspend fun updateProfile(name: String, avatarId: String): Result<Unit>

    suspend fun uploadProfileImage(imageBytes: ByteArray): Result<String>

    suspend fun getProfile(userId: String): Result<UserProfileResponse>

    suspend fun toggleFollow(userId: Int): Result<Unit>

    suspend fun searchUsers(query: String): Result<List<UserProfileResponse>>
    
    suspend fun getFollowingUsers(): Result<List<UserProfileResponse>>
    
    suspend fun getFollowers(): Result<List<UserProfileResponse>>
    
    suspend fun getFollowRequests(): Result<List<com.yusufteker.planora.shared.api.FollowRequestResponse>>
    
    suspend fun acceptFollowRequest(requestId: Int): Result<Unit>
    
    suspend fun rejectFollowRequest(requestId: Int): Result<Unit>
    
    suspend fun removeFollower(userId: Int): Result<Unit>

    suspend fun requestCalendarAccess(userId: Int): Result<Unit>
    suspend fun getCalendarAccessRequests(): Result<List<com.yusufteker.planora.shared.api.CalendarAccessRequestDto>>
    suspend fun acceptCalendarRequest(requestId: Int): Result<Unit>
    suspend fun rejectCalendarRequest(requestId: Int): Result<Unit>
    suspend fun getCalendarGrants(): Result<List<com.yusufteker.planora.shared.api.CalendarAccessGrantDto>>
    suspend fun revokeCalendarAccess(userId: Int): Result<Unit>
}
