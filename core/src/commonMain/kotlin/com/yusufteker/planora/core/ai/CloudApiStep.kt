package com.yusufteker.planora.core.ai

import com.yusufteker.planora.shared.ai.AiChatContext
import com.yusufteker.planora.shared.ai.AiChatResult
import io.github.aakira.napier.Napier

class CloudApiStep(
    private val cloudAiManager: CloudAiManager,
    private val platformName: String
) : AiStep {
    override suspend fun process(input: String, context: AiChatContext): AiChatResult? {
        Napier.d("CloudApiStep ($platformName): Cloud (Gemini API) deneniyor...", tag = "AiManagerLog")
        return cloudAiManager.processMessage(input, context)
    }
}
