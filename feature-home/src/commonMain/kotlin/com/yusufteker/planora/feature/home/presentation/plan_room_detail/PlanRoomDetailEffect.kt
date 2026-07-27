package com.yusufteker.planora.feature.home.presentation.plan_room_detail

sealed interface PlanRoomDetailEffect : com.yusufteker.planora.core.base.UiEffect {
    object NavigateBack : PlanRoomDetailEffect
    data class ShowToast(val message: String) : PlanRoomDetailEffect
    data class NavigateToCreateTask(val roomId: String) : PlanRoomDetailEffect
    data class NavigateToCreateEvent(val roomId: String) : PlanRoomDetailEffect
    data class NavigateToTaskDetail(val taskId: String) : PlanRoomDetailEffect
    data class NavigateToTaskEditor(val taskId: String) : PlanRoomDetailEffect
    data class NavigateToEventDetail(val eventId: String) : PlanRoomDetailEffect
    data class NavigateToEventEditor(val eventId: String) : PlanRoomDetailEffect
    data class NavigateToNoteEditor(val noteId: String) : PlanRoomDetailEffect
}
