package com.yusufteker.pulse.feature.auth.presentation.login

import com.yusufteker.pulse.core.base.UiEffect

/**
 * Side effects for the Login screen.
 */
sealed interface LoginEffect : UiEffect {
    data object NavigateToHome : LoginEffect
    data object NavigateToRegister : LoginEffect
    data class ShowError(val message: String) : LoginEffect
}
