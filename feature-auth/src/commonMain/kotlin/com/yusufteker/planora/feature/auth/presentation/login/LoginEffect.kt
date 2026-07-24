package com.yusufteker.planora.feature.auth.presentation.login

import com.yusufteker.planora.core.base.UiEffect

/**
 * Side effects for the Login screen.
 */
sealed interface LoginEffect : UiEffect {
    data object NavigateToHome : LoginEffect
    data object NavigateToRegister : LoginEffect
    data class ShowError(val message: String) : LoginEffect
}
