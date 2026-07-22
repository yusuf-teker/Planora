package com.yusufteker.pulse.feature.auth.presentation.forgot_password

import com.yusufteker.pulse.core.base.UiEvent

/**
 * UI Events for the Forgot Password flow.
 */
sealed interface ForgotPasswordEvent : UiEvent {
    data class EmailChanged(val email: String) : ForgotPasswordEvent
    data class CodeChanged(val code: String) : ForgotPasswordEvent
    data class NewPasswordChanged(val newPassword: String) : ForgotPasswordEvent
    data object TogglePasswordVisibility : ForgotPasswordEvent
    data object SendCodeClicked : ForgotPasswordEvent
    data object ResetPasswordClicked : ForgotPasswordEvent
    data object BackToEmailClicked : ForgotPasswordEvent
    data object BackToLoginClicked : ForgotPasswordEvent
}

