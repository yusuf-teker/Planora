package com.yusufteker.planora.feature.home.presentation.aichat

sealed class AiChatEvent : com.yusufteker.planora.core.base.UiEvent {
    data class InputTextChanged(val text: String) : AiChatEvent()
    data object SendMessage : AiChatEvent()
    data object ClearChat : AiChatEvent()
    data object CancelGeneration : AiChatEvent()
}

sealed class AiChatEffect : com.yusufteker.planora.core.base.UiEffect {
    data class ShowSnackbar(val message: String) : AiChatEffect()
}
