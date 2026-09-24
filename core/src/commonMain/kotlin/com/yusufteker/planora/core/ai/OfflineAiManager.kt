package com.yusufteker.planora.core.ai

import com.yusufteker.planora.shared.ai.AiChatContext
import com.yusufteker.planora.shared.ai.AiChatResult
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform bağımsız çevrimdışı AI yöneticisi arayüzü.
 *
 * Android: System AI (Gemini Nano) → Edge AI SDK (indirilebilir) → RuleBasedNlpEngine
 * iOS:     Apple Intelligence Foundation Models → NaturalLanguage → RuleBasedNlpEngine
 */
interface OfflineAiManager {

    /**
     * Kullanıcı mesajını işler; niyet, varlık çıkarımı ve sohbet yanıtı döner.
     * Uzun sürebilir, bu yüzden suspend fonksiyondur.
     */
    suspend fun processMessage(input: String, context: AiChatContext): AiChatResult

    /**
     * Mesajı doğrudan yerel kural tabanlı motor (RuleBased) ile işler.
     * Bulut veya LLM adımları (Gemini vb.) kesinlikle çalıştırılmaz.
     * Kota tükendiğinde çağrılır.
     *
     * @param input Kullanıcının yazdığı mesaj
     * @param context Mevcut planlama, oda ve görev bağlamı
     * @return Kural tabanlı çıkarım sonucu
     */
    suspend fun processRuleBased(input: String, context: AiChatContext): AiChatResult

    /**
     * Kaynakları serbest bırakır (AI ekranı kapandığında çağrılır).
     */
    fun release()
}
