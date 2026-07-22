package com.yusufteker.pulse.feature.auth.presentation.forgot_password

import com.yusufteker.pulse.core.base.UiEffect
import com.yusufteker.pulse.core.ui.text.UiText

/**
 * One-time side effects for the Forgot Password screen.
 */
sealed interface ForgotPasswordEffect : UiEffect {
    data object NavigateToLogin : ForgotPasswordEffect
    data class ShowToast(val message: UiText) : ForgotPasswordEffect
}

