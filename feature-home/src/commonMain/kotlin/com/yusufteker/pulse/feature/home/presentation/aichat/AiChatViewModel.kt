package com.yusufteker.pulse.feature.home.presentation.aichat

import com.yusufteker.pulse.core.ai.OfflineAiManager
import com.yusufteker.pulse.core.base.BaseViewModel
import com.yusufteker.pulse.core.preferences.SessionPreferences
import com.yusufteker.pulse.core.utils.getCurrentTimeMs
import com.yusufteker.pulse.feature.home.domain.repository.PlanRepository
import com.yusufteker.pulse.feature.home.domain.repository.ProfileRepository

import com.yusufteker.pulse.shared.ai.AiChatContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import pulsy.core.generated.resources.Res
import pulsy.core.generated.resources.*
import com.yusufteker.pulse.core.ai.CloudAiManager
import com.yusufteker.pulse.shared.ai.AiChatResult
import com.yusufteker.pulse.shared.getPlatformName
import com.yusufteker.pulse.shared.isEmulator

class AiChatViewModel(
    private val planRepository: PlanRepository,
    private val profileRepository: ProfileRepository,
    private val sessionPreferences: SessionPreferences,
    private val offlineAiManager: OfflineAiManager,
    private val cloudAiManager: CloudAiManager
) : BaseViewModel<AiChatState, AiChatEvent, AiChatEffect>(AiChatState()) {

    init {
        // AI kullanılabilirliğini kontrol et
        checkAiAvailability()
    }

    private fun checkAiAvailability() {
        // No-op for now since we removed local LLM
    }

    override fun onEvent(event: AiChatEvent) {
        when (event) {
            is AiChatEvent.InputTextChanged -> {
                setState { copy(inputText = event.text) }
            }
            is AiChatEvent.SendMessage -> {
                sendMessage()
            }

            is AiChatEvent.ClearChat -> {
                setState { copy(messages = emptyList(), inputText = "") }
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
                inputText = "",
                isLoading = true
            )
        }

        launch {
            try {
                // AI bağlamını oluştur
                val context = buildAiChatContext()

                var result: AiChatResult? = null

                // 1. Eğer emülatörse cihazın LLM'ini (Apple Intelligence vb.) kullanamayacağımız için Cloud AI'yi dene
                if (isEmulator() && !getPlatformName().contains("Android")) {
                    io.github.aakira.napier.Napier.d("Emülatör tespit edildi: Direkt Cloud (Gemini API) çalıştırılıyor...", tag = "AiManagerLog")
                    result = cloudAiManager.processMessage(input, context)
                }

                // 2. Gerçek cihazsa veya Cloud API başarısız olursa çevrimdışı motoru kullan
                if (result == null) {
                    result = offlineAiManager.processMessage(input, context)
                }

                // AI yanıtını göster
                val responseMessage = aiLoadingMessage.copy(
                    text = result.replyText,
                    isLoading = false
                )
                setState {
                    copy(
                        messages = messages.map {
                            if (it.id == aiLoadingMessage.id) responseMessage else it
                        },
                        isLoading = false
                    )
                }

                // Görev oluşturma gerekiyorsa
                if (result.shouldCreateTask) {
                    result.suggestedTaskRequest?.let {
                        val createResult = planRepository.createTask(it)
                        if (createResult.isSuccess) {
                            val typeName = result.extractedEntities?.type?.name ?: getString(Res.string.task_label_simple)
                            val title = result.extractedEntities?.title ?: getString(Res.string.action_label_simple)
                            val timeInfo = result.extractedEntities?.dateTime?.let { ms ->
                                val dt = kotlinx.datetime.Instant.fromEpochMilliseconds(ms)
                                    .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
                                val minutes = dt.minute.toString().padStart(2, '0')
                                val dateStr = "${dt.dayOfMonth} ${dt.month.name.take(3)} ${dt.year}"
                                "$dateStr ${dt.hour}:$minutes"
                            } ?: ""
                            
                            val timeText = if (timeInfo.isNotEmpty()) " ($timeInfo)" else ""
                            val successSuffix = getString(Res.string.ai_chat_task_created_success, title, timeText)
                            
                            setState {
                                copy(
                                    messages = messages.map {
                                        if (it.id == responseMessage.id) {
                                            it.copy(text = it.text + successSuffix)
                                        } else it
                                    }
                                )
                            }
                        } else {
                            val errorMsg = createResult.exceptionOrNull()?.message ?: getString(Res.string.error_unknown)
                            val errorSuffix = getString(Res.string.ai_chat_task_created_failed, errorMsg)
                            setState {
                                copy(
                                    messages = messages.map {
                                        if (it.id == responseMessage.id) {
                                            it.copy(text = it.text + errorSuffix)
                                        } else it
                                    }
                                )
                            }
                        }
                    }

                }
            } catch (e: Exception) {
                val errorMessage = aiLoadingMessage.copy(
                    text = getString(Res.string.ai_chat_error_occurred, e.message ?: ""),
                    isLoading = false
                )
                setState {
                    copy(
                        messages = messages.map {
                            if (it.id == aiLoadingMessage.id) errorMessage else it
                        },
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * AI için oturum bağlamını oluşturur.
     */
    private suspend fun buildAiChatContext(): AiChatContext {
        val currentUserId = sessionPreferences.getUserId()?.toIntOrNull() ?: 0

        val followedUsers = try {
            val users = profileRepository.getFollowingUsers().getOrNull() ?: emptyList()
            users.map { it.id to it.username }
        } catch (e: Exception) {
            emptyList()
        }

        val recentMessages = state.value.messages
            .filter { !it.isLoading }
            .takeLast(4)
            .map { it.text }

        return AiChatContext(
            currentUserId = currentUserId,
            followedUsers = followedUsers,
            recentMessages = recentMessages
        )
    }


}
