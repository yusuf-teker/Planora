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
import pulse.core.generated.resources.Res
import pulse.core.generated.resources.error_create_room_failed
import pulse.core.generated.resources.error_invalid_user_id
import pulse.core.generated.resources.error_operation_failed
import pulse.core.generated.resources.error_room_name_empty
import pulse.core.generated.resources.error_send_invitation_failed
import pulse.core.generated.resources.invitation_declined
import pulse.core.generated.resources.invitation_sent_success
import pulse.core.generated.resources.room_created_success
import pulse.core.generated.resources.room_joined_success

class PlanRoomsViewModel(
    private val planRepository: PlanRepository,
    private val snackbarManager: SnackbarManager
) : BaseViewModel<PlanRoomsState, PlanRoomsEvent, PlanRoomsEffect>(PlanRoomsState()) {

    init {
        // Observe rooms from local DB continuously
        viewModelScope.launch {
            planRepository.observeAllPlanRooms().collect { rooms ->
                setState { copy(rooms = rooms) }
            }
            // üye sayıs
        }

        
        // ViewModel başlatıldığında odaları ve davetleri sunucudan yükle
        onEvent(PlanRoomsEvent.LoadRooms)
        onEvent(PlanRoomsEvent.LoadPendingInvitations)
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
                    if (name.isBlank()) {
                        snackbarManager.showMessage(getString(Res.string.error_room_name_empty), SnackbarType.ERROR)
                        return@launch
                    }
                    
                    setState { copy(isLoading = true) }
                    val result = planRepository.createPlanRoom(CreatePlanRoomRequest(name))
                    result.onSuccess {
                        snackbarManager.showMessage(getString(Res.string.room_created_success), SnackbarType.SUCCESS)
                        setState { 
                            copy(
                                isLoading = false, 
                                isCreateRoomDialogVisible = false,
                                createRoomNameInput = "",
                                // Refresh rooms here (veya Flow tetiklenir)
                            )
                        }
                    }.onFailure {
                        setState { copy(isLoading = false) }
                        snackbarManager.showMessage(getString(Res.string.error_create_room_failed), SnackbarType.ERROR)
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
                    if (userId == null) {
                        snackbarManager.showMessage(getString(Res.string.error_invalid_user_id), SnackbarType.ERROR)
                        return@launch
                    }
                    
                    setState { copy(isLoading = true) }
                    val result = planRepository.inviteUserToRoom(roomId, InviteUserRequest(userId))
                    result.onSuccess {
                        setState { copy(isLoading = false, isInviteDialogVisible = false) }
                        snackbarManager.showMessage(getString(Res.string.invitation_sent_success), SnackbarType.SUCCESS)
                    }.onFailure {
                        setState { copy(isLoading = false) }
                        snackbarManager.showMessage(getString(Res.string.error_send_invitation_failed), SnackbarType.ERROR)
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
                    val result = planRepository.respondToInvite(event.roomId, event.accept)
                    result.onSuccess {
                        val msg = if (event.accept) getString(Res.string.room_joined_success) else getString(Res.string.invitation_declined)
                        snackbarManager.showMessage(msg, SnackbarType.SUCCESS)
                        setState { copy(isLoading = false) }
                        onEvent(PlanRoomsEvent.LoadPendingInvitations)
                        if (event.accept) {
                            onEvent(PlanRoomsEvent.LoadRooms)
                        }
                    }.onFailure {
                        setState { copy(isLoading = false) }
                        snackbarManager.showMessage(getString(Res.string.error_operation_failed), SnackbarType.ERROR)
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
        }
    }
}
