package com.yusufteker.planora.core.analytics

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.analytics
import dev.gitlive.firebase.crashlytics.crashlytics

actual class AnalyticsManager {
    actual fun logEvent(name: String, parameters: Map<String, Any>) {
        try {
            Firebase.analytics.logEvent(name, parameters)
        } catch (_: Throwable) {}
    }

    actual fun setUserId(userId: String) {
        try {
            Firebase.analytics.setUserId(userId)
            Firebase.crashlytics.setUserId(userId)
        } catch (_: Throwable) {}
    }

    actual fun logException(exception: Throwable) {
        try {
            Firebase.crashlytics.recordException(exception)
        } catch (_: Throwable) {}
    }
}
