package com.yusufteker.planora.core.config

/**
 * Uygulama genelindeki gizli/çevre konfigürasyonlarına erişim sağlar.
 */
expect object AppConfig {
    val apiNinjasKey: String?
}
