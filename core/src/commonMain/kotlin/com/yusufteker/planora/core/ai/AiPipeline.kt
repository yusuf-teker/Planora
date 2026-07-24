package com.yusufteker.planora.core.ai

import com.yusufteker.planora.shared.ai.AiChatContext
import com.yusufteker.planora.shared.ai.AiChatResult
import io.github.aakira.napier.Napier

/**
 * AI zincirindeki (pipeline) her bir adımı temsil eder (örn: Apple Intelligence, Local LLM, Cloud API).
 */
interface AiStep {
    /**
     * Bu adımın belirtilen input'u işleyip işleyemeyeceğini ve sonucu döner.
     * Eğer işleyemezse (örneğin internet yok, model inmedi vs.) null döner.
     */
    suspend fun process(input: String, context: AiChatContext): AiChatResult?
}

/**
 * Belirlenen AiStep'leri sırasıyla (Chain of Responsibility) dener.
 * İlk null olmayan sonucu döndüren adımdan cevabı alır.
 */
class AiPipeline(
    private val steps: List<AiStep>
) {
    suspend fun processMessage(input: String, context: AiChatContext): AiChatResult {
        Napier.i("[AiPipeline] 🚀 Pipeline başladı. Toplam adım sayısı: ${steps.size}", tag = "AiManagerLog")
        
        for ((index, step) in steps.withIndex()) {
            val stepName = step::class.simpleName ?: "Bilinmeyen Adım"
            Napier.d("[AiPipeline] ⏳ Adım ${index + 1}/${steps.size} deneniyor: $stepName", tag = "AiManagerLog")
            
            val result = step.process(input, context)
            if (result != null) {
                Napier.i("[AiPipeline] ✅ Başarılı! Sonuç '$stepName' tarafından üretildi.", tag = "AiManagerLog")
                return result
            } else {
                Napier.d("[AiPipeline] ⏭️ Atlandı. '$stepName' null döndü, bir sonraki adıma geçiliyor.", tag = "AiManagerLog")
            }
        }
        
        Napier.e("[AiPipeline] ❌ Tüm adımlar başarısız oldu!", tag = "AiManagerLog")
        throw IllegalStateException("Tüm AI adımları başarısız oldu ve sonuç üretilemedi! (En azından RuleBasedStep'in her zaman sonuç dönmesi gerekir)")
    }
}
