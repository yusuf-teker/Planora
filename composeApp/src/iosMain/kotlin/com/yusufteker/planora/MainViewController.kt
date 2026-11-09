package com.yusufteker.planora

import androidx.compose.ui.window.ComposeUIViewController
import com.yusufteker.planora.core.utils.NotificationSyncBridge
import com.yusufteker.planora.di.initKoin
import com.yusufteker.planora.feature.home.domain.repository.PlanRepository
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private var isNapierInitialized = false

/**
 * Initializes Planora core services on iOS (Napier logging, Koin DI, background sync handlers).
 * This must be called from Swift's `AppDelegate.didFinishLaunchingWithOptions` to ensure that
 * background push notifications or system wakeups can safely access DI dependencies without crashing.
 *
 * Safe to call multiple times.
 */
@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
fun initPlanora() {
    try {
        if (!isNapierInitialized) {
            Napier.base(DebugAntilog())
            isNapierInitialized = true
        }
        initKoin()
        setupIosBackgroundSync()
        
        // Hook uncaught fatal Kotlin exceptions to Crashlytics
        kotlin.native.setUnhandledExceptionHook { throwable ->
            try {
                val name = throwable::class.simpleName ?: "FatalKotlinException"
                val message = throwable.message ?: "Fatal uncaught Kotlin exception"
                val stack = throwable.stackTraceToString()
                com.yusufteker.planora.core.analytics.IosCrashlyticsBridge.onRecordError?.invoke(name, message, stack)
                Napier.e("FATAL UNCAUGHT KOTLIN EXCEPTION: $message", throwable, tag = "Crashlytics")
            } catch (e: Throwable) {
                println("Error in Crashlytics unhandledExceptionHook: ${e.message}")
            }
        }
    } catch (e: Throwable) {
        println("Planora initPlanora error: ${e.message}")
    }
}

private object IosSyncHolder : KoinComponent {
    val planRepository: PlanRepository by inject()
}

private fun setupIosBackgroundSync() {
    try {
        NotificationSyncBridge.setSyncHandler {
            try {
                IosSyncHolder.planRepository.fetchMyTasks()
            } catch (e: Throwable) {
                Napier.e("Background task sync failed: ${e.message}", e, tag = "IosSync")
            }
        }
        NotificationSyncBridge.setRoomSyncHandler {
            try {
                IosSyncHolder.planRepository.fetchMyRooms()
            } catch (e: Throwable) {
                Napier.e("Background room sync failed: ${e.message}", e, tag = "IosSync")
            }
        }
    } catch (e: Throwable) {
        Napier.e("setupIosBackgroundSync failed: ${e.message}", e, tag = "IosSync")
    }
}

/**
 * iOS entry point.
 *
 * Initializes Koin and returns a ComposeUIViewController
 * hosting the shared [App] composable.
 */
fun MainViewController() = ComposeUIViewController(
    configure = {
        initPlanora()
    }
) {
    App()
}
