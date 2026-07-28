package com.yusufteker.planora.feature.home.presentation.event_detail

sealed interface EventDetailEvent {
    data class OnLoadEvent(
        val eventId: String? = null,
        val planRoomId: String? = null,
        val sharedTitle: String? = null,
        val sharedNote: String? = null,
        val sharedDate: Long? = null,
        val sharedSender: String? = null
    ) : EventDetailEvent
    data object OnEditClick : EventDetailEvent
    data object OnDeleteClick : EventDetailEvent
    data object OnBackClick : EventDetailEvent
    data object OnShareClick : EventDetailEvent
    data object OnCopyClick : EventDetailEvent
}
