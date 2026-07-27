package com.yusufteker.planora.feature.home.presentation.event_editor

sealed interface EventEditorEffect {
    object NavigateBack : EventEditorEffect
    data class ShowToast(val message: String) : EventEditorEffect
    data class ShareItem(val url: String) : EventEditorEffect
}
