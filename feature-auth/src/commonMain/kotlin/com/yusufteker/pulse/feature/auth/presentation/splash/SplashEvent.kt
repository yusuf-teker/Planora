package com.yusufteker.pulse.feature.auth.presentation.splash

import com.yusufteker.pulse.core.base.UiEvent

/**
 * UI events for the Splash screen.
 */
sealed interface SplashEvent : UiEvent {
    /** Triggered when the splash animation completes */
    data object AnimationCompleted : SplashEvent
}
