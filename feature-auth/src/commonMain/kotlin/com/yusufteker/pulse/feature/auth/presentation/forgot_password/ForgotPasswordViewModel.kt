package com.yusufteker.pulse.feature.auth.presentation.forgot_password

import com.yusufteker.pulse.core.base.BaseViewModel
import com.yusufteker.pulse.core.ui.text.UiText
import com.yusufteker.pulse.feature.auth.domain.repository.AuthRepository
import pulsy.core.generated.resources.Res
import pulsy.core.generated.resources.code_sent_success
import pulsy.core.generated.resources.error_enter_code
import pulsy.core.generated.resources.error_enter_valid_email
import pulsy.core.generated.resources.error_password_required
import pulsy.core.generated.resources.password_reset_success

/**
 * ViewModel managing the Forgot Password & Reset Password state flow.
 *
 * Communicates with [AuthRepository] to dispatch reset OTP codes via Resend
 * and apply new passwords.
 *
 * @param authRepository Repository handling network authentication requests.
 */
class ForgotPasswordViewModel(
    private val authRepository: AuthRepository
) : BaseViewModel<ForgotPasswordState, ForgotPasswordEvent, ForgotPasswordEffect>(
    initialState = ForgotPasswordState()
) {

    override fun onEvent(event: ForgotPasswordEvent) {
        when (event) {
            is ForgotPasswordEvent.EmailChanged -> {
                setState { copy(email = event.email, emailError = null) }
            }

            is ForgotPasswordEvent.CodeChanged -> {
                setState { copy(code = event.code, codeError = null) }
            }

            is ForgotPasswordEvent.NewPasswordChanged -> {
                setState { copy(newPassword = event.newPassword, passwordError = null) }
            }

            is ForgotPasswordEvent.TogglePasswordVisibility -> {
                setState { copy(isPasswordVisible = !isPasswordVisible) }
            }

            is ForgotPasswordEvent.SendCodeClicked -> {
                if (validateEmail()) {
                    setState { copy(isLoading = true) }
                    launch {
                        val result = authRepository.forgotPassword(currentState.email.trim())
                        setState { copy(isLoading = false) }

                        result.fold(
                            onSuccess = {
                                setState {
                                    copy(
                                        isCodeSent = true,
                                        successMessage = UiText.StringResourceId(Res.string.code_sent_success)
                                    )
                                }
                            },
                            onFailure = { error ->
                                setState {
                                    copy(emailError = UiText.DynamicString(error.message ?: "Failed to send reset code"))
                                }
                            }
                        )
                    }
                }
            }

            is ForgotPasswordEvent.ResetPasswordClicked -> {
                if (validateResetForm()) {
                    setState { copy(isLoading = true) }
                    launch {
                        val result = authRepository.resetPassword(
                            email = currentState.email.trim(),
                            code = currentState.code.trim(),
                            newPassword = currentState.newPassword
                        )
                        setState { copy(isLoading = false) }

                        result.fold(
                            onSuccess = {
                                setEffect(ForgotPasswordEffect.ShowToast(UiText.StringResourceId(Res.string.password_reset_success)))
                                setEffect(ForgotPasswordEffect.NavigateToLogin)
                            },
                            onFailure = { error ->
                                setState {
                                    copy(codeError = UiText.DynamicString(error.message ?: "Failed to reset password"))
                                }
                            }
                        )
                    }
                }
            }

            is ForgotPasswordEvent.BackToEmailClicked -> {
                setState { copy(isCodeSent = false, successMessage = null, codeError = null, passwordError = null) }
            }

            is ForgotPasswordEvent.BackToLoginClicked -> {
                setEffect(ForgotPasswordEffect.NavigateToLogin)
            }
        }
    }

    private fun validateEmail(): Boolean {
        val email = currentState.email.trim()
        if (email.isBlank() || !email.contains("@")) {
            setState { copy(emailError = UiText.StringResourceId(Res.string.error_enter_valid_email)) }
            return false
        }
        return true
    }

    private fun validateResetForm(): Boolean {
        var isValid = true

        if (currentState.code.trim().length != 6) {
            setState { copy(codeError = UiText.StringResourceId(Res.string.error_enter_code)) }
            isValid = false
        }

        if (currentState.newPassword.isBlank()) {
            setState { copy(passwordError = UiText.StringResourceId(Res.string.error_password_required)) }
            isValid = false
        }

        return isValid
    }
}
