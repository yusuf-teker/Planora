package com.yusufteker.pulse.core.ai

/**
 * iosApp (Swift) tarafında uygulama başlatılırken doldurulur.
 * Kotlin/Native bu object'i otomatik olarak Swift'e
 * "IosAiBridge.shared" şeklinde export eder — cinterop gerekmez.
 */
object IosAiBridge {
    var isAvailable: (() -> Boolean)? = null
    var parse: ((userInput: String, completion: (String?) -> Unit) -> Unit)? = null
}