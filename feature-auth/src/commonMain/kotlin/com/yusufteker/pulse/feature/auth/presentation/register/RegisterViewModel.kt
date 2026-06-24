package com.yusufteker.pulse.feature.auth.presentation.register

import com.yusufteker.pulse.core.base.BaseViewModel

/**
 * ViewModel for the Register screen.
 *
 * Handles form validation and registration flow.
 * Repository integration will be added in future phases.
 */
class RegisterViewModel : BaseViewModel<RegisterState, RegisterEvent, RegisterEffect>(
    initialState = RegisterState()
) {

    override fun onEvent(event: RegisterEvent) {
        when (event) {
            is RegisterEvent.NameChanged -> {
                setState { copy(name = event.name, nameError = null) }
            }

            is RegisterEvent.EmailChanged -> {
                setState { copy(email = event.email, emailError = null) }
            }

            is RegisterEvent.PasswordChanged -> {
                setState { copy(password = event.password, passwordError = null) }
            }

            is RegisterEvent.ConfirmPasswordChanged -> {
                setState { copy(confirmPassword = event.confirmPassword, confirmPasswordError = null) }
            }

            is RegisterEvent.TogglePasswordVisibility -> {
                setState { copy(isPasswordVisible = !isPasswordVisible) }
            }

            is RegisterEvent.RegisterClicked -> {
                // TODO: Implement registration with repository
                if (validateForm()) {
                    setState { copy(isLoading = true) }
                    setEffect(RegisterEffect.NavigateToHome)
                }
            }

            is RegisterEvent.LoginClicked -> {
                setEffect(RegisterEffect.NavigateBack)
            }
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        if (currentState.name.isBlank()) {
            setState { copy(nameError = "İsim gerekli") }
            isValid = false
        }

        if (currentState.email.isBlank()) {
            setState { copy(emailError = "E-posta adresi gerekli") }
            isValid = false
        }

        if (currentState.password.length < 6) {
            setState { copy(passwordError = "Şifre en az 6 karakter olmalı") }
            isValid = false
        }

        if (currentState.password != currentState.confirmPassword) {
            setState { copy(confirmPasswordError = "Şifreler eşleşmiyor") }
            isValid = false
        }

        return isValid
    }
}
