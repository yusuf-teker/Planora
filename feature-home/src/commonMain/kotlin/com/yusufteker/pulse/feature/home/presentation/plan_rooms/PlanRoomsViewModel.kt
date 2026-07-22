package com.yusufteker.pulse.feature.home.presentation.plan_rooms

import androidx.lifecycle.viewModelScope
import com.yusufteker.pulse.core.base.BaseViewModel
import com.yusufteker.pulse.core.snackbar.SnackbarManager
import com.yusufteker.pulse.core.snackbar.SnackbarType
import com.yusufteker.pulse.feature.home.domain.repository.PlanRepository
import com.yusufteker.pulse.shared.api.CreatePlanRoomRequest
import com.yusufteker.pulse.shared.api.InviteUserRequest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import pulsy.core.generated.resources.Res
import pulsy.core.generated.resources.error_create_room_failed
import pulsy.core.generated.resources.error_invalid_user_id
import pulsy.core.generated.resources.error_operation_failed
import pulsy.core.generated.resources.error_room_name_empty
import pulsy.core.generated.resources.error_send_invitation_failed
import pulsy.core.generated.resources.invitation_declined
import pulsy.core.generated.resources.invitation_sent_success
import pulsy.core.generated.resources.room_created_success
import pulsy.core.generated.resources.room_joined_success

import com.yusufteker.pulse.feature.home.domain.repository.ProfileRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

class PlanRoomsViewModel(
    private val planRepository: PlanRepository,
    private val profileRepository: ProfileRepository,
    private val snackbarManager: SnackbarManager
) : BaseViewModel<PlanRoomsState, PlanRoomsEvent, PlanRoomsEffect>(PlanRoomsState()) {

    init {
        // Observe rooms from local DB continuously
        viewModelScope.launch {
            planRepository.observeAllPlanRooms().collect { rooms ->
                setState { copy(rooms = rooms) }
                loadMissingMemberProfiles(rooms)
            }
        }

        // ViewModel başlatıldığında odaları ve davetleri sunucudan yükle
        onEvent(PlanRoomsEvent.LoadRooms)
        onEvent(PlanRoomsEvent.LoadPendingInvitations)
    }

    private fun loadMissingMemberProfiles(rooms: List<com.yusufteker.pulse.shared.api.PlanRoomDto>) {
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
                setState { copy(isCreateRoomDialogVisible = event.isVisible) }
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
                setState { copy(isLoading = true) }
                viewModelScope.launch {
                    val joinedMsg = getString(Res.string.room_joined_success)
                    val declinedMsg = getString(Res.string.invitation_declined)
                    val failureMsg = getString(Res.string.error_operation_failed)

                    val result = planRepository.respondToInvite(event.roomId, event.accept)
                    if (result.isSuccess) {
                        val msg = if (event.accept) joinedMsg else declinedMsg
                        snackbarManager.showMessage(msg, SnackbarType.SUCCESS)
                        setState { copy(isLoading = false) }
                        onEvent(PlanRoomsEvent.LoadPendingInvitations)
                        if (event.accept) {
                            onEvent(PlanRoomsEvent.LoadRooms)
                            // Odaya katıldıktan sonra paylaşılan görevleri de çek
                            viewModelScope.launch {
                                planRepository.fetchMyTasks()
                            }
                        }
                    } else {
                        setState { copy(isLoading = false) }
                        snackbarManager.showMessage(failureMsg, SnackbarType.ERROR)
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
