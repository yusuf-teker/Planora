package com.yusufteker.pulse.feature.home.presentation.profile

import com.yusufteker.pulse.core.base.BaseViewModel
import com.yusufteker.pulse.core.preferences.SessionPreferences
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.combine

import com.yusufteker.pulse.feature.home.domain.repository.ProfileRepository

class ProfileViewModel(
    private val sessionPreferences: SessionPreferences,
    private val profileRepository: ProfileRepository
) : BaseViewModel<ProfileState, ProfileEvent, ProfileEffect>(
    initialState = ProfileState()
) {

    init {
        // Flow olarak dinle: giriş/çıkış yapılınca DataStore değişir,
        // combine her iki değeri birlikte yakalar ve UI otomatik güncellenir.
        launch {
            combine(
                sessionPreferences.userIdFlow,
                sessionPreferences.userNameFlow,
                sessionPreferences.userAvatarFlow
            ) { userId, name, avatarId -> Triple(userId, name, avatarId) }
            .collect { (userId, name, avatarId) ->
                val isLoggedIn = userId != null
                setState { copy(isLoggedIn = isLoggedIn) }
                Napier.d(tag = "Screen", message = { "DataStore Flow geldi → isim='$name', avatar='$avatarId', isLoggedIn=$isLoggedIn" })
                // Only update from datastore if it's my profile and hasn't been loaded from network yet
                if (state.value.isMyProfile && state.value.profileId == null) {
                    setState {
                        copy(
                            name = name ?: "Misafir",
                            avatarId = avatarId ?: "avatar_1"
                        )
                    }
                }
            }
        }
    }

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
                
                setState { 
                    copy(
                        isLoading = true, 
                        profileId = userIdToLoad,
                        // Only clear if it's a different profile
                        name = if (isSameProfile) name else "",
                        avatarId = if (isSameProfile) avatarId else "",
                        username = if (isSameProfile) username else "",
                        followersCount = if (isSameProfile) followersCount else 0,
                        followingCount = if (isSameProfile) followingCount else 0,
                        postsCount = if (isSameProfile) postsCount else 0,
                        isFollowedByMe = if (isSameProfile) isFollowedByMe else false
                    ) 
                }
                launch {
                    val isMyProfile = userIdToLoad == null
                    
                    if (isMyProfile && !state.value.isLoggedIn) {
                        setState {
                            copy(
                                name = "Misafir",
                                avatarId = "avatar_1",
                                isMyProfile = true,
                                isLoading = false
                            )
                        }
                        return@launch
                    }
                    
                    val targetId = userIdToLoad?.toString() ?: "me"
                    
                    val result = profileRepository.getProfile(targetId)
                    result.onSuccess { profile ->
                        setState {
                            copy(
                                name = profile.name,
                                username = profile.username,
                                avatarId = profile.avatarId,
                                followersCount = profile.followersCount,
                                followingCount = profile.followingCount,
                                postsCount = profile.postsCount,
                                isFollowedByMe = profile.isFollowedByMe,
                                isMyProfile = isMyProfile,
                                isLoading = false
                            )
                        }
                    }.onFailure {
                        setState { copy(isLoading = false) }
                    }
                }
            }

            is ProfileEvent.ToggleFollowClicked -> {
                toggleFollow()
            }
            
            is ProfileEvent.LogoutClicked -> {
                launch {
                    sessionPreferences.clearSession()
                    setEffect(ProfileEffect.NavigateToLogin)
                }
            }
        }
    }
    
    // Extracted ToggleFollow logic
    private fun toggleFollow() {
        val currentProfileId = state.value.profileId ?: return
        
        // Optimistic UI update
        val wasFollowed = state.value.isFollowedByMe
        val currentFollowers = state.value.followersCount
        setState { 
            copy(
                isFollowedByMe = !wasFollowed,
                followersCount = if (wasFollowed) currentFollowers - 1 else currentFollowers + 1
            ) 
        }

        launch {
            val result = profileRepository.toggleFollow(currentProfileId)
            result.onFailure {
                // Revert on failure
                setState { 
                    copy(
                        isFollowedByMe = wasFollowed,
                        followersCount = currentFollowers
                    ) 
                }
            }
        }
    }
}
