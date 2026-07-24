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
            AndroidGeminiNanoStep(appContext),
            CloudApiStep(cloudAiManager, "Android"),
            RuleBasedStep(ruleBasedEngine, "Android", strictMode = false)
        )
    )

    override suspend fun processMessage(input: String, context: AiChatContext): AiChatResult {
        return pipeline.processMessage(input, context)
    }

    override fun release() {
        // No-op
    }
}