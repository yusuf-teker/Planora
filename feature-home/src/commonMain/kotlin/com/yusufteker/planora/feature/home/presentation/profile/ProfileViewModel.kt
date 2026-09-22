package com.yusufteker.planora.feature.home.presentation.profile

import androidx.lifecycle.viewModelScope
import com.yusufteker.planora.core.base.BaseViewModel
import com.yusufteker.planora.core.preferences.SessionPreferences
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.combine
import com.yusufteker.planora.core.database.PlanoraDatabase
import com.yusufteker.planora.core.database.clearAll

import com.yusufteker.planora.feature.home.domain.repository.ProfileRepository
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import planora.core.generated.resources.Res
import planora.core.generated.resources.*

class ProfileViewModel(
    private val sessionPreferences: SessionPreferences,
    private val profileRepository: ProfileRepository,
    private val database: PlanoraDatabase
) : BaseViewModel<ProfileState, ProfileEvent, ProfileEffect>(
    initialState = ProfileState()
) {

    init {
        // Flow olarak dinle: giriş/çıkış yapılınca DataStore değişir,
        // combine her iki değeri birlikte yakalar ve UI otomatik güncellenir.
        launch {
            val flow1 = combine(
                sessionPreferences.userIdFlow,
                sessionPreferences.userNameFlow,
                sessionPreferences.userHandleFlow,
                sessionPreferences.userAvatarFlow
            ) { id, name, username, avatar -> ProfileDataPart1(id, name, username, avatar) }
            
            val flow2 = combine(
                sessionPreferences.userProfileImageUrlFlow,
                sessionPreferences.followersCountFlow,
                sessionPreferences.followingCountFlow
            ) { url, followers, following -> ProfileDataPart2(url, followers, following) }
            
            combine(flow1, flow2) { part1, part2 ->
                ProfileData(part1.userId, part1.name, part1.username, part1.avatarId, part2.profileImageUrl, part2.followers, part2.following)
            }
            .collect { data ->
                val isLoggedIn = data.userId != null
                setState { copy(isLoggedIn = isLoggedIn) }
                Napier.d(tag = "Screen", message = { "DataStore Flow geldi → isim='${data.name}', handle='${data.username}', avatar='${data.avatarId}', isLoggedIn=$isLoggedIn" })
                // Only update from datastore if it's my profile and hasn't been loaded from network yet
                if (state.value.isMyProfile && state.value.profileId == null) {
                    val guestName = getString(Res.string.profile_guest)
                    setState {
                        copy(
                            name = data.name ?: guestName,
                            username = data.username ?: "",
                            avatarId = data.avatarId ?: "avatar_1",
                            profileImageUrl = data.profileImageUrl,
                            followersCount = data.followersCount,
                            followingCount = data.followingCount
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            sessionPreferences.isPremiumFlow.collect { isPrem ->
                if (state.value.isMyProfile) {
                    setState { copy(isPremium = isPrem) }
                }
            }
        }

        viewModelScope.launch {
            sessionPreferences.premiumUntilFlow.collect { until ->
                if (state.value.isMyProfile) {
                    setState { copy(premiumUntil = until) }
                }
            }
        }
    }
    
    private data class ProfileDataPart1(
        val userId: String?,
        val name: String?,
        val username: String?,
        val avatarId: String?
    )
    
    private data class ProfileDataPart2(
        val profileImageUrl: String?,
        val followers: Int,
        val following: Int
    )

    private data class ProfileData(
        val userId: String?,
        val name: String?,
        val username: String?,
        val avatarId: String?,
        val profileImageUrl: String?,
        val followersCount: Int,
        val followingCount: Int
    )

    override fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.BackClicked -> {
                setEffect(ProfileEffect.NavigateBack)
            }

            is ProfileEvent.EditProfileClicked -> {
                setState { copy(isEditing = true) }
            }

            is ProfileEvent.NameChanged -> {
                setState { copy(name = event.name) }
            }

            is ProfileEvent.AvatarSelected -> {
                setState { copy(avatarId = event.avatarId) }
            }
            
            is ProfileEvent.ProfileImageSelected -> {
                launch {
                    setState { copy(isUploadingImage = true) }
                    val result = profileRepository.uploadProfileImage(event.imageBytes)
                    result.onSuccess { url ->
                        setState { copy(profileImageUrl = url, isUploadingImage = false) }
                        Napier.d(tag = "Screen") { "Profile image uploaded: $url" }
                    }.onFailure { e ->
                        setState { copy(isUploadingImage = false) }
                        Napier.e(tag = "Screen", throwable = e) { "Failed to upload profile image" }
                    }
                }
            }

            is ProfileEvent.SaveClicked -> {
                launch {
                    val result = profileRepository.updateProfile(state.value.name, state.value.avatarId)
                    if (result.isSuccess) {
                        setState { copy(isEditing = false) }
                        Napier.d(tag = "Screen", message = { "Profil backend'e kaydedildi." })
                    } else {
                        Napier.e(tag = "Screen", message = { "Profil kaydedilemedi: ${result.exceptionOrNull()?.message}" })
                    }
                }
            }
            
            is ProfileEvent.LoadProfile -> {
                val userIdToLoad = event.userId
                val isSameProfile = state.value.profileId == userIdToLoad
                val isMyProfile = userIdToLoad == null
                
                setState { 
                    copy(
                        isLoading = true, 
                        profileId = userIdToLoad,
                        isMyProfile = isMyProfile,
                        // Only clear if it's a different profile and not my profile
                        name = if (isSameProfile || isMyProfile) name else "",
                        avatarId = if (isSameProfile || isMyProfile) avatarId else "",
                        profileImageUrl = if (isSameProfile || isMyProfile) profileImageUrl else null,
                        username = if (isSameProfile || isMyProfile) username else "",
                        followersCount = if (isSameProfile) followersCount else 0,
                        followingCount = if (isSameProfile) followingCount else 0,
                        postsCount = if (isSameProfile) postsCount else 0,
                        isFollowedByMe = if (isSameProfile) isFollowedByMe else false
                    ) 
                }
                launch {
                    if (isMyProfile) {
                        val localName = sessionPreferences.getUserName()
                        val localHandle = sessionPreferences.getUserHandle()
                        val localAvatar = sessionPreferences.getUserAvatar()
                        val localProfileImageUrl = sessionPreferences.getUserProfileImageUrl()
                        if (localName != null) {
                            setState {
                                copy(
                                    name = localName,
                                    username = localHandle ?: username,
                                    avatarId = localAvatar ?: "avatar_1",
                                    profileImageUrl = localProfileImageUrl
                                )
                            }
                        } else if (!state.value.isLoggedIn) {
                            val guestName = getString(Res.string.profile_guest)
                            setState {
                                copy(
                                    name = guestName,
                                    avatarId = "avatar_1",
                                    isLoading = false
                                )
                            }
                            return@launch
                        }
                    }
                    
                    val targetId = userIdToLoad?.toString() ?: "me"
                    
                    val result = profileRepository.getProfile(targetId)
                    result.onSuccess { profile ->
                        if (isMyProfile) {
                            launch {
                                sessionPreferences.saveUserProfile(
                                    userId = profile.id.toString(),
                                    name = profile.name,
                                    avatarId = profile.avatarId,
                                    profileImageUrl = profile.profileImageUrl,
                                    followersCount = profile.followersCount,
                                    followingCount = profile.followingCount,
                                    username = profile.username
                                )
                                sessionPreferences.setPremium(profile.isPremium, profile.premiumUntil)
                            }
                        }
                        setState {
                            copy(
                                name = profile.name,
                                username = profile.username,
                                avatarId = profile.avatarId,
                                profileImageUrl = profile.profileImageUrl,
                                followersCount = profile.followersCount,
                                followingCount = profile.followingCount,
                                postsCount = profile.postsCount,
                                isFollowedByMe = profile.isFollowedByMe,
                                followRequestStatus = profile.followRequestStatus,
                                calendarAccessStatus = profile.calendarAccessStatus,
                                isMyProfile = isMyProfile,
                                isPremium = profile.isPremium,
                                premiumUntil = profile.premiumUntil,
                                isLoading = false
                            )
                        }
                    }.onFailure {
                        setState { copy(isLoading = false) }
                    }
                    
                    // Profil yüklendikten sonra eğer benim profilimse takip isteklerini de çek
                    if (isMyProfile) {
                        loadPendingRequests()
                        loadCalendarRequestsAndGrants()
                    }
                }
            }

            is ProfileEvent.ToggleFollowClicked -> {
                toggleFollow()
            }
            
            is ProfileEvent.NavigateToFollowList -> {
                setEffect(ProfileEffect.NavigateToFollowList(event.tab))
            }
            
            is ProfileEvent.LogoutClicked -> {
                launch {
                    database.planoraDatabaseQueries.clearAll()
                    sessionPreferences.clearSession()
                    setEffect(ProfileEffect.NavigateToLogin)
                }
            }
            
            is ProfileEvent.AcceptRequestClicked -> {
                acceptRequest(event.requestId)
            }
            
            is ProfileEvent.RejectRequestClicked -> {
                rejectRequest(event.requestId)
            }

            is ProfileEvent.RequestCalendarAccessClicked -> {
                val targetId = state.value.profileId ?: return
                val currentStatus = state.value.calendarAccessStatus
                val newStatus = if (currentStatus == "PENDING" || currentStatus == "ACCEPTED") null else "PENDING"
                setState { copy(calendarAccessStatus = newStatus) }
                launch {
                    val result = profileRepository.requestCalendarAccess(targetId)
                    result.onFailure {
                        setState { copy(calendarAccessStatus = currentStatus) }
                    }
                }
            }

            is ProfileEvent.RevokeCalendarAccessClicked -> {
                val targetId = state.value.profileId ?: return
                val currentStatus = state.value.calendarAccessStatus
                setState { copy(calendarAccessStatus = null) }
                launch {
                    val result = profileRepository.requestCalendarAccess(targetId)
                    result.onFailure {
                        setState { copy(calendarAccessStatus = currentStatus) }
                    }
                }
            }

            is ProfileEvent.AcceptCalendarRequestClicked -> {
                launch {
                    profileRepository.acceptCalendarRequest(event.requestId).onSuccess {
                        loadCalendarRequestsAndGrants()
                    }
                }
            }

            is ProfileEvent.RejectCalendarRequestClicked -> {
                launch {
                    profileRepository.rejectCalendarRequest(event.requestId).onSuccess {
                        loadCalendarRequestsAndGrants()
                    }
                }
            }

            is ProfileEvent.RevokeCalendarGrantClicked -> {
                launch {
                    profileRepository.revokeCalendarAccess(event.userId).onSuccess {
                        loadCalendarRequestsAndGrants()
                    }
                }
            }
        }
    }
    
    private fun loadPendingRequests() {
        launch {
            setState { copy(isLoadingRequests = true) }
            profileRepository.getFollowRequests()
                .onSuccess { requests ->
                    setState { copy(pendingRequests = requests, isLoadingRequests = false) }
                    sessionPreferences.updatePendingFollowRequestsCount(requests.size)
                }
                .onFailure {
                    setState { copy(isLoadingRequests = false) }
                    Napier.e(tag = "ProfileList") { "Failed to load requests: ${it.message}" }
                }
        }
    }

    private fun loadCalendarRequestsAndGrants() {
        launch {
            profileRepository.getCalendarAccessRequests().onSuccess { requests ->
                setState { copy(pendingCalendarRequests = requests) }
                sessionPreferences.updatePendingCalendarRequestsCount(requests.size)
            }
            profileRepository.getCalendarGrants().onSuccess { grants ->
                setState { copy(calendarGrants = grants) }
            }
        }
    }

    private fun acceptRequest(requestId: Int) {
        val originalRequests = state.value.pendingRequests
        setState { copy(pendingRequests = pendingRequests.filter { it.id != requestId }) }

        launch {
            sessionPreferences.updateFollowCounts(followersDelta = 1, followingDelta = 0)
            sessionPreferences.updatePendingFollowRequestsCount(state.value.pendingRequests.size)
            
            profileRepository.acceptFollowRequest(requestId)
                .onFailure {
                    // Revert
                    setState { copy(pendingRequests = originalRequests) }
                    sessionPreferences.updateFollowCounts(followersDelta = -1, followingDelta = 0)
                    sessionPreferences.updatePendingFollowRequestsCount(originalRequests.size)
                }
        }
    }

    private fun rejectRequest(requestId: Int) {
        val originalRequests = state.value.pendingRequests
        setState { copy(pendingRequests = pendingRequests.filter { it.id != requestId }) }

        launch {
            sessionPreferences.updatePendingFollowRequestsCount(state.value.pendingRequests.size)
            
            profileRepository.rejectFollowRequest(requestId).onFailure {
                // Revert
                setState { copy(pendingRequests = originalRequests) }
                sessionPreferences.updatePendingFollowRequestsCount(originalRequests.size)
            }
        }
    }

    // Extracted ToggleFollow logic
    private fun toggleFollow() {
        val currentProfileId = state.value.profileId ?: return
        
        val isFollowed = state.value.isFollowedByMe
        val requestStatus = state.value.followRequestStatus
        val currentFollowers = state.value.followersCount

        // Optimistic UI state transition
        val newIsFollowed: Boolean
        val newRequestStatus: String?
        val newFollowersCount: Int
        val followingDelta: Int

        if (isFollowed) {
            // Takiptesin -> Takibi bırak (Unfollow)
            newIsFollowed = false
            newRequestStatus = null
            newFollowersCount = currentFollowers - 1
            followingDelta = -1
        } else if (requestStatus == "PENDING") {
            // İstek gönderilmiş -> İsteği iptal et (Geri çek)
            newIsFollowed = false
            newRequestStatus = null
            newFollowersCount = currentFollowers
            followingDelta = 0
        } else {
            // İstek yok -> İstek gönder (PENDING)
            newIsFollowed = false
            newRequestStatus = "PENDING"
            newFollowersCount = currentFollowers
            followingDelta = 0
        }

        setState { 
            copy(
                isFollowedByMe = newIsFollowed,
                followRequestStatus = newRequestStatus,
                followersCount = newFollowersCount
            ) 
        }

        launch {
            if (followingDelta != 0) {
                sessionPreferences.updateFollowCounts(
                    followersDelta = 0,
                    followingDelta = followingDelta
                )
            }
            
            val result = profileRepository.toggleFollow(currentProfileId)
            result.onFailure {
                // Revert on failure
                setState { 
                    copy(
                        isFollowedByMe = isFollowed,
                        followRequestStatus = requestStatus,
                        followersCount = currentFollowers
                    ) 
                }
                if (followingDelta != 0) {
                    sessionPreferences.updateFollowCounts(
                        followersDelta = 0,
                        followingDelta = -followingDelta
                    )
                }
            }
        }
    }
}
