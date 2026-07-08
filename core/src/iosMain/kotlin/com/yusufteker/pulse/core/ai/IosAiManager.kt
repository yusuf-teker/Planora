package com.yusufteker.pulse.core.ai

import com.yusufteker.pulse.shared.ai.AiChatContext
import com.yusufteker.pulse.shared.ai.AiChatResult
import kotlinx.serialization.Serializable

class IosAiManager(
    private val cloudAiManager: CloudAiManager
) : OfflineAiManager {

    private val ruleBasedEngine = RuleBasedNlpEngine()

    private val pipeline = AiPipeline(
        listOf(
            IosAppleIntelligenceStep(ruleBasedEngine),
            CloudApiStep(cloudAiManager, "iOS"),
            RuleBasedStep(ruleBasedEngine, "iOS", strictMode = false)
        )
    )

    override suspend fun processMessage(input: String, context: AiChatContext): AiChatResult {
        return pipeline.processMessage(input, context)
    }

    override fun release() {
        // No-op
    }
}

@Serializable
data class FoundationModelIntentResult(
    val type: String,
    val title: String,
    val hasDeadline: Boolean,
    val deadline: String?,
    val time: String?
)