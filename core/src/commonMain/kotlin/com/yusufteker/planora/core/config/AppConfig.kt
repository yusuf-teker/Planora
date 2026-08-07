package com.yusufteker.planora.core.config

/**
 * Uygulama genelindeki gizli/çevre konfigürasyonlarına erişim sağlar.
 * `local.properties` veya çevre değişkenlerinden (CI/CD) derleme zamanında `BuildKonfig` ile güvenli şekilde üretilir.
 */
object AppConfig {
    val apiNinjasKey: String? = BuildConfig.API_NINJAS_KEY.ifEmpty { null }
    val geminiApiKey: String = BuildConfig.GEMINI_API_KEY
}
