package com.yusufteker.pulse.feature.home.presentation.aichat

import com.yusufteker.pulse.core.ai.OfflineAiManager
import com.yusufteker.pulse.core.base.BaseViewModel
import com.yusufteker.pulse.core.preferences.SessionPreferences
import com.yusufteker.pulse.core.utils.getCurrentTimeMs
import com.yusufteker.pulse.feature.home.domain.repository.PlanRepository
import com.yusufteker.pulse.feature.home.domain.repository.ProfileRepository
import com.yusufteker.pulse.shared.ai.AiAvailabilityState
import com.yusufteker.pulse.shared.ai.AiChatContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import com.yusufteker.pulse.core.ai.CloudAiManager
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
        // İndirme ilerlemesini gözlemle
        observeDownloadProgress()
    }

    private fun checkAiAvailability() {
        val state = offlineAiManager.availabilityState()
        setState {
            copy(
                aiAvailability = state,
                showDownloadPrompt = state == AiAvailabilityState.PROMPT_DOWNLOAD
            )
        }
    }

    private fun observeDownloadProgress() {
        launch {
            offlineAiManager.downloadProgress().collectLatest { progress ->
                setState {
                    copy(
                        downloadProgress = progress,
                        aiAvailability = when {
                            progress == 1.0f -> AiAvailabilityState.AVAILABLE
                            progress != null -> AiAvailabilityState.DOWNLOADING
                            else -> aiAvailability
                        }
                    )
                }
            }
        }
    }

    override fun onEvent(event: AiChatEvent) {
        when (event) {
            is AiChatEvent.InputTextChanged -> {
                setState { copy(inputText = event.text) }
            }
            is AiChatEvent.SendMessage -> {
                sendMessage()
            }
            is AiChatEvent.DismissDownloadPrompt -> {
                setState { copy(showDownloadPrompt = false) }
            }
            is AiChatEvent.RequestModelDownload -> {
                requestDownload()
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

                var result: com.yusufteker.pulse.shared.ai.AiChatResult? = null

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
                            val successSuffix = "\n\n✅ ${result.extractedEntities?.type?.name ?: "Görev"} başarıyla oluşturuldu."
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
                            val errorMsg = createResult.exceptionOrNull()?.message ?: "Bilinmeyen hata"
                            val errorSuffix = "\n\n❌ Oluşturulamadı: $errorMsg"
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
                    text = "Üzgünüm, bir hata oluştu: ${e.message}\n\nLütfen tekrar dener misin?",
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

    /**
     * Kullanıcı model indirmeyi kabul ettiğinde tetiklenir.
     */
    private fun requestDownload() {
        setState {
            copy(
                showDownloadPrompt = true,
                aiAvailability = AiAvailabilityState.DOWNLOADING,
                downloadProgress = 0f
            )
        }

        launch {
            try {
                offlineAiManager.requestDownload()
                // Tamamlandığında downloadProgress Flow üzerinden bildirilir
            } catch (e: Exception) {
                setState {
                    copy(
                        showDownloadPrompt = false,
                        aiAvailability = AiAvailabilityState.BASIC_ONLY,
                        downloadProgress = null
                    )
                }
                setEffect(AiChatEffect.ShowSnackbar("Model indirme başarısız: ${e.message}"))
            }
        }
    }
}
