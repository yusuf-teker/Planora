package com.yusufteker.pulse.feature.home.presentation.search

import com.yusufteker.pulse.core.base.BaseViewModel
import com.yusufteker.pulse.feature.home.domain.repository.ProfileRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

import com.yusufteker.pulse.core.preferences.SessionPreferences

class SearchUsersViewModel(
    private val sessionPreferences: SessionPreferences,
    private val profileRepository: ProfileRepository
) : BaseViewModel<SearchUsersState, SearchUsersEvent, SearchUsersEffect>(
    initialState = SearchUsersState()
) {
    private var searchJob: Job? = null

    override fun onEvent(event: SearchUsersEvent) {
        when (event) {
            is SearchUsersEvent.OnQueryChanged -> {
                setState { copy(query = event.query) }
                performSearch(event.query)
            }
            is SearchUsersEvent.OnToggleFollow -> {
                toggleFollow(event.userId)
            }
            SearchUsersEvent.OnBackClicked -> {
                setEffect(SearchUsersEffect.NavigateBack)
            }
            is SearchUsersEvent.OnUserClicked -> {
                setEffect(SearchUsersEffect.NavigateToProfile(event.userId))
            }
        }
    }

    private fun performSearch(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            setState { copy(results = emptyList(), isLoading = false, error = null) }
            return
        }

        searchJob = launch {
            delay(500) // Debounce
            setState { copy(isLoading = true, error = null) }
            
            val result = profileRepository.searchUsers(query)
            result.onSuccess { users ->
                setState { copy(results = users, isLoading = false) }
            }.onFailure { error ->
                setState { copy(isLoading = false, error = error.message) }
            }
        }
    }

    private fun toggleFollow(userId: Int) {
        val currentResults = state.value.results
        val userIndex = currentResults.indexOfFirst { it.id == userId }
        if (userIndex == -1) return

        val user = currentResults[userIndex]
        val isFollowing = user.isFollowedByMe
        val requestStatus = user.followRequestStatus
        val currentFollowers = user.followersCount

        val newIsFollowed: Boolean
        val newRequestStatus: String?
        val newFollowersCount: Int
        val followingDelta: Int

        if (isFollowing) {
            newIsFollowed = false
            newRequestStatus = null
            newFollowersCount = currentFollowers - 1
            followingDelta = -1
        } else if (requestStatus == "PENDING") {
            newIsFollowed = false
            newRequestStatus = null
            newFollowersCount = currentFollowers
            followingDelta = 0
        } else {
            newIsFollowed = false
            newRequestStatus = "PENDING"
            newFollowersCount = currentFollowers
            followingDelta = 0
        }

        // Optimistic UI update
        val updatedUsers = currentResults.toMutableList()
        updatedUsers[userIndex] = user.copy(
            isFollowedByMe = newIsFollowed,
            followRequestStatus = newRequestStatus,
            followersCount = newFollowersCount
        )
        setState { copy(results = updatedUsers) }

        launch {
            if (followingDelta != 0) {
                sessionPreferences.updateFollowCounts(
                    followersDelta = 0,
                    followingDelta = followingDelta
                )
            }
            
            val result = profileRepository.toggleFollow(userId)
            result.onFailure {
                // Revert on failure locally
                val revertedUsers = state.value.results.toMutableList()
                val idx = revertedUsers.indexOfFirst { it.id == userId }
                if (idx != -1) {
                    revertedUsers[idx] = user
                    setState { copy(results = revertedUsers) }
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
