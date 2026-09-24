package com.yusufteker.planora.core.ai

import android.content.Context
import com.yusufteker.planora.shared.ai.AiChatContext
import com.yusufteker.planora.shared.ai.AiChatResult

class AndroidAiManager(
    private val appContext: Context,
    private val cloudAiManager: CloudAiManager
) : OfflineAiManager {

    private val ruleBasedEngine = RuleBasedNlpEngine()

    private val pipeline = AiPipeline(
        listOf(
            CloudApiStep(cloudAiManager, "Android"),
            AndroidGeminiNanoStep(appContext),
            RuleBasedStep(ruleBasedEngine, "Android", strictMode = false)
        )
    )

    override suspend fun processMessage(input: String, context: AiChatContext): AiChatResult {
        return pipeline.processMessage(input, context)
    }

    override suspend fun processRuleBased(input: String, context: AiChatContext): AiChatResult {
        return ruleBasedEngine.processMessage(input, context)
    }

    override fun release() {
        // No-op
    }
}