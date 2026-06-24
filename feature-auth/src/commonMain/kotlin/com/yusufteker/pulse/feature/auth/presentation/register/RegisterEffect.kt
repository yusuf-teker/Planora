package com.yusufteker.pulse.feature.auth.presentation.register

import com.yusufteker.pulse.core.base.UiEffect

/**
 * Side effects for the Register screen.
 */
sealed interface RegisterEffect : UiEffect {
    data object NavigateToHome : RegisterEffect
    data object NavigateBack : RegisterEffect
    data class ShowError(val message: String) : RegisterEffect
}
