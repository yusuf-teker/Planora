package com.yusufteker.planora.feature.home.presentation.plan_room_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufteker.planora.core.base.BaseViewModel
import com.yusufteker.planora.feature.home.domain.repository.PlanRepository
import com.yusufteker.planora.feature.home.domain.repository.ProfileRepository
import com.yusufteker.planora.feature.home.domain.use_case.getEarliestEventDateInMonth
import com.yusufteker.planora.shared.api.InviteUserRequest
import com.yusufteker.planora.shared.api.UserProfileResponse
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
import planora.core.generated.resources.Res
import planora.core.generated.resources.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.datetime.toLocalDateTime

class PlanRoomDetailViewModel(
    private val planRepository: PlanRepository,
    private val profileRepository: ProfileRepository,
    private val sessionPreferences: com.yusufteker.planora.core.preferences.SessionPreferences
) : BaseViewModel<PlanRoomDetailState, PlanRoomDetailEvent, PlanRoomDetailEffect>(
    initialState = PlanRoomDetailState()
) {

    private var allFollowingUsers = emptyList<UserProfileResponse>()

    override fun onEvent(event: PlanRoomDetailEvent) {
        when (event) {
            is PlanRoomDetailEvent.LoadRoom -> loadRoom(event.roomId)
            PlanRoomDetailEvent.OnBackClick -> setEffect(PlanRoomDetailEffect.NavigateBack)
            PlanRoomDetailEvent.OnInviteUserClick -> {
                setState { copy(isEditRoomBottomSheetOpen = true, renameRoomName = currentState.roomName) }
                openInviteDialog()
            }
            PlanRoomDetailEvent.OnDismissInviteDialog -> setState { copy(isInviteDialogOpen = false) }
            PlanRoomDetailEvent.OnCreateTaskClick -> setEffect(PlanRoomDetailEffect.NavigateToCreateTask(currentState.roomId))
            PlanRoomDetailEvent.OnCreateEventClick -> setEffect(PlanRoomDetailEffect.NavigateToCreateEvent(currentState.roomId))
            is PlanRoomDetailEvent.OnSearchQueryChange -> updateSearchQuery(event.query)
            is PlanRoomDetailEvent.OnUserSelectToInvite -> inviteUser(event.userId)
            is PlanRoomDetailEvent.OnTaskClick -> handleTaskClick(event.task)
            
            // Edit Room & Rename Events
            PlanRoomDetailEvent.OnEditRoomClick -> {
                setState { copy(isEditRoomBottomSheetOpen = true, renameRoomName = currentState.roomName) }
                openInviteDialog()
            }
            PlanRoomDetailEvent.OnDismissEditRoomBottomSheet -> {
                setState { copy(isEditRoomBottomSheetOpen = false) }
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
                val current = currentState.selectedMemberUserIdsFilter
                val next = if (event.userId == null) {
                    emptySet()
                } else if (current.contains(event.userId)) {
                    current - event.userId
                } else {
                    current + event.userId
                }
                setState { copy(selectedMemberUserIdsFilter = next) }
            }
            is PlanRoomDetailEvent.OnTaskSearchQueryChange -> setState { copy(taskSearchQuery = event.query) }
            is PlanRoomDetailEvent.OnCalendarDateSelected -> setState { copy(calendarSelectedDate = event.date) }
            PlanRoomDetailEvent.OnCalendarPreviousMonth -> {
                val current = currentState.calendarCurrentMonth ?: return
                val prev = current.minus(1, DateTimeUnit.MONTH)
                val nowMs = com.yusufteker.planora.core.utils.getCurrentTimeMs()
                val today = kotlinx.datetime.Instant.fromEpochMilliseconds(nowMs)
                    .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
                val selected = if (prev.year == today.year && prev.monthNumber == today.monthNumber) today else prev
                setState { copy(calendarCurrentMonth = prev, calendarSelectedDate = selected) }
            }
            PlanRoomDetailEvent.OnCalendarNextMonth -> {
                val current = currentState.calendarCurrentMonth ?: return
                val next = current.plus(1, DateTimeUnit.MONTH)
                val nowMs = com.yusufteker.planora.core.utils.getCurrentTimeMs()
                val today = kotlinx.datetime.Instant.fromEpochMilliseconds(nowMs)
                    .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
                val selected = if (next.year == today.year && next.monthNumber == today.monthNumber) today else next
                setState { copy(calendarCurrentMonth = next, calendarSelectedDate = selected) }
            }
            PlanRoomDetailEvent.OnCopyInviteLinkClick -> {
                viewModelScope.launch {
                    setEffect(PlanRoomDetailEffect.ShowToast(getString(Res.string.invite_link_copied)))
                }
            }
            is PlanRoomDetailEvent.OnRoomImageSelected -> uploadRoomImage(event.imageBytes)
            is PlanRoomDetailEvent.OnRemoveMemberClick -> {
                setState { copy(memberToRemove = event.user, isRemoveMemberDialogOpen = true) }
            }
            PlanRoomDetailEvent.OnConfirmRemoveMember -> {
                val member = currentState.memberToRemove
                setState { copy(isRemoveMemberDialogOpen = false, memberToRemove = null) }
                if (member != null) {
                    removeMember(member.id)
                }
            }
            PlanRoomDetailEvent.OnDismissRemoveMemberDialog -> {
                setState { copy(isRemoveMemberDialogOpen = false, memberToRemove = null) }
            }
        }
    }
    
    private var loadRoomJob: kotlinx.coroutines.Job? = null

    private fun loadRoom(roomId: String) {
        if (currentState.roomId == roomId && loadRoomJob?.isActive == true) {
            // Already observing this room, just refresh network data once
            viewModelScope.launch { planRepository.fetchRoomTasks(roomId) }
            viewModelScope.launch { planRepository.fetchMyRooms() }
            return
        }

        loadRoomJob?.cancel()
        setState { copy(roomId = roomId, isLoading = true, isMembersLoading = true) }
        
        // Default calendar month and date to today
        if (currentState.calendarCurrentMonth == null) {
            val nowMs = com.yusufteker.planora.core.utils.getCurrentTimeMs()
            val today = kotlinx.datetime.Instant.fromEpochMilliseconds(nowMs)
                .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
            val monthStart = kotlinx.datetime.LocalDate(today.year, today.monthNumber, 1)
            setState { copy(calendarCurrentMonth = monthStart, calendarSelectedDate = today) }
        }
        
        loadRoomJob = viewModelScope.launch {
            // 1. Observe all rooms to get this room's details (name, members)
            launch {
                planRepository.observeAllPlanRooms().collect { rooms ->
                    val room = rooms.find { it.id == roomId }
                    if (room != null) {
                        val myUserId = sessionPreferences.getUserId() ?: ""
                        val isCreator = room.creatorId.toString() == myUserId
                        setState { 
                            copy(
                                roomName = room.name, 
                                roomImageUrl = room.imageUrl, 
                                isRoomCreator = isCreator, 
                                myUserId = myUserId,
                                creatorId = room.creatorId,
                                roomMembers = room.members,
                                isLoading = false
                            ) 
                        }
                        
                        // Fetch missing profiles for members concurrently
                        val currentProfiles = currentState.memberProfiles.toMutableMap()
                        val validMemberUserIds = room.members.map { it.userId }.toSet()
                        currentProfiles.keys.retainAll(validMemberUserIds)

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
                                }.onFailure {
                                    if (!currentProfiles.containsKey(userId)) {
                                        val participantInfo = currentState.roomTasks
                                            .flatMap { it.participants }
                                            .find { it.userId == userId }
                                        currentProfiles[userId] = UserProfileResponse(
                                            id = userId,
                                            name = participantInfo?.name ?: "Kullanıcı $userId",
                                            username = "",
                                            email = "",
                                            avatarId = participantInfo?.avatarId ?: "default",
                                            profileImageUrl = participantInfo?.profileImageUrl
                                        )
                                    }
                                }
                            }
                            setState { copy(memberProfiles = currentProfiles.toMap(), isMembersLoading = false) }
                        } else {
                            setState { copy(memberProfiles = currentProfiles.toMap(), isMembersLoading = false) }
                        }
                    } else {
                        setState { copy(isLoading = false, isMembersLoading = false) }
                    }
                }
            }

            // 2. Observe all tasks to filter tasks belonging to this room
            launch {
                planRepository.observeAllTasks().collect { tasks ->
                    val roomTasks = tasks.filter { it.sharedRoomIds.contains(roomId) && it.parentId == null }
                    val nowMs = com.yusufteker.planora.core.utils.getCurrentTimeMs()
                    val today = kotlinx.datetime.Instant.fromEpochMilliseconds(nowMs)
                        .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
                    val selectedDate = currentState.calendarSelectedDate ?: today

                    val currentProfiles = currentState.memberProfiles.toMutableMap()
                    var profilesUpdated = false
                    roomTasks.forEach { task ->
                        task.participants.forEach { participant ->
                            if (!currentProfiles.containsKey(participant.userId)) {
                                currentProfiles[participant.userId] = UserProfileResponse(
                                    id = participant.userId,
                                    name = participant.name,
                                    username = "",
                                    email = "",
                                    avatarId = participant.avatarId ?: "default",
                                    profileImageUrl = participant.profileImageUrl
                                )
                                profilesUpdated = true
                            }
                        }
                    }

                    val existingMemberUserIds = currentState.roomMembers.map { it.userId }.toSet()
                    val extraUserIds = roomTasks.flatMap { t -> t.participants.map { it.userId } + t.creatorId }
                        .filter { it > 0 && !existingMemberUserIds.contains(it) }
                        .toSet()

                    val updatedRoomMembers = if (extraUserIds.isNotEmpty()) {
                        currentState.roomMembers + extraUserIds.map { extraId ->
                            com.yusufteker.planora.shared.api.PlanRoomMemberDto(
                                roomId = roomId,
                                userId = extraId,
                                status = com.yusufteker.planora.shared.api.RoomMemberStatus.ACCEPTED,
                                role = com.yusufteker.planora.shared.api.RoomMemberRole.MEMBER,
                                joinedAt = null
                            )
                        }
                    } else {
                        currentState.roomMembers
                    }

                    if (profilesUpdated) {
                        setState { copy(roomTasks = roomTasks, calendarSelectedDate = selectedDate, memberProfiles = currentProfiles.toMap(), roomMembers = updatedRoomMembers) }
                    } else {
                        setState { copy(roomTasks = roomTasks, calendarSelectedDate = selectedDate, roomMembers = updatedRoomMembers) }
                    }
                }
            }
        }
        
        // 3. Fetch network data once on load
        viewModelScope.launch {
            planRepository.fetchRoomTasks(roomId)
        }
        viewModelScope.launch {
            planRepository.fetchMyRooms()
        }
    }
    
    private var searchJob: kotlinx.coroutines.Job? = null

    private fun openInviteDialog() {
        setState { copy(isInviteDialogOpen = true, isFollowingLoading = true, inviteError = null) }
        viewModelScope.launch {
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
                setState { copy(
                        isFollowingLoading = false,
                        followingUsers = emptyList(),
                        inviteError = null
                    ) 
                }
            }
        }
    }
    
    private fun updateSearchQuery(query: String) {
        setState { copy(searchQuery = query) }
        searchJob?.cancel()
        
        if (query.isBlank()) {
            setState { copy(followingUsers = allFollowingUsers, isFollowingLoading = false, inviteError = null) }
            return
        }
        
        val localFiltered = filterUsers(query)
        setState { copy(followingUsers = localFiltered) }
        
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            if (localFiltered.isEmpty()) {
                setState { copy(isFollowingLoading = true, inviteError = null) }
            }
            val result = profileRepository.searchUsers(query)
            if (result.isSuccess) {
                val remoteUsers = result.getOrThrow()
                val combined = (localFiltered + remoteUsers).distinctBy { it.id }
                setState { copy(followingUsers = combined, isFollowingLoading = false, inviteError = null) }
            } else {
                setState { copy(isFollowingLoading = false) }
            }
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
        if (currentState.invitingUserIds.contains(userId)) return
        
        setState { copy(invitingUserIds = invitingUserIds + userId) }

        viewModelScope.launch {
            val successMsg = getString(Res.string.room_user_invited_success)
            val failureMsg = getString(Res.string.error_operation_failed)
            val request = InviteUserRequest(userId = userId)
            val result = planRepository.inviteUserToRoom(roomId, request)
            
            setState { copy(invitingUserIds = invitingUserIds - userId) }
            
            if (result.isSuccess) {
                setEffect(PlanRoomDetailEffect.ShowToast(successMsg))
                loadRoom(roomId)
            } else {
                val e = result.exceptionOrNull()
                val errorMsg = e?.message?.takeIf { it.isNotBlank() } ?: failureMsg
                setEffect(PlanRoomDetailEffect.ShowToast(errorMsg))
            }
        }
    }

    private fun handleTaskClick(task: com.yusufteker.planora.shared.api.TaskDto) {
        when (task.type) {
            com.yusufteker.planora.shared.api.TaskType.TASK -> setEffect(PlanRoomDetailEffect.NavigateToTaskDetail(task.id))
            com.yusufteker.planora.shared.api.TaskType.EVENT -> setEffect(PlanRoomDetailEffect.NavigateToEventDetail(task.id))
            com.yusufteker.planora.shared.api.TaskType.NOTE -> setEffect(PlanRoomDetailEffect.NavigateToNoteEditor(task.id))
            com.yusufteker.planora.shared.api.TaskType.FOLDER -> {}
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
                setState { copy(roomName = newName) }
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

    private fun uploadRoomImage(imageBytes: ByteArray) {
        val roomId = currentState.roomId
        if (roomId.isBlank()) return

        viewModelScope.launch {
            val successMsg = getString(Res.string.toast_room_image_updated_success)
            val failureMsg = getString(Res.string.error_operation_failed)

            setState { copy(isUploadingImage = true) }
            val result = planRepository.uploadRoomImage(roomId, imageBytes)
            setState { copy(isUploadingImage = false) }

            if (result.isSuccess) {
                val imageUrl = result.getOrNull()
                setState { copy(roomImageUrl = imageUrl) }
                setEffect(PlanRoomDetailEffect.ShowToast(successMsg))
            } else {
                val e = result.exceptionOrNull()
                setEffect(PlanRoomDetailEffect.ShowToast(e?.message ?: failureMsg))
            }
        }
    }

    private fun removeMember(targetUserId: Int) {
        val roomId = currentState.roomId
        if (roomId.isBlank()) return

        viewModelScope.launch {
            val successMsg = getString(Res.string.toast_member_removed_success)
            val failureMsg = getString(Res.string.error_operation_failed)

            setState { copy(isLoading = true) }
            val result = planRepository.removeMemberFromRoom(roomId, targetUserId)
            setState { copy(isLoading = false) }

            if (result.isSuccess) {
                val updatedProfiles = currentState.memberProfiles.toMutableMap().apply {
                    remove(targetUserId)
                }
                val updatedRoomMembers = currentState.roomMembers.filter { it.userId != targetUserId }
                val currentFilter = currentState.selectedMemberUserIdsFilter
                val nextFilter = currentFilter - targetUserId
                setState {
                    copy(
                        memberProfiles = updatedProfiles.toMap(),
                        roomMembers = updatedRoomMembers,
                        selectedMemberUserIdsFilter = nextFilter
                    )
                }
                setEffect(PlanRoomDetailEffect.ShowToast(successMsg))
            } else {
                val e = result.exceptionOrNull()
                setEffect(PlanRoomDetailEffect.ShowToast(e?.message ?: failureMsg))
            }
        }
    }
}

