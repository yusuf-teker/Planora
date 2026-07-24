package com.yusufteker.planora.feature.home.presentation.follow_list

import com.yusufteker.planora.core.base.BaseViewModel
import com.yusufteker.planora.core.preferences.SessionPreferences
import com.yusufteker.planora.feature.home.domain.repository.ProfileRepository
import io.github.aakira.napier.Napier

class FollowListViewModel(
    private val profileRepository: ProfileRepository,
    private val sessionPreferences: SessionPreferences
) : BaseViewModel<FollowListState, FollowListEvent, FollowListEffect>(
    initialState = FollowListState()
) {

    override fun onEvent(event: FollowListEvent) {
        when (event) {
            is FollowListEvent.LoadAll -> loadAll()
            is FollowListEvent.TabSelected -> setState { copy(selectedTab = event.index) }
            is FollowListEvent.UnfollowClicked -> unfollowUser(event.userId)
            is FollowListEvent.RemoveFollowerClicked -> removeFollower(event.userId)
            is FollowListEvent.AcceptRequestClicked -> acceptRequest(event.requestId)
            is FollowListEvent.RejectRequestClicked -> rejectRequest(event.requestId)
            is FollowListEvent.BackClicked -> setEffect(FollowListEffect.NavigateBack)
        }
    }

    private fun loadAll() {
        loadFollowers()
        loadFollowing()
        loadRequests()
    }

    private fun loadFollowers() {
        launch {
            setState { copy(isLoadingFollowers = true) }
            profileRepository.getFollowers()
                .onSuccess { setState { copy(followers = it, isLoadingFollowers = false) } }
                .onFailure { 
                    setState { copy(isLoadingFollowers = false) }
                    Napier.e(tag = "FollowList") { "Failed to load followers: ${it.message}" }
                }
        }
    }

    private fun loadFollowing() {
        launch {
            setState { copy(isLoadingFollowing = true) }
            profileRepository.getFollowingUsers()
                .onSuccess { setState { copy(following = it, isLoadingFollowing = false) } }
                .onFailure { 
                    setState { copy(isLoadingFollowing = false) }
                    Napier.e(tag = "FollowList") { "Failed to load following: ${it.message}" }
                }
        }
    }

    private fun loadRequests() {
        launch {
            setState { copy(isLoadingRequests = true) }
            profileRepository.getFollowRequests()
                .onSuccess { 
                    setState { copy(requests = it, isLoadingRequests = false) }
                    // Update pending count in preferences
                    sessionPreferences.updatePendingFollowRequestsCount(it.size)
                }
                .onFailure { 
                    setState { copy(isLoadingRequests = false) }
                    Napier.e(tag = "FollowList") { "Failed to load requests: ${it.message}" }
                }
        }
    }

    private fun unfollowUser(userId: Int) {
        // Optimistic UI: listeden çıkar
        val originalFollowing = state.value.following
        setState { copy(following = following.filter { it.id != userId }) }

        launch {
            sessionPreferences.updateFollowCounts(followersDelta = 0, followingDelta = -1)
            
            profileRepository.toggleFollow(userId).onFailure {
                // Revert
                setState { copy(following = originalFollowing) }
                sessionPreferences.updateFollowCounts(followersDelta = 0, followingDelta = 1)
            }
        }
    }

    private fun removeFollower(userId: Int) {
        // Optimistic UI: listeden çıkar
        val originalFollowers = state.value.followers
        setState { copy(followers = followers.filter { it.id != userId }) }

        launch {
            sessionPreferences.updateFollowCounts(followersDelta = -1, followingDelta = 0)
            
            profileRepository.removeFollower(userId).onFailure {
                // Revert
                setState { copy(followers = originalFollowers) }
                sessionPreferences.updateFollowCounts(followersDelta = 1, followingDelta = 0)
            }
        }
    }

    private fun acceptRequest(requestId: Int) {
        // Optimistic UI
        val originalRequests = state.value.requests
        val acceptedRequest = originalRequests.find { it.id == requestId }
        setState { copy(requests = requests.filter { it.id != requestId }) }

        launch {
            sessionPreferences.updateFollowCounts(followersDelta = 1, followingDelta = 0)
            sessionPreferences.updatePendingFollowRequestsCount(state.value.requests.size)
            
            profileRepository.acceptFollowRequest(requestId)
                .onSuccess {
                    // Reload followers to include the new follower
                    loadFollowers()
                }
                .onFailure {
                    // Revert
                    setState { copy(requests = originalRequests) }
                    sessionPreferences.updateFollowCounts(followersDelta = -1, followingDelta = 0)
                    sessionPreferences.updatePendingFollowRequestsCount(originalRequests.size)
                }
        }
    }

    private fun rejectRequest(requestId: Int) {
        // Optimistic UI
        val originalRequests = state.value.requests
        setState { copy(requests = requests.filter { it.id != requestId }) }

        launch {
            sessionPreferences.updatePendingFollowRequestsCount(state.value.requests.size)
            
            profileRepository.rejectFollowRequest(requestId).onFailure {
                // Revert
                setState { copy(requests = originalRequests) }
                sessionPreferences.updatePendingFollowRequestsCount(originalRequests.size)
            }
        }
    }
}
