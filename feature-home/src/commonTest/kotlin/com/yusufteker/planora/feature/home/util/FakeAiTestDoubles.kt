package com.yusufteker.planora.feature.home.util

import com.yusufteker.planora.core.ai.OfflineAiManager
import com.yusufteker.planora.feature.home.data.api.AiApi
import com.yusufteker.planora.shared.ai.AiChatContext
import com.yusufteker.planora.shared.ai.AiChatResult
import com.yusufteker.planora.shared.ai.AiChatServerRequest
import com.yusufteker.planora.shared.ai.AiChatServerResponse
import com.yusufteker.planora.shared.ai.AiIntent
import com.yusufteker.planora.shared.ai.AiQuotaDto
import kotlinx.coroutines.delay

/**
 * InMemory Fake AiApi implementasyonu.
 * Test senaryolarında sunucu AI yanıtlarını, kota aşımı ve gecikmeleri taklit eder.
 */
class FakeAiApi : AiApi() {

    var quotaResult: Result<AiQuotaDto> = Result.success(
        AiQuotaDto(
            isPremium = false,
            dailyRemaining = 1,
            dailyLimit = 1,
            weeklyRemaining = 3,
            weeklyLimit = 3
        )
    )

    var chatResponseResult: Result<AiChatServerResponse> = Result.success(
        AiChatServerResponse(
            result = AiChatResult(
                intent = AiIntent.CHAT,
                replyText = "Merhaba! Size nasıl yardımcı olabilirim?"
            ),
            quota = AiQuotaDto(
                isPremium = false,
                dailyRemaining = 0,
                dailyLimit = 1,
                weeklyRemaining = 2,
                weeklyLimit = 3
            ),
            quotaExceeded = false
        )
    )

    var simulateDelayMs: Long = 0L
    var sendChatMessageCallCount: Int = 0
    var getQuotaCallCount: Int = 0

    override suspend fun getQuota(): Result<AiQuotaDto> {
        getQuotaCallCount++
        if (simulateDelayMs > 0) delay(simulateDelayMs)
        return quotaResult
    }

    override suspend fun sendChatMessage(request: AiChatServerRequest): Result<AiChatServerResponse> {
        sendChatMessageCallCount++
        if (simulateDelayMs > 0) delay(simulateDelayMs)
        return chatResponseResult
    }
}

/**
 * InMemory Fake OfflineAiManager implementasyonu.
 * Çevrimdışı veya kota aşımı durumunda devreye giren yerel AI motorunu taklit eder.
 */
class FakeOfflineAiManager : OfflineAiManager {

    var offlineResult: AiChatResult = AiChatResult(
        intent = AiIntent.CHAT,
        replyText = "Yerel kural tabanlı motor yanıtı."
    )

    var processMessageCallCount: Int = 0
    var processRuleBasedCallCount: Int = 0
    var releaseCallCount: Int = 0

    override suspend fun processMessage(input: String, context: AiChatContext): AiChatResult {
        processMessageCallCount++
        return offlineResult
    }

    override suspend fun processRuleBased(input: String, context: AiChatContext): AiChatResult {
        processMessageCallCount++
        processRuleBasedCallCount++
        return offlineResult
    }

    override fun release() {
        releaseCallCount++
    }
}
