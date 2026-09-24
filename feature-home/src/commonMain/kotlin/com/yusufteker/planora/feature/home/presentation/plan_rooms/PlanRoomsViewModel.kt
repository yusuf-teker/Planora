package com.yusufteker.planora.feature.home.presentation.plan_rooms

import androidx.lifecycle.viewModelScope
import com.yusufteker.planora.core.base.BaseViewModel
import com.yusufteker.planora.core.snackbar.SnackbarManager
import com.yusufteker.planora.core.snackbar.SnackbarType
import com.yusufteker.planora.feature.home.domain.repository.PlanRepository
import com.yusufteker.planora.shared.api.CreatePlanRoomRequest
import com.yusufteker.planora.shared.api.InviteUserRequest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import planora.core.generated.resources.Res
import planora.core.generated.resources.error_create_room_failed
import planora.core.generated.resources.error_invalid_user_id
import planora.core.generated.resources.error_operation_failed
import planora.core.generated.resources.error_room_name_empty
import planora.core.generated.resources.error_send_invitation_failed
import planora.core.generated.resources.invitation_declined
import planora.core.generated.resources.invitation_sent_success
import planora.core.generated.resources.room_created_success
import planora.core.generated.resources.room_joined_success

import com.yusufteker.planora.core.preferences.SessionPreferences
import com.yusufteker.planora.feature.home.domain.repository.ProfileRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

import kotlinx.coroutines.flow.combine

class PlanRoomsViewModel(
    private val planRepository: PlanRepository,
    private val profileRepository: ProfileRepository,
    private val snackbarManager: SnackbarManager,
    private val sessionPreferences: SessionPreferences
) : BaseViewModel<PlanRoomsState, PlanRoomsEvent, PlanRoomsEffect>(PlanRoomsState()) {

    companion object {
        const val MAX_FREE_PLAN_ROOMS = 3
    }

    init {
        // Observe user Premium subscription status to enforce the 3-room free tier limit
        viewModelScope.launch {
            sessionPreferences.isPremiumFlow.collect { isPrem ->
                setState { copy(isPremium = isPrem) }
            }
        }
        // Observe rooms and tasks from local DB continuously to ensure member list includes task participants/creators
        viewModelScope.launch {
            combine(
                planRepository.observeAllPlanRooms(),
                planRepository.observeAllTasks()
            ) { rooms, tasks ->
                rooms.map { room ->
                    val roomTasks = tasks.filter { it.sharedRoomIds.contains(room.id) }
                    val existingMemberUserIds = room.members.map { it.userId }.toSet()
                    val extraUserIds = roomTasks.flatMap { t -> t.participants.map { it.userId } + t.creatorId }
                        .filter { it > 0 && !existingMemberUserIds.contains(it) }
                        .toSet()

                    if (extraUserIds.isNotEmpty()) {
                        val mergedMembers = room.members + extraUserIds.map { extraId ->
                            com.yusufteker.planora.shared.api.PlanRoomMemberDto(
                                roomId = room.id,
                                userId = extraId,
                                status = com.yusufteker.planora.shared.api.RoomMemberStatus.ACCEPTED,
                                role = com.yusufteker.planora.shared.api.RoomMemberRole.MEMBER,
                                joinedAt = null
                            )
                        }
                        room.copy(members = mergedMembers)
                    } else {
                        room
                    }
                }
            }.collect { mergedRooms ->
                setState { copy(rooms = mergedRooms) }
                loadMissingMemberProfiles(mergedRooms)
            }
        }

        // ViewModel başlatıldığında odaları ve davetleri sunucudan yükle
        onEvent(PlanRoomsEvent.LoadRooms)
        onEvent(PlanRoomsEvent.LoadPendingInvitations)
    }

    private fun loadMissingMemberProfiles(rooms: List<com.yusufteker.planora.shared.api.PlanRoomDto>) {
        val currentProfiles = state.value.memberProfiles.toMutableMap()
        val allMemberUserIds = rooms.flatMap { room -> room.members.map { it.userId } }.distinct()
        val missingUserIds = allMemberUserIds.filter { !currentProfiles.containsKey(it) }

        if (missingUserIds.isEmpty()) return

        viewModelScope.launch {
            val deferred = missingUserIds.map { userId ->
                async {
                    userId to profileRepository.getProfile(userId.toString())
                }
            }
            val results = deferred.awaitAll()
            results.forEach { (userId, result) ->
                result.onSuccess { profile ->
                    currentProfiles[userId] = profile
                }
            }
            setState { copy(memberProfiles = currentProfiles.toMap()) }
        }
    }

    override fun onEvent(event: PlanRoomsEvent) {
        when (event) {
            PlanRoomsEvent.LoadRooms -> {
                viewModelScope.launch {
                    planRepository.fetchMyRooms() // API'den çekip DB'ye yazar
                }
            }
            PlanRoomsEvent.LoadPendingInvitations -> {
                viewModelScope.launch {
                    val result = planRepository.getMyPendingInvitations()
                    result.onSuccess { invitations ->
                        setState { copy(pendingInvitations = invitations) }
                    }
                }
            }
            
            // --- ODA OLUŞTURMA ---
            is PlanRoomsEvent.OnCreateRoomClick -> {
                if (event.isVisible && !currentState.isPremium && currentState.rooms.size >= MAX_FREE_PLAN_ROOMS) {
                    setState { copy(showRoomLimitDialog = true) }
                } else {
                    setState { copy(isCreateRoomDialogVisible = event.isVisible) }
                }
            }
            is PlanRoomsEvent.OnDismissRoomLimitDialog -> {
                setState { copy(showRoomLimitDialog = false) }
                if (event.navigateToPremium) {
                    setEffect(PlanRoomsEffect.NavigateToPremium)
                }
            }
            is PlanRoomsEvent.OnCreateRoomNameChanged -> {
                setState { copy(createRoomNameInput = event.name) }
            }
            PlanRoomsEvent.SubmitCreateRoom -> {
                val name = state.value.createRoomNameInput
                viewModelScope.launch {
                    val emptyNameMsg = getString(Res.string.error_room_name_empty)
                    val successMsg = getString(Res.string.room_created_success)
                    val failureMsg = getString(Res.string.error_create_room_failed)

                    if (name.isBlank()) {
                        snackbarManager.showMessage(emptyNameMsg, SnackbarType.ERROR)
                        return@launch
                    }
                    
                    setState { copy(isLoading = true) }
                    val result = planRepository.createPlanRoom(CreatePlanRoomRequest(name))
                    if (result.isSuccess) {
                        snackbarManager.showMessage(successMsg, SnackbarType.SUCCESS)
                        setState { 
                            copy(
                                isLoading = false, 
                                isCreateRoomDialogVisible = false,
                                createRoomNameInput = ""
                            )
                        }
                    } else {
                        setState { copy(isLoading = false) }
                        snackbarManager.showMessage(failureMsg, SnackbarType.ERROR)
                    }
                }
            }
            
            // --- DAVET ATMA ---
            is PlanRoomsEvent.OnInviteClick -> {
                setState { 
                    copy(
                        isInviteDialogVisible = event.isVisible,
                        selectedRoomIdForInvite = if (event.isVisible) event.roomId else null,
                        inviteUserIdInput = ""
                    )
                }
            }
            is PlanRoomsEvent.OnInviteUserIdChanged -> {
                setState { copy(inviteUserIdInput = event.userIdStr) }
            }
            PlanRoomsEvent.SubmitInvite -> {
                val roomId = state.value.selectedRoomIdForInvite ?: return
                val userId = state.value.inviteUserIdInput.toIntOrNull()
                
                viewModelScope.launch {
                    val invalidUserMsg = getString(Res.string.error_invalid_user_id)
                    val successMsg = getString(Res.string.invitation_sent_success)
                    val failureMsg = getString(Res.string.error_send_invitation_failed)

                    if (userId == null) {
                        snackbarManager.showMessage(invalidUserMsg, SnackbarType.ERROR)
                        return@launch
                    }
                    
                    setState { copy(isLoading = true) }
                    val result = planRepository.inviteUserToRoom(roomId, InviteUserRequest(userId))
                    if (result.isSuccess) {
                        setState { copy(isLoading = false, isInviteDialogVisible = false) }
                        snackbarManager.showMessage(successMsg, SnackbarType.SUCCESS)
                    } else {
                        setState { copy(isLoading = false) }
                        snackbarManager.showMessage(failureMsg, SnackbarType.ERROR)
                    }
                }
            }
            
            // --- DAVETLERİ GÖRÜNTÜLEME VE YANITLAMA ---
            is PlanRoomsEvent.OnInvitationsClick -> {
                setState { copy(isInvitationsDialogVisible = event.isVisible) }
                if (event.isVisible) {
                    onEvent(PlanRoomsEvent.LoadPendingInvitations)
                }
            }
            is PlanRoomsEvent.RespondToInvite -> {
                if (currentState.processingInviteIds.contains(event.roomId)) return
                if (event.accept && !currentState.isPremium && currentState.rooms.size >= MAX_FREE_PLAN_ROOMS) {
                    setState { copy(showRoomLimitDialog = true) }
                    return
                }
                val updatedInvitations = currentState.pendingInvitations.filter { it.id != event.roomId }
                setState {
                    copy(
                        processingInviteIds = processingInviteIds + event.roomId,
                        pendingInvitations = updatedInvitations
                    )
                }
                viewModelScope.launch {
                    val joinedMsg = getString(Res.string.room_joined_success)
                    val declinedMsg = getString(Res.string.invitation_declined)
                    val failureMsg = getString(Res.string.error_operation_failed)

                    val result = planRepository.respondToInvite(event.roomId, event.accept)
                    setState { copy(processingInviteIds = processingInviteIds - event.roomId) }
                    if (result.isSuccess) {
                        val msg = if (event.accept) joinedMsg else declinedMsg
                        snackbarManager.showMessage(msg, SnackbarType.SUCCESS)
                        onEvent(PlanRoomsEvent.LoadPendingInvitations)
                        if (event.accept) {
                            onEvent(PlanRoomsEvent.LoadRooms)
                            // Odaya katıldıktan sonra paylaşılan görevleri de çek
                            viewModelScope.launch {
                                planRepository.fetchMyTasks()
                            }
                        }
                    } else {
                        snackbarManager.showMessage(failureMsg, SnackbarType.ERROR)
                        onEvent(PlanRoomsEvent.LoadPendingInvitations)
                    }
                }
            }
            
            is PlanRoomsEvent.OnRoomClick -> {
                setEffect(PlanRoomsEffect.NavigateToRoomDetail(event.roomId))
            }
            
            PlanRoomsEvent.ToggleFab -> {
                setState { copy(isFabExpanded = !isFabExpanded) }
            }
            
            PlanRoomsEvent.OnCreateTaskClick -> {
                setState { copy(isFabExpanded = false) }
                setEffect(PlanRoomsEffect.NavigateToCreateTask)
            }
            
            PlanRoomsEvent.OnCreateEventClick -> {
                setState { copy(isFabExpanded = false) }
                setEffect(PlanRoomsEffect.NavigateToCreateEvent)
            }
        }
    }
}
