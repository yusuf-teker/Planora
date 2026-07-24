package com.yusufteker.planora

import androidx.compose.ui.window.ComposeUIViewController
import com.yusufteker.planora.di.initKoin

/**
 * iOS entry point.
 *
 * Initializes Koin and returns a ComposeUIViewController
 * hosting the shared [App] composable.
 */
fun MainViewController() = ComposeUIViewController(
    configure = {
        io.github.aakira.napier.Napier.base(io.github.aakira.napier.DebugAntilog())
        initKoin()
    }
) {
    App()
}
