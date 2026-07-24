package com.yusufteker.planora.core.analytics

import io.github.aakira.napier.Napier

actual class AnalyticsManager {
    actual fun logEvent(name: String, parameters: Map<String, Any>) {
        // Firebase Analytics events are handled natively via FirebaseApp.configure() in AppDelegate.
        Napier.d("Analytics Event: $name, params: $parameters")
    }

    actual fun setUserId(userId: String) {
        // Firebase Crashlytics userId is set natively.
        Napier.d("Analytics setUserId: $userId")
    }

    actual fun logException(exception: Throwable) {
        // Firebase Crashlytics records exceptions automatically on iOS.
        Napier.e("Analytics logException: ${exception.message}", exception)
    }
}
