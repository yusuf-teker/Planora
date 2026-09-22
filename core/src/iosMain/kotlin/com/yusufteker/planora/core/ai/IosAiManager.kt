package com.yusufteker.planora.core.ai

import com.yusufteker.planora.shared.ai.AiChatContext
import com.yusufteker.planora.shared.ai.AiChatResult
import kotlinx.serialization.Serializable

class IosAiManager(
    private val cloudAiManager: CloudAiManager
) : OfflineAiManager {

    private val ruleBasedEngine = RuleBasedNlpEngine()

    private val pipeline = AiPipeline(
        listOf(
            CloudApiStep(cloudAiManager, "iOS"),
            IosAppleIntelligenceStep(ruleBasedEngine),
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