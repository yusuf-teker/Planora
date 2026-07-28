package com.yusufteker.planora.feature.home.presentation.event_detail

sealed interface EventDetailEffect {
    data object NavigateBack : EventDetailEffect
    data class NavigateToEditEvent(val eventId: String, val planRoomId: String?) : EventDetailEffect
    data class NavigateToCopyEvent(val eventId: String, val planRoomId: String?) : EventDetailEffect
    data class ShowToast(val message: String) : EventDetailEffect
    data class ShareItem(val url: String) : EventDetailEffect
}
