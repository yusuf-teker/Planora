package com.yusufteker.pulse.feature.home.presentation.aichat
import com.yusufteker.pulse.core.utils.getCurrentTimeMs
data class AiChatMessage(
    val id: String = getCurrentTimeMs().toString(),
    val text: String,
    val isUser: Boolean,
    val isLoading: Boolean = false
)

data class AiChatState(
    val messages: List<AiChatMessage> = listOf(
        AiChatMessage(text = "Merhaba! Sana nasıl yardımcı olabilirim? Görev ekleyebilir veya not alabilirsin.", isUser = false)
    ),
    val inputText: String = "",
    val isLoading: Boolean = false
) : com.yusufteker.pulse.core.base.UiState
