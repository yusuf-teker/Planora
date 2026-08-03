package com.yusufteker.planora.core.utils

import android.content.Context

actual object NotificationSyncBridge {
    var onWidgetUpdateRequested: (() -> Unit)? = null

    actual fun setSyncHandler(handler: suspend () -> Unit) {
    }

    actual fun triggerWidgetUpdate() {
        try {
            onWidgetUpdateRequested?.invoke()

            val context = org.koin.core.context.GlobalContext.getOrNull()?.get<Context>()
            if (context != null) {
                val clazz = Class.forName("com.yusufteker.planora.android.widget.PlanoraWidgetUpdater")
                val method = clazz.getMethod("updateAllWidgets", Context::class.java)
                method.invoke(null, context)
            }
        } catch (e: Throwable) {
            // Ignore reflection errors if handler already handled or class unavailable
        }
    }
}

