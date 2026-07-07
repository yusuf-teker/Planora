package com.yusufteker.pulse.core.ai

import com.yusufteker.pulse.shared.ai.AiAvailabilityState
import com.yusufteker.pulse.shared.ai.AiChatContext
import com.yusufteker.pulse.shared.ai.AiChatResult
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
     * AI motorunun anlık kullanılabilirlik durumu.
     * UI'da anlık gösterge için hızlı çağrılabilir.
     */
    fun availabilityState(): AiAvailabilityState

    /**
     * Model indirme ilerlemesi (0.0 .. 1.0). İndirme yoksa null.
     */
    fun downloadProgress(): StateFlow<Float?>

    /**
     * Kullanıcı indirmeyi onayladığında tetiklenir.
     * Android'de Edge AI SDK ile Gemini Nano indirir.
     * iOS'ta no-op (her şey bundled).
     */
    suspend fun requestDownload()

    /**
     * Kaynakları serbest bırakır (AI ekranı kapandığında çağrılır).
     */
    fun release()
}
