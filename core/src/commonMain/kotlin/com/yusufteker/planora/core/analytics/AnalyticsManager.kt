package com.yusufteker.planora.core.analytics

expect class AnalyticsManager() {
    fun logEvent(name: String, parameters: Map<String, Any> = emptyMap())
    fun setUserId(userId: String)
    fun logException(exception: Throwable)
}
