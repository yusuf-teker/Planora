package com.yusufteker.pulse.feature.home.presentation.aichat

import com.yusufteker.pulse.core.base.BaseViewModel
import com.yusufteker.pulse.feature.home.domain.repository.PlanRepository
import com.yusufteker.pulse.feature.home.domain.repository.ProfileRepository
import kotlinx.coroutines.launch

class AiChatViewModel(
    private val planRepository: PlanRepository,
    private val profileRepository: ProfileRepository
) : BaseViewModel<AiChatState, AiChatEvent, AiChatEffect>(AiChatState()) {

    override fun onEvent(event: AiChatEvent) {
        when (event) {
            is AiChatEvent.InputTextChanged -> {
                setState { copy(inputText = event.text) }
            }
            is AiChatEvent.SendMessage -> {
                sendMessage()
            }
        }
    }

    private fun sendMessage() {
        val input = state.value.inputText.trim()
        if (input.isEmpty()) return

        val userMessage = AiChatMessage(text = input, isUser = true)
        val aiLoadingMessage = AiChatMessage(text = "...", isUser = false, isLoading = true)

        setState { 
            copy(
                messages = messages + userMessage + aiLoadingMessage,
                inputText = ""
            ) 
        }

        launch {
            try {
                // Determine task type based on followers matching
                val followers = profileRepository.getFollowingUsers().getOrNull() ?: emptyList()
                val mentionedUser = followers.find { input.contains(it.username, ignoreCase = true) }
                
                val request = com.yusufteker.pulse.shared.api.CreateTaskRequest(
                    title = if (mentionedUser != null) "AI Gen: Event with ${mentionedUser.username}" else "AI Gen: ${input.take(15)}...",
                    description = input,
                    startTime = com.yusufteker.pulse.core.utils.getCurrentTimeMs(),
                    type = if (mentionedUser != null) com.yusufteker.pulse.shared.api.TaskType.EVENT else com.yusufteker.pulse.shared.api.TaskType.NOTE,
                    status = com.yusufteker.pulse.shared.api.TaskStatus.PENDING,
                    visibility = com.yusufteker.pulse.shared.api.TaskVisibility.PRIVATE,
                    participants = if (mentionedUser != null) mapOf(mentionedUser.id to "PENDING") else emptyMap(),
                    isRecurring = false,
                    recurrenceRule = null,
                    isFlexible = true,
                    isOptional = true,
                    isPostponable = true,
                    isAllDay = false,
                    reminders = emptyList()
                )
                
                val result = planRepository.createTask(request)
                if (result.isSuccess) {
                    val responseMessage = aiLoadingMessage.copy(
                        text = "İşlem tamamlandı! Talebini yerine getirdim.",
                        isLoading = false
                    )
                    setState {
                        copy(messages = messages.map { if (it.id == aiLoadingMessage.id) responseMessage else it })
                    }
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Bilinmeyen bir hata."
                    val responseMessage = aiLoadingMessage.copy(
                        text = "Maalesef oluştururken bir hata oldu: $errorMsg",
                        isLoading = false
                    )
                    setState {
                        copy(messages = messages.map { if (it.id == aiLoadingMessage.id) responseMessage else it })
                    }
                }
            } catch (e: Exception) {
                val errorMessage = aiLoadingMessage.copy(
                    text = "Üzgünüm, bir hata oluştu: ${e.message}",
                    isLoading = false
                )
                setState {
                    copy(
                        messages = messages.map { if (it.id == aiLoadingMessage.id) errorMessage else it }
                    )
                }
            }
        }
    }
}
