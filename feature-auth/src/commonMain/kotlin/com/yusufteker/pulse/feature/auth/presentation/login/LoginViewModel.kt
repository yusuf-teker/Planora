package com.yusufteker.pulse.feature.auth.presentation.login

import com.yusufteker.pulse.core.base.BaseViewModel

/**
 * ViewModel for the Login screen.
 *
 * Handles form validation and login flow.
 * Repository integration will be added in future phases.
 */
class LoginViewModel : BaseViewModel<LoginState, LoginEvent, LoginEffect>(
    initialState = LoginState()
) {

    override fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged -> {
                setState { copy(email = event.email, emailError = null) }
            }

            is LoginEvent.PasswordChanged -> {
                setState { copy(password = event.password, passwordError = null) }
            }

            is LoginEvent.TogglePasswordVisibility -> {
                setState { copy(isPasswordVisible = !isPasswordVisible) }
            }

            is LoginEvent.LoginClicked -> {
                // TODO: Implement login with repository
                // For now, navigate to Home directly
                if (validateForm()) {
                    setState { copy(isLoading = true) }
                    setEffect(LoginEffect.NavigateToHome)
                }
            }

            is LoginEvent.RegisterClicked -> {
                setEffect(LoginEffect.NavigateToRegister)
            }
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        if (currentState.email.isBlank()) {
            setState { copy(emailError = "E-posta adresi gerekli") }
            isValid = false
        }

        if (currentState.password.isBlank()) {
            setState { copy(passwordError = "Şifre gerekli") }
            isValid = false
        }

        return isValid
    }
}
