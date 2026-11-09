package com.yusufteker.planora.core.analytics

import io.github.aakira.napier.Napier

actual class AnalyticsManager {
    actual fun logEvent(name: String, parameters: Map<String, Any>) {
        Napier.d("Analytics Event: $name, params: $parameters")
    }

    actual fun setUserId(userId: String) {
        try {
            IosCrashlyticsBridge.onSetUserId?.invoke(userId)
        } catch (e: Throwable) {
            Napier.w("Failed to set Crashlytics userId: ${e.message}")
        }
        Napier.d("Analytics setUserId: $userId")
    }

    actual fun logException(exception: Throwable) {
        try {
            val name = exception::class.simpleName ?: "KotlinException"
            val message = exception.message ?: (exception::class.simpleName ?: "Unknown error")
            val stackTrace = exception.stackTraceToString()
            IosCrashlyticsBridge.onRecordError?.invoke(name, message, stackTrace)
        } catch (e: Throwable) {
            Napier.w("Failed to record error to Crashlytics: ${e.message}")
        }
        Napier.e("Analytics logException: ${exception.message}", exception)
    }
}
