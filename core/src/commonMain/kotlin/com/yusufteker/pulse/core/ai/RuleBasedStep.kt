package com.yusufteker.pulse.core.ai

import com.yusufteker.pulse.shared.ai.AiChatContext
import com.yusufteker.pulse.shared.ai.AiChatResult
import io.github.aakira.napier.Napier

class RuleBasedStep(
    private val ruleBasedEngine: RuleBasedNlpEngine,
    private val platformName: String,
    private val strictMode: Boolean = false
) : AiStep {
    override suspend fun process(input: String, context: AiChatContext): AiChatResult? {
        val result = ruleBasedEngine.processMessage(input, context)
        
        if (strictMode) {
            val confidence = result?.extractedEntities?.confidence ?: 0f
            val formattedConfidence = ((confidence * 100).toInt() / 100.0).toString()
            if (confidence >= 0.8f) {
                Napier.d("RuleBasedStep ($platformName): Yüksek güven ($formattedConfidence), Cloud atlanıyor.", tag = "AiManagerLog")
                return result
            } else {
                Napier.d("RuleBasedStep ($platformName): Düşük güven ($formattedConfidence), Cloud'a geçiliyor.", tag = "AiManagerLog")
                return null
            }
        }
        
        Napier.d("RuleBasedStep ($platformName): Fallback çalışıyor...", tag = "AiManagerLog")
        return result
    }
}
