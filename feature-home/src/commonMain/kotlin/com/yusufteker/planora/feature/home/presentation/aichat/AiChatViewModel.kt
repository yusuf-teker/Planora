package com.yusufteker.planora.feature.home.presentation.aichat

import androidx.lifecycle.viewModelScope
import com.yusufteker.planora.core.ai.CloudAiManager
import com.yusufteker.planora.core.ai.OfflineAiManager
import com.yusufteker.planora.core.base.BaseViewModel
import com.yusufteker.planora.core.preferences.SessionPreferences
import com.yusufteker.planora.core.ui.text.UiText
import com.yusufteker.planora.core.utils.getCurrentTimeMs
import com.yusufteker.planora.feature.home.data.api.AiApi
import com.yusufteker.planora.feature.home.domain.repository.PlanRepository
import com.yusufteker.planora.feature.home.domain.repository.ProfileRepository
import com.yusufteker.planora.shared.ai.AiChatContext
import com.yusufteker.planora.shared.ai.AiChatResult
import com.yusufteker.planora.shared.ai.AiChatServerRequest
import com.yusufteker.planora.shared.ai.AiIntent
import com.yusufteker.planora.shared.ai.MyTaskInfo
import com.yusufteker.planora.shared.ai.SharedRoomInfo
import com.yusufteker.planora.shared.api.CreatePlanRoomRequest
import com.yusufteker.planora.shared.api.InviteUserRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import planora.core.generated.resources.Res
import planora.core.generated.resources.*

/**
 * Yapay Zeka Sohbet Ekranı ViewModel'i.
 *
 * Sıralama Kuralı:
 * 1. Sırada daima Gemini (Cloud/Server AI) çalıştırılır. Kullanıcının günlük/haftalık kotası takip edilir.
 * 2. Kullanıcının kotası dolduğunda veya çevrimdışı olunduğunda 2. sıradaki yerel motor (cihaz içi/rule-based) devreye girer.
 */
class AiChatViewModel(
    private val planRepository: PlanRepository,
    private val profileRepository: ProfileRepository,
    private val sessionPreferences: SessionPreferences,
    private val offlineAiManager: OfflineAiManager,
    private val cloudAiManager: CloudAiManager,
    private val aiApi: AiApi
) : BaseViewModel<AiChatState, AiChatEvent, AiChatEffect>(AiChatState()) {

    private var generationJob: Job? = null

    init {
        loadQuota()
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
            AiChatEvent.CancelGeneration -> {
                generationJob?.cancel()
                setState { copy(isLoading = false) }
            }
            AiChatEvent.DismissLimitDialog -> {
                setState { copy(showLimitReachedDialog = false) }
            }
            AiChatEvent.RefreshQuota -> {
                loadQuota()
            }
        }
    }

    /**
     * Kullanıcının güncel AI token/kullanım kotasını sunucudan çeker.
     */
    private fun loadQuota() {
        viewModelScope.launch {
            setState { copy(isQuotaLoading = true) }
            val quotaResult = aiApi.getQuota()
            if (quotaResult.isSuccess) {
                setState { copy(quota = quotaResult.getOrThrow(), isQuotaLoading = false) }
            } else {
                setState { copy(isQuotaLoading = false) }
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
                val context = buildAiChatContext()

                var result: AiChatResult? = null
                var isFallbackUsed = false
                var fallbackNote: String? = null

                // ── 1. SIRA (BİRİNCİL): Sunucu üzerinden Gemini AI (Kota takip edilerek) ──
                val serverResponseResult = aiApi.sendChatMessage(
                    AiChatServerRequest(
                        message = input,
                        context = context
                    )
                )

                val serverResponse = serverResponseResult.getOrNull()

                if (serverResponse != null) {
                    // Kotayı anlık olarak güncelle
                    setState { copy(quota = serverResponse.quota) }

                    if (serverResponse.quotaExceeded) {
                        // KOTA DOLDU! 2. sıradaki yerel motora düşülür.
                        isFallbackUsed = true
                        fallbackNote = runCatching { getString(Res.string.ai_fallback_notice) }
                            .getOrElse { "\n\n(Not: Günlük/haftalık bulut kotanız dolduğu için yanıt cihaz içi yerel motor tarafından üretilmiştir.)" }
                    } else if (serverResponse.result != null) {
                        result = serverResponse.result
                    } else if (!serverResponse.errorMessage.isNullOrBlank()) {
                        isFallbackUsed = true
                    }
                } else {
                    // Sunucuya ulaşılamadı veya çevrimdışı -> 2. sıradaki yerel motora düş
                    isFallbackUsed = true
                }

                // ── 2. SIRA (İKİNCİL / FALLBACK): Cihaz İçi / Yerel Motor ──
                if (result == null) {
                    result = offlineAiManager.processMessage(input, context)
                }

                val baseReply = result.replyText
                val finalReply = if (isFallbackUsed && fallbackNote != null) {
                    baseReply + fallbackNote
                } else {
                    baseReply
                }

                val responseMessage = AiChatMessage(
                    text = UiText.DynamicString(finalReply),
                    isUser = false,
                    isLoading = false
                )

                setState {
                    copy(
                        messages = messages + responseMessage,
                        isLoading = false,
                        fallbackUsed = isFallbackUsed
                    )
                }

                // Eylemleri Yürüt:
                // A) Görev / Etkinlik / Not Oluşturma
                if (result.shouldCreateTask && result.suggestedTaskRequest != null) {
                    executeCreateTask(result, responseMessage)
                }

                // B) Plan Odası Oluşturma
                if (result.shouldCreateRoom && !result.roomNameToCreate.isNullOrBlank()) {
                    executeCreateRoom(result.roomNameToCreate!!, responseMessage)
                }

                // C) Plan Odasına Üye Davet Etme
                if (result.shouldInviteUser && result.inviteRoomId != null && result.inviteUserId != null) {
                    executeInviteUser(result.inviteRoomId!!, result.inviteUserId!!, responseMessage)
                }

            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                val errorDesc = runCatching { getString(Res.string.ai_chat_error_occurred, e.message ?: "") }
                    .getOrElse { "Hata oluştu: ${e.message ?: "Bilinmeyen hata"}" }
                val errorMessage = AiChatMessage(
                    text = UiText.DynamicString(errorDesc),
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

    private suspend fun executeCreateTask(result: AiChatResult, responseMessage: AiChatMessage) {
        val req = result.suggestedTaskRequest ?: return
        val createResult = planRepository.createTask(req)
        if (createResult.isSuccess) {
            val title = result.extractedEntities?.title ?: req.title
            val timeInfo = result.extractedEntities?.dateTime?.let { ms ->
                val dt = Instant.fromEpochMilliseconds(ms)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                val minutes = dt.minute.toString().padStart(2, '0')
                val dateStr = "${dt.dayOfMonth} ${dt.month.name.take(3)} ${dt.year}"
                "$dateStr ${dt.hour}:$minutes"
            } ?: ""
            val timeText = if (timeInfo.isNotEmpty()) " ($timeInfo)" else ""
            val successSuffix = runCatching { getString(Res.string.ai_chat_task_created_success, title, timeText) }
                .getOrElse { "\n\n✅ Görev oluşturuldu: $title$timeText" }
            appendToMessage(responseMessage.id, successSuffix)
        } else {
            val errorMsg = createResult.exceptionOrNull()?.message ?: "Bilinmeyen hata"
            val errorSuffix = runCatching { getString(Res.string.ai_chat_task_created_failed, errorMsg) }
                .getOrElse { "\n\n❌ Görev oluşturulamadı: $errorMsg" }
            appendToMessage(responseMessage.id, errorSuffix)
        }
    }

    private suspend fun executeCreateRoom(roomName: String, responseMessage: AiChatMessage) {
        val createResult = planRepository.createPlanRoom(CreatePlanRoomRequest(name = roomName))
        if (createResult.isSuccess) {
            val successSuffix = runCatching { getString(Res.string.ai_chat_room_created_success, roomName) }
                .getOrElse { "\n\n✅ Plan odası oluşturuldu: $roomName" }
            appendToMessage(responseMessage.id, successSuffix)
        } else {
            val errorMsg = createResult.exceptionOrNull()?.message ?: "Bilinmeyen hata"
            val errorSuffix = runCatching { getString(Res.string.ai_chat_room_created_failed, errorMsg) }
                .getOrElse { "\n\n❌ Plan odası oluşturulamadı: $errorMsg" }
            appendToMessage(responseMessage.id, errorSuffix)
        }
    }

    private suspend fun executeInviteUser(roomId: String, userId: Int, responseMessage: AiChatMessage) {
        val inviteResult = planRepository.inviteUserToRoom(roomId, InviteUserRequest(userId = userId))
        if (inviteResult.isSuccess) {
            val successSuffix = runCatching { getString(Res.string.ai_chat_user_invited_success) }
                .getOrElse { "\n\n✅ Kullanıcı odaya davet edildi." }
            appendToMessage(responseMessage.id, successSuffix)
        } else {
            val errorMsg = inviteResult.exceptionOrNull()?.message ?: "Bilinmeyen hata"
            val errorSuffix = runCatching { getString(Res.string.ai_chat_user_invited_failed, errorMsg) }
                .getOrElse { "\n\n❌ Kullanıcı davet edilemedi: $errorMsg" }
            appendToMessage(responseMessage.id, errorSuffix)
        }
    }

    private fun appendToMessage(messageId: String, textToAppend: String) {
        setState {
            copy(
                messages = messages.map { msg ->
                    if (msg.id == messageId) {
                        val currentText = if (msg.text is UiText.DynamicString) msg.text.value else ""
                        msg.copy(text = UiText.DynamicString(currentText + textToAppend))
                    } else msg
                }
            )
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

        // Takvim erişimi olan kullanıcılar (isimleriyle birlikte)
        val accessibleUsers = try {
            planRepository.observeAccessibleUsers().first()
                .map { it.userId.toInt() to (it.name.ifBlank { it.username }) }
        } catch (e: Exception) {
            emptyList()
        }

        // Ortak odalar ve üye isimleri
        val sharedRooms = try {
            planRepository.observeAllPlanRooms().first().map { room ->
                val memberNames = room.members
                    .filter { it.status == com.yusufteker.planora.shared.api.RoomMemberStatus.ACCEPTED }
                    .mapNotNull { member ->
                        accessibleUsers.find { it.first == member.userId }?.second
                            ?: followedUsers.find { it.first == member.userId }?.second
                    }
                SharedRoomInfo(
                    roomId = room.id,
                    roomName = room.name,
                    memberNames = memberNames
                )
            }
        } catch (e: Exception) {
            emptyList()
        }

        // Kullanıcının mevcut görevleri (çakışma tespiti için)
        val myTasks = try {
            planRepository.observeAllTasks().first().map { task ->
                MyTaskInfo(
                    title = task.title,
                    startTime = task.startTime,
                    endTime = task.endTime,
                    type = task.type
                )
            }
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
            recentMessages = recentMessages,
            sharedRooms = sharedRooms,
            myTasks = myTasks,
            accessibleUsers = accessibleUsers
        )
    }
}
