package com.yusufteker.pulse.feature.home.presentation.plan_room_detail

sealed interface PlanRoomDetailEffect {
    object NavigateBack : PlanRoomDetailEffect
    data class ShowToast(val message: String) : PlanRoomDetailEffect
}
