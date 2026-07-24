package com.yusufteker.planora.core.analytics

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.analytics
import dev.gitlive.firebase.crashlytics.crashlytics

actual class AnalyticsManager {
    actual fun logEvent(name: String, parameters: Map<String, Any>) {
        Firebase.analytics.logEvent(name, parameters)
    }

    actual fun setUserId(userId: String) {
        Firebase.analytics.setUserId(userId)
        Firebase.crashlytics.setUserId(userId)
    }

    actual fun logException(exception: Throwable) {
        Firebase.crashlytics.recordException(exception)
    }
}
