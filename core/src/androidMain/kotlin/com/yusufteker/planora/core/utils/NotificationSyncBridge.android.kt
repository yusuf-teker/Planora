package com.yusufteker.planora.core.utils

actual object NotificationSyncBridge {
    actual fun setSyncHandler(handler: suspend () -> Unit) {
        // Android handles background sync in PlanoraFcmService
    }
}
