package com.yusufteker.pulse

import androidx.compose.ui.window.ComposeUIViewController
import com.yusufteker.pulse.di.initKoin

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
