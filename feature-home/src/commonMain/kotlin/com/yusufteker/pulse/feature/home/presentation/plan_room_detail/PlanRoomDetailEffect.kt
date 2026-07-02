package com.yusufteker.pulse.feature.home.presentation.plan_room_detail

sealed interface PlanRoomDetailEffect : com.yusufteker.pulse.core.base.UiEffect {
    object NavigateBack : PlanRoomDetailEffect
    data class ShowToast(val message: String) : PlanRoomDetailEffect
    data class NavigateToCreateTask(val roomId: String) : PlanRoomDetailEffect
    data class NavigateToCreateEvent(val roomId: String) : PlanRoomDetailEffect
}
