package com.yusufteker.pulse.feature.home.presentation.plan_rooms

import androidx.lifecycle.viewModelScope
import com.yusufteker.pulse.core.base.BaseViewModel
import com.yusufteker.pulse.core.snackbar.SnackbarManager
import com.yusufteker.pulse.core.snackbar.SnackbarType
import com.yusufteker.pulse.feature.home.domain.repository.PlanRepository
import com.yusufteker.pulse.shared.api.CreatePlanRoomRequest
import com.yusufteker.pulse.shared.api.InviteUserRequest
import kotlinx.coroutines.launch

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
                if (name.isBlank()) {
                    snackbarManager.showMessage("Room name cannot be empty", SnackbarType.ERROR)
                    return
                }
                
                setState { copy(isLoading = true) }
                viewModelScope.launch {
                    val result = planRepository.createPlanRoom(CreatePlanRoomRequest(name))
                    result.onSuccess {
                        snackbarManager.showMessage("Room created successfully", SnackbarType.SUCCESS)
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
                        snackbarManager.showMessage("Failed to create room", SnackbarType.ERROR)
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
                
                if (userId == null) {
                    snackbarManager.showMessage("Invalid User ID", SnackbarType.ERROR)
                    return
                }
                
                setState { copy(isLoading = true) }
                viewModelScope.launch {
                    val result = planRepository.inviteUserToRoom(roomId, InviteUserRequest(userId))
                    result.onSuccess {
                        setState { copy(isLoading = false, isInviteDialogVisible = false) }
                        snackbarManager.showMessage("Invitation sent!", SnackbarType.SUCCESS)
                    }.onFailure {
                        setState { copy(isLoading = false) }
                        snackbarManager.showMessage("Failed to send invitation", SnackbarType.ERROR)
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
                        snackbarManager.showMessage(if (event.accept) "Odaya katılıldı!" else "Reddedildi", SnackbarType.SUCCESS)
                        setState { copy(isLoading = false) }
                        onEvent(PlanRoomsEvent.LoadPendingInvitations)
                        if (event.accept) {
                            onEvent(PlanRoomsEvent.LoadRooms)
                        }
                    }.onFailure {
                        setState { copy(isLoading = false) }
                        snackbarManager.showMessage("İşlem başarısız", SnackbarType.ERROR)
                    }
                }
            }
            
            is PlanRoomsEvent.OnRoomClick -> {
                setEffect(PlanRoomsEffect.NavigateToRoomDetail(event.roomId))
            }
        }
    }
}
