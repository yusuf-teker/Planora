package com.yusufteker.planora.core.utils

expect object NotificationSyncBridge {
    fun setSyncHandler(handler: suspend () -> Unit)
}
