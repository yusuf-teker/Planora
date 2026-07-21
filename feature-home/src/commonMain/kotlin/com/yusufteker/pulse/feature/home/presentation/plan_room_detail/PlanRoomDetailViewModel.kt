package com.yusufteker.pulse.feature.home.presentation.plan_room_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufteker.pulse.core.base.BaseViewModel
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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlinx.datetime.minus

class PlanRoomDetailViewModel(
    private val planRepository: PlanRepository,
    private val profileRepository: ProfileRepository,
    private val sessionPreferences: com.yusufteker.pulse.core.preferences.SessionPreferences
) : BaseViewModel<PlanRoomDetailState, PlanRoomDetailEvent, PlanRoomDetailEffect>(
    initialState = PlanRoomDetailState()
) {

    private var allFollowingUsers = emptyList<UserProfileResponse>()

    override fun onEvent(event: PlanRoomDetailEvent) {
        when (event) {
            is PlanRoomDetailEvent.LoadRoom -> loadRoom(event.roomId)
            PlanRoomDetailEvent.OnBackClick -> setEffect(PlanRoomDetailEffect.NavigateBack)
            PlanRoomDetailEvent.OnInviteUserClick -> openInviteDialog()
            PlanRoomDetailEvent.OnDismissInviteDialog -> setState { copy(isInviteDialogOpen = false) }
            PlanRoomDetailEvent.OnCreateTaskClick -> setEffect(PlanRoomDetailEffect.NavigateToCreateTask(currentState.roomId))
            PlanRoomDetailEvent.OnCreateEventClick -> setEffect(PlanRoomDetailEffect.NavigateToCreateEvent(currentState.roomId))
            is PlanRoomDetailEvent.OnSearchQueryChange -> updateSearchQuery(event.query)
            is PlanRoomDetailEvent.OnUserSelectToInvite -> inviteUser(event.userId)
            is PlanRoomDetailEvent.OnTaskClick -> handleTaskClick(event.task)
            
            
            // Refresh is automatic
            // Rename & Delete Events
            PlanRoomDetailEvent.OnEditRoomClick -> {
                setState { copy(isRenameDialogOpen = true, renameRoomName = roomName) }
            }
            PlanRoomDetailEvent.OnDismissRenameDialog -> {
                setState { copy(isRenameDialogOpen = false) }
            }
            is PlanRoomDetailEvent.OnRenameRoomNameChange -> {
                setState { copy(renameRoomName = event.name) }
            }
            PlanRoomDetailEvent.OnRenameRoomSubmit -> renameRoom()
            PlanRoomDetailEvent.OnDeleteRoomClick -> deleteRoom()
        }
    }
    
    private fun loadRoom(roomId: String) {
        setState { copy(roomId = roomId, isLoading = true) }
        
        // 1. Observe all rooms to get this room's details (name, members)
        viewModelScope.launch {
            planRepository.observeAllPlanRooms().collect { rooms ->
                val room = rooms.find { it.id == roomId }
                if (room != null) {
                    val myUserId = sessionPreferences.getUserId() ?: ""
                    val isCreator = room.creatorId.toString() == myUserId
                    setState { copy(roomName = room.name, isRoomCreator = isCreator, myUserId = myUserId, isLoading = false) }
                    
                    // Fetch missing profiles for members
                    val currentProfiles = currentState.memberProfiles.toMutableMap()
                    var profilesUpdated = false
                    
                    room.members.forEach { member ->
                        if (!currentProfiles.containsKey(member.userId)) {
                            // Fetch profile from backend
                            val result = profileRepository.getProfile(member.userId.toString())
                            result.onSuccess { profile ->
                                currentProfiles[member.userId] = profile
                                profilesUpdated = true
                            }
                        }
                    }
                    
                    if (profilesUpdated) {
                        setState { copy(memberProfiles = currentProfiles.toMap()) }
                    }
                } else {
                    setState { copy(isLoading = false) }
                }
            }
        }
        viewModelScope.launch {
            planRepository.observeAllTasks().collect { tasks ->
                val roomTasks = tasks.filter { it.sharedRoomIds.contains(roomId) && it.parentId == null }
                setState { copy(roomTasks = roomTasks) }
            }
        }
        
        // 3. Fetch from network
        viewModelScope.launch {
            planRepository.fetchRoomTasks(roomId)
        }
    }
    
    private fun openInviteDialog() {
        setState { copy(isInviteDialogOpen = true, isFollowingLoading = true, inviteError = null) }
        viewModelScope.launch {
            val result = profileRepository.getFollowingUsers()
            result.onSuccess { users ->
                allFollowingUsers = users
                setState { copy(
                        isFollowingLoading = false,
                        followingUsers = filterUsers(searchQuery)
                    ) 
                }
            }.onFailure { e ->
                setState { copy(
                        isFollowingLoading = false,
                        inviteError = e.message ?: "Takip edilenler yüklenemedi."
                    ) 
                }
            }
        }
    }
    
    private fun updateSearchQuery(query: String) {
        setState { copy(
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
        val roomId = currentState.roomId
        if (roomId.isBlank()) return
        
        viewModelScope.launch {
            setState { copy(isLoading = true) }
            val request = InviteUserRequest(userId = userId)
            val result = planRepository.inviteUserToRoom(roomId, request)
            
            setState { copy(isLoading = false, isInviteDialogOpen = false) }
            
            result.onSuccess {
                setEffect(PlanRoomDetailEffect.ShowToast("Kullanıcı davet edildi."))
            }.onFailure { e ->
                setEffect(PlanRoomDetailEffect.ShowToast(e.message ?: "Kullanıcı davet edilemedi."))
            }
        }
    }

    private fun handleTaskClick(task: com.yusufteker.pulse.shared.api.TaskDto) {
        val isMine = task.creatorId.toString() == currentState.myUserId
        // Wait, the user is the creator if task.creatorId == myUserId. Actually, if they are a member they can edit?
        // Let's just always navigate to detail. The detail screen will handle if they can edit or not.
        when (task.type) {
            com.yusufteker.pulse.shared.api.TaskType.TASK -> setEffect(PlanRoomDetailEffect.NavigateToTaskEditor(task.id))
            com.yusufteker.pulse.shared.api.TaskType.EVENT -> setEffect(PlanRoomDetailEffect.NavigateToEventDetail(task.id))
            com.yusufteker.pulse.shared.api.TaskType.NOTE -> setEffect(PlanRoomDetailEffect.NavigateToNoteEditor(task.id))
            com.yusufteker.pulse.shared.api.TaskType.FOLDER -> {}
        }
    }

    private fun renameRoom() {
        val roomId = currentState.roomId
        val newName = currentState.renameRoomName
        if (roomId.isBlank() || newName.isBlank()) return
        
        viewModelScope.launch {
            setState { copy(isLoading = true) }
            val result = planRepository.renameRoom(roomId, newName)
            setState { copy(isLoading = false, isRenameDialogOpen = false) }
            
            result.onSuccess {
                setEffect(PlanRoomDetailEffect.ShowToast("Oda adı değiştirildi."))
            }.onFailure { e ->
                setEffect(PlanRoomDetailEffect.ShowToast(e.message ?: "Oda adı değiştirilemedi."))
            }
        }
    }
    
    private fun deleteRoom() {
        val roomId = currentState.roomId
        if (roomId.isBlank()) return
        
        viewModelScope.launch {
            setState { copy(isLoading = true) }
            val result = planRepository.deleteRoom(roomId)
            setState { copy(isLoading = false) }
            
            result.onSuccess {
                setEffect(PlanRoomDetailEffect.ShowToast("Oda silindi."))
                kotlinx.coroutines.delay(100) // Small delay to ensure toast starts
                setEffect(PlanRoomDetailEffect.NavigateBack)
            }.onFailure { e ->
                setEffect(PlanRoomDetailEffect.ShowToast(e.message ?: "Oda silinemedi."))
            }
        }
    }

}
