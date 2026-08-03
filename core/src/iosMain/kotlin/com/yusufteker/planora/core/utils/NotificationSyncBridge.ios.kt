package com.yusufteker.planora.core.utils

import com.yusufteker.planora.core.widget.IosWidgetBridge

actual object NotificationSyncBridge {
    actual fun setSyncHandler(handler: suspend () -> Unit) {
        IosNotificationBridge.onSyncTasksRequested = handler
    }

    actual fun triggerWidgetUpdate() {
        try {
            IosWidgetBridge.syncWidgetData()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
