package com.yusufteker.pulse.feature.home.presentation.plan_room_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufteker.pulse.feature.home.domain.repository.PlanRepository
import com.yusufteker.pulse.feature.home.domain.repository.ProfileRepository
import com.yusufteker.pulse.shared.api.InviteUserRequest
import com.yusufteker.pulse.shared.api.UserProfileResponse
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlanRoomDetailViewModel(
    private val planRepository: PlanRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PlanRoomDetailState())
    val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<PlanRoomDetailEffect>()
    val effect = _effect.asSharedFlow()
    
    private var allFollowingUsers = emptyList<UserProfileResponse>()

    fun onEvent(event: PlanRoomDetailEvent) {
        when (event) {
            is PlanRoomDetailEvent.LoadRoom -> loadRoom(event.roomId)
            PlanRoomDetailEvent.OnBackClick -> setEffect(PlanRoomDetailEffect.NavigateBack)
            PlanRoomDetailEvent.OnInviteUserClick -> openInviteDialog()
            PlanRoomDetailEvent.OnDismissInviteDialog -> _state.update { it.copy(isInviteDialogOpen = false) }
            is PlanRoomDetailEvent.OnSearchQueryChange -> updateSearchQuery(event.query)
            is PlanRoomDetailEvent.OnUserSelectToInvite -> inviteUser(event.userId)
        }
    }
    
    private fun loadRoom(roomId: String) {
        _state.update { it.copy(roomId = roomId) }
        viewModelScope.launch {
            planRepository.observeAllPlanRooms().collect { rooms ->
                val room = rooms.find { it.id == roomId }
                if (room != null) {
                    _state.update { it.copy(roomName = room.name) }
                }
            }
        }
    }
    
    private fun openInviteDialog() {
        _state.update { it.copy(isInviteDialogOpen = true, isFollowingLoading = true, inviteError = null) }
        viewModelScope.launch {
            val result = profileRepository.getFollowingUsers()
            result.onSuccess { users ->
                allFollowingUsers = users
                _state.update { 
                    it.copy(
                        isFollowingLoading = false,
                        followingUsers = filterUsers(it.searchQuery)
                    ) 
                }
            }.onFailure { e ->
                _state.update { 
                    it.copy(
                        isFollowingLoading = false,
                        inviteError = e.message ?: "Takip edilenler yüklenemedi."
                    ) 
                }
            }
        }
    }
    
    private fun updateSearchQuery(query: String) {
        _state.update { 
            it.copy(
                searchQuery = query,
                followingUsers = filterUsers(query)
            ) 
        }
    }
    
    private fun filterUsers(query: String) = 
        if (query.isBlank()) allFollowingUsers 
        else allFollowingUsers.filter { 
            it.name.contains(query, ignoreCase = true) || it.username.contains(query, ignoreCase = true) 
        }
        
    private fun inviteUser(userId: Int) {
        val roomId = _state.value.roomId
        if (roomId.isBlank()) return
        
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val request = InviteUserRequest(userId = userId)
            val result = planRepository.inviteUserToRoom(roomId, request)
            
            _state.update { it.copy(isLoading = false, isInviteDialogOpen = false) }
            
            result.onSuccess {
                setEffect(PlanRoomDetailEffect.ShowToast("Kullanıcı davet edildi."))
            }.onFailure { e ->
                setEffect(PlanRoomDetailEffect.ShowToast(e.message ?: "Kullanıcı davet edilemedi."))
            }
        }
    }

    private fun setEffect(effect: PlanRoomDetailEffect) {
        viewModelScope.launch {
            _effect.emit(effect)
        }
    }
}
