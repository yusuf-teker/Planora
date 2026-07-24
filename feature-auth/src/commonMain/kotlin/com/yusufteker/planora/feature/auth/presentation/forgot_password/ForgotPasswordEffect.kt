package com.yusufteker.planora.feature.auth.presentation.forgot_password

import com.yusufteker.planora.core.base.UiEffect
import com.yusufteker.planora.core.ui.text.UiText

/**
 * One-time side effects for the Forgot Password screen.
 */
sealed interface ForgotPasswordEffect : UiEffect {
    data object NavigateToLogin : ForgotPasswordEffect
    data class ShowToast(val message: UiText) : ForgotPasswordEffect
}

