package com.yusufteker.pulse.core.ai

/**
 * iosApp (Swift) tarafında uygulama başlatılırken doldurulur.
 * Kotlin/Native bu object'i otomatik olarak Swift'e
 * "IosAiBridge.shared" şeklinde export eder — cinterop gerekmez.
 */
object IosAiBridge {
    var isAvailable: (() -> Boolean)? = null
    var parse: ((userInput: String, completion: (String?) -> Unit) -> Unit)? = null

    // ── Local LLM (LocalLLMClient / llama.cpp GGUF) ──
    /** Model daha önce indirilmiş mi (uygulama yeniden açıldığında sormamak için). */
    var isLocalModelDownloaded: (() -> Boolean)? = null
    /** Progress 0f..1f aralığında raporlanır, completion(true) = başarılı indi. */
    var downloadLocalModel: ((onProgress: (Float) -> Unit, completion: (Boolean) -> Unit) -> Unit)? = null
    /** Ortak SYSTEM_PROMPT + kullanıcı input'unu birleştirip çağırıyoruz, JSON string bekleniyor. */
    var generateLocal: ((prompt: String, completion: (String?) -> Unit) -> Unit)? = null
}