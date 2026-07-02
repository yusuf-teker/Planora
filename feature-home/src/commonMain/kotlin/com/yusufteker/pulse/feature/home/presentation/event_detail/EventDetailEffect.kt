package com.yusufteker.pulse.feature.home.presentation.event_detail

sealed interface EventDetailEffect {
    object NavigateBack : EventDetailEffect
    data class ShowToast(val message: String) : EventDetailEffect
}
