package com.yusufteker.pulse.feature.home.presentation.aichat

sealed class AiChatEvent : com.yusufteker.pulse.core.base.UiEvent {
    data class InputTextChanged(val text: String) : AiChatEvent()
    data object SendMessage : AiChatEvent()
    data object DismissDownloadPrompt : AiChatEvent()
    data object RequestModelDownload : AiChatEvent()
    data object ClearChat : AiChatEvent()
}

sealed class AiChatEffect : com.yusufteker.pulse.core.base.UiEffect {
    data class ShowSnackbar(val message: String) : AiChatEffect()
}
