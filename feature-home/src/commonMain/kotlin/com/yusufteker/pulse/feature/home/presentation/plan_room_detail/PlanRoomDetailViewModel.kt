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

import org.jetbrains.compose.resources.getString
import pulsy.core.generated.resources.Res
import pulsy.core.generated.resources.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.datetime.toLocalDateTime

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
            
            // Rename Events
            PlanRoomDetailEvent.OnEditRoomClick -> {
                setState { copy(isRenameDialogOpen = true, renameRoomName = currentState.roomName) }
            }
            PlanRoomDetailEvent.OnDismissRenameDialog -> {
                setState { copy(isRenameDialogOpen = false) }
            }
            is PlanRoomDetailEvent.OnRenameRoomNameChange -> {
                setState { copy(renameRoomName = event.name) }
            }
            PlanRoomDetailEvent.OnRenameRoomSubmit -> renameRoom()
            
            // Delete Dialog Events
            PlanRoomDetailEvent.OnDeleteRoomClick -> {
                setState { copy(isDeleteConfirmationOpen = true) }
            }
            PlanRoomDetailEvent.OnConfirmDeleteRoom -> {
                setState { copy(isDeleteConfirmationOpen = false) }
                deleteRoom()
            }
            PlanRoomDetailEvent.OnDismissDeleteDialog -> {
                setState { copy(isDeleteConfirmationOpen = false) }
            }
            
            // Leave Room Events
            PlanRoomDetailEvent.OnLeaveRoomClick -> {
                setState { copy(isLeaveConfirmationOpen = true) }
            }
            PlanRoomDetailEvent.OnConfirmLeaveRoom -> {
                setState { copy(isLeaveConfirmationOpen = false) }
                leaveRoom()
            }
            PlanRoomDetailEvent.OnDismissLeaveDialog -> {
                setState { copy(isLeaveConfirmationOpen = false) }
            }
            
            // Tabs, Filters and Calendar Events
            is PlanRoomDetailEvent.OnTabSelected -> setState { copy(selectedTab = event.tab) }
            is PlanRoomDetailEvent.OnFilterSelected -> setState { copy(selectedFilter = event.filter) }
            is PlanRoomDetailEvent.OnMemberFilterSelected -> {
                val current = currentState.selectedMemberUserIdFilter
                val next = if (current == event.userId) null else event.userId
                setState { copy(selectedMemberUserIdFilter = next) }
            }
            is PlanRoomDetailEvent.OnTaskSearchQueryChange -> setState { copy(taskSearchQuery = event.query) }
            is PlanRoomDetailEvent.OnCalendarDateSelected -> setState { copy(calendarSelectedDate = event.date) }
            PlanRoomDetailEvent.OnCalendarPreviousMonth -> {
                val current = currentState.calendarCurrentMonth ?: return
                val prev = current.minus(1, DateTimeUnit.MONTH)
                setState { copy(calendarCurrentMonth = prev) }
            }
            PlanRoomDetailEvent.OnCalendarNextMonth -> {
                val current = currentState.calendarCurrentMonth ?: return
                val next = current.plus(1, DateTimeUnit.MONTH)
                setState { copy(calendarCurrentMonth = next) }
            }
            PlanRoomDetailEvent.OnCopyInviteLinkClick -> {
                viewModelScope.launch {
                    setEffect(PlanRoomDetailEffect.ShowToast(getString(Res.string.invite_link_copied)))
                }
            }
        }
    }
    
    private fun loadRoom(roomId: String) {
        setState { copy(roomId = roomId, isLoading = true, isMembersLoading = true) }
        
        // Default calendar month if not set
        if (currentState.calendarCurrentMonth == null) {
            val nowMs = com.yusufteker.pulse.core.utils.getCurrentTimeMs()
            val today = kotlinx.datetime.Instant.fromEpochMilliseconds(nowMs)
                .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
            setState { copy(calendarCurrentMonth = today, calendarSelectedDate = today) }
        }
        
        // 1. Observe all rooms to get this room's details (name, members)
        viewModelScope.launch {
            planRepository.observeAllPlanRooms().collect { rooms ->
                val room = rooms.find { it.id == roomId }
                if (room != null) {
                    val myUserId = sessionPreferences.getUserId() ?: ""
                    val isCreator = room.creatorId.toString() == myUserId
                    setState { copy(roomName = room.name, isRoomCreator = isCreator, myUserId = myUserId, isLoading = false) }
                    
                    // Fetch missing profiles for members concurrently
                    val currentProfiles = currentState.memberProfiles.toMutableMap()
                    val missingMemberIds = room.members.map { it.userId }.filter { !currentProfiles.containsKey(it) }
                    
                    if (missingMemberIds.isNotEmpty()) {
                        val deferredProfiles = missingMemberIds.map { userId ->
                            async {
                                userId to profileRepository.getProfile(userId.toString())
                            }
                        }
                        val results = deferredProfiles.awaitAll()
                        results.forEach { (userId, result) ->
                            result.onSuccess { profile ->
                                currentProfiles[userId] = profile
                            }
                        }
                        setState { copy(memberProfiles = currentProfiles.toMap(), isMembersLoading = false) }
                    } else {
                        setState { copy(isMembersLoading = false) }
                    }
                } else {
                    setState { copy(isLoading = false, isMembersLoading = false) }
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
            val defaultErrorMsg = getString(Res.string.error_operation_failed)
            val result = profileRepository.getFollowingUsers()
            if (result.isSuccess) {
                val users = result.getOrThrow()
                allFollowingUsers = users
                setState { copy(
                        isFollowingLoading = false,
                        followingUsers = filterUsers(searchQuery)
                    ) 
                }
            } else {
                val e = result.exceptionOrNull()
                setState { copy(
                        isFollowingLoading = false,
                        inviteError = e?.message ?: defaultErrorMsg
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
            val successMsg = getString(Res.string.room_user_invited_success)
            val failureMsg = getString(Res.string.error_operation_failed)
            setState { copy(isLoading = true) }
            val request = InviteUserRequest(userId = userId)
            val result = planRepository.inviteUserToRoom(roomId, request)
            
            setState { copy(isLoading = false, isInviteDialogOpen = false) }
            
            if (result.isSuccess) {
                setEffect(PlanRoomDetailEffect.ShowToast(successMsg))
            } else {
                val e = result.exceptionOrNull()
                setEffect(PlanRoomDetailEffect.ShowToast(e?.message ?: failureMsg))
            }
        }
    }

    private fun handleTaskClick(task: com.yusufteker.pulse.shared.api.TaskDto) {
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
            val successMsg = getString(Res.string.room_renamed_success)
            val failureMsg = getString(Res.string.error_operation_failed)
            setState { copy(isLoading = true) }
            val result = planRepository.renameRoom(roomId, newName)
            setState { copy(isLoading = false, isRenameDialogOpen = false) }
            
            if (result.isSuccess) {
                setEffect(PlanRoomDetailEffect.ShowToast(successMsg))
            } else {
                val e = result.exceptionOrNull()
                setEffect(PlanRoomDetailEffect.ShowToast(e?.message ?: failureMsg))
            }
        }
    }
    
    private fun deleteRoom() {
        val roomId = currentState.roomId
        if (roomId.isBlank()) return
        
        viewModelScope.launch {
            val successMsg = getString(Res.string.room_deleted_success)
            val failureMsg = getString(Res.string.error_delete_room_failed)
            setState { copy(isLoading = true) }
            val result = planRepository.deleteRoom(roomId)
            setState { copy(isLoading = false) }
            
            if (result.isSuccess) {
                setEffect(PlanRoomDetailEffect.ShowToast(successMsg))
                kotlinx.coroutines.delay(100)
                setEffect(PlanRoomDetailEffect.NavigateBack)
            } else {
                val e = result.exceptionOrNull()
                setEffect(PlanRoomDetailEffect.ShowToast(e?.message ?: failureMsg))
            }
        }
    }

    private fun leaveRoom() {
        val roomId = currentState.roomId
        if (roomId.isBlank()) return
        
        viewModelScope.launch {
            val successMsg = getString(Res.string.room_left_success)
            val failureMsg = getString(Res.string.error_leave_room_failed)
            setState { copy(isLoading = true) }
            val result = planRepository.leaveRoom(roomId)
            setState { copy(isLoading = false) }
            
            if (result.isSuccess) {
                setEffect(PlanRoomDetailEffect.ShowToast(successMsg))
                kotlinx.coroutines.delay(100)
                setEffect(PlanRoomDetailEffect.NavigateBack)
            } else {
                val e = result.exceptionOrNull()
                setEffect(PlanRoomDetailEffect.ShowToast(e?.message ?: failureMsg))
            }
        }
    }
}

