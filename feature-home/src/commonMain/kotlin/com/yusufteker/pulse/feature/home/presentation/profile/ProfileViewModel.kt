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
                sessionPreferences.userNameFlow,
                sessionPreferences.userAvatarFlow
            ) { name, avatarId -> Pair(name, avatarId) }
            .collect { (name, avatarId) ->
                Napier.d(tag = "Screen", message = { "DataStore Flow geldi → isim='$name', avatar='$avatarId'" })
                // Only update from datastore if it's my profile and hasn't been loaded from network yet
                if (state.value.isMyProfile && state.value.profileId == null) {
                    setState {
                        copy(
                            name = name ?: "Kullanıcı",
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
                setState { copy(isLoading = true, profileId = userIdToLoad) }
                launch {
                    val isMyProfile = userIdToLoad == null
                    
                    val targetId = userIdToLoad?.toString() ?: "me"
                    
                    val result = profileRepository.getProfile(targetId)
                    result.onSuccess { profile ->
                        setState {
                            copy(
                                name = profile.name,
                                avatarId = profile.avatarId,
                                followersCount = profile.followersCount,
                                followingCount = profile.followingCount,
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
    }
}
