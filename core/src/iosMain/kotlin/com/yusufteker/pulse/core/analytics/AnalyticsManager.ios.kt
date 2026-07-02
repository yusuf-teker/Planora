package com.yusufteker.pulse.core.analytics

actual class AnalyticsManager {
    actual fun logEvent(name: String, parameters: Map<String, Any>) {
        // iOS implementation deferred to Phase 2
    }

    actual fun setUserId(userId: String) {
        // iOS implementation deferred to Phase 2
    }

    actual fun logException(exception: Throwable) {
        // iOS implementation deferred to Phase 2
    }
}
