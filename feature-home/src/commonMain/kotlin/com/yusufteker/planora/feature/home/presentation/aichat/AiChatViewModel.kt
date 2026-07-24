package com.yusufteker.planora.feature.home.presentation.aichat

import com.yusufteker.planora.core.ai.OfflineAiManager
import com.yusufteker.planora.core.base.BaseViewModel
import com.yusufteker.planora.core.preferences.SessionPreferences
import com.yusufteker.planora.core.ui.text.UiText
import com.yusufteker.planora.core.utils.getCurrentTimeMs
import com.yusufteker.planora.feature.home.domain.repository.PlanRepository
import com.yusufteker.planora.feature.home.domain.repository.ProfileRepository

import com.yusufteker.planora.shared.ai.AiChatContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import planora.core.generated.resources.Res
import planora.core.generated.resources.*
import com.yusufteker.planora.core.ai.CloudAiManager
import com.yusufteker.planora.shared.ai.AiChatResult
import com.yusufteker.planora.shared.getPlatformName
import com.yusufteker.planora.shared.isEmulator

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

    private var generationJob: kotlinx.coroutines.Job? = null

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
            AiChatEvent.CancelGeneration -> {
                generationJob?.cancel()
                setState { copy(isLoading = false) }
            }
        }
    }

    private fun sendMessage() {
        val input = state.value.inputText.trim()
        if (input.isEmpty()) return

        val userMessage = AiChatMessage(text = UiText.DynamicString(input), isUser = true)

        setState {
            copy(
                messages = messages + userMessage,
                inputText = "",
                isLoading = true
            )
        }

        generationJob = launch {
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
                val responseMessage = AiChatMessage(
                    text = UiText.DynamicString(result.replyText),
                    isUser = false,
                    isLoading = false
                )
                setState {
                    copy(
                        messages = messages + responseMessage,
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
                                    messages = messages.map { msg ->
                                        if (msg.id == responseMessage.id) {
                                            val currentText = if (msg.text is UiText.DynamicString) msg.text.value else ""
                                            msg.copy(text = UiText.DynamicString(currentText + successSuffix))
                                        } else msg
                                    }
                                )
                            }
                        } else {
                            val errorMsg = createResult.exceptionOrNull()?.message ?: getString(Res.string.error_unknown)
                            val errorSuffix = getString(Res.string.ai_chat_task_created_failed, errorMsg)
                            setState {
                                copy(
                                    messages = messages.map { msg ->
                                        if (msg.id == responseMessage.id) {
                                            val currentText = if (msg.text is UiText.DynamicString) msg.text.value else ""
                                            msg.copy(text = UiText.DynamicString(currentText + errorSuffix))
                                        } else msg
                                    }
                                )
                            }
                        }
                    }

                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Sadece iptal edildi, bir şey yapmaya gerek yok (isLoading zaten false yapıldı)
                throw e
            } catch (e: Exception) {
                val errorMessage = AiChatMessage(
                    text = UiText.DynamicString(getString(Res.string.ai_chat_error_occurred, e.message ?: "")),
                    isUser = false,
                    isLoading = false
                )
                setState {
                    copy(
                        messages = messages + errorMessage,
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
            .map { if (it.text is UiText.DynamicString) it.text.value else "" }

        return AiChatContext(
            currentUserId = currentUserId,
            followedUsers = followedUsers,
            recentMessages = recentMessages
        )
    }


}
