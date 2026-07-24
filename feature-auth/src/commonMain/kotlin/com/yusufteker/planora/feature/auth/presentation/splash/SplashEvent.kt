package com.yusufteker.planora.feature.auth.presentation.splash

import com.yusufteker.planora.core.base.UiEvent

/**
 * UI events for the Splash screen.
 */
sealed interface SplashEvent : UiEvent {
    /** Triggered when the splash animation completes */
    data object AnimationCompleted : SplashEvent
}
