package com.yusufteker.planora.feature.home.data.repository

import com.yusufteker.planora.feature.home.domain.repository.ProfileRepository
import com.yusufteker.planora.shared.api.CalendarAccessGrantDto
import com.yusufteker.planora.shared.api.CalendarAccessRequestDto
import com.yusufteker.planora.shared.api.FollowRequestResponse
import com.yusufteker.planora.shared.api.UserProfileResponse

/**
 * InMemory Fake ProfileRepository implementasyonu.
 * Test senaryolarında profil ve kullanıcı aramalarını taklit eder.
 */
class FakeProfileRepository : ProfileRepository {

    val profilesMap = mutableMapOf<String, UserProfileResponse>()

    override suspend fun updateProfile(name: String, avatarId: String): Result<Unit> = Result.success(Unit)
    override suspend fun uploadProfileImage(imageBytes: ByteArray): Result<String> = Result.success("https://test.com/avatar.jpg")

    override suspend fun getProfile(userId: String): Result<UserProfileResponse> {
        val profile = profilesMap[userId] ?: UserProfileResponse(
            id = userId.toIntOrNull() ?: 1,
            name = "Test User $userId",
            username = "user_$userId",
            email = "user_$userId@test.com",
            avatarId = "avatar_1",
            profileImageUrl = null,
            followersCount = 10,
            followingCount = 5,
            isFollowedByMe = false
        )
        return Result.success(profile)
    }

    override suspend fun toggleFollow(userId: Int): Result<Unit> = Result.success(Unit)
    override suspend fun searchUsers(query: String): Result<List<UserProfileResponse>> = Result.success(emptyList())
    override suspend fun getFollowingUsers(): Result<List<UserProfileResponse>> = Result.success(emptyList())
    override suspend fun getFollowers(): Result<List<UserProfileResponse>> = Result.success(emptyList())
    override suspend fun getFollowRequests(): Result<List<FollowRequestResponse>> = Result.success(emptyList())
    override suspend fun acceptFollowRequest(requestId: Int): Result<Unit> = Result.success(Unit)
    override suspend fun rejectFollowRequest(requestId: Int): Result<Unit> = Result.success(Unit)
    override suspend fun removeFollower(userId: Int): Result<Unit> = Result.success(Unit)
    override suspend fun requestCalendarAccess(userId: Int): Result<Unit> = Result.success(Unit)
    override suspend fun getCalendarAccessRequests(): Result<List<CalendarAccessRequestDto>> = Result.success(emptyList())
    override suspend fun acceptCalendarRequest(requestId: Int): Result<Unit> = Result.success(Unit)
    override suspend fun rejectCalendarRequest(requestId: Int): Result<Unit> = Result.success(Unit)
    override suspend fun getCalendarGrants(): Result<List<CalendarAccessGrantDto>> = Result.success(emptyList())
    override suspend fun revokeCalendarAccess(userId: Int): Result<Unit> = Result.success(Unit)
}
