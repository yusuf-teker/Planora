package com.yusufteker.planora.feature.home.presentation.aichat

import com.yusufteker.planora.core.base.UiState
import com.yusufteker.planora.core.ui.text.UiText
import com.yusufteker.planora.core.utils.getCurrentTimeMs
import planora.core.generated.resources.Res
import planora.core.generated.resources.ai_chat_welcome_message

import kotlin.random.Random

data class AiChatMessage(
    val id: String = "${getCurrentTimeMs()}_${Random.nextInt()}",
    val text: UiText,
    val isUser: Boolean,
    val isLoading: Boolean = false
)

data class AiChatState(
    val messages: List<AiChatMessage> = listOf(
        AiChatMessage(
            text = UiText.StringResourceId(Res.string.ai_chat_welcome_message),
            isUser = false
        )
    ),
    val inputText: String = "",
    val isLoading: Boolean = false,

) : UiState

