package com.yusufteker.planora.feature.auth.presentation.register

import com.yusufteker.planora.core.base.BaseViewModel
import com.yusufteker.planora.core.ui.text.UiText
import com.yusufteker.planora.core.util.toUiText
import com.yusufteker.planora.feature.auth.domain.usecase.RegisterUseCase
import com.yusufteker.planora.feature.auth.domain.usecase.SendRegisterCodeUseCase
import com.yusufteker.planora.shared.api.RegisterRequest
import planora.core.generated.resources.Res
import planora.core.generated.resources.error_email_required
import planora.core.generated.resources.error_enter_code
import planora.core.generated.resources.error_name_required
import planora.core.generated.resources.error_username_required
import planora.core.generated.resources.error_password_short
import planora.core.generated.resources.error_passwords_mismatch
import planora.core.generated.resources.error_register_failed

/**
 * ViewModel for the Register screen.
 */
class RegisterViewModel(
    private val registerUseCase: RegisterUseCase,
    private val sendRegisterCodeUseCase: SendRegisterCodeUseCase
) : BaseViewModel<RegisterState, RegisterEvent, RegisterEffect>(
    initialState = RegisterState()
) {

    override fun onEvent(event: RegisterEvent) {
        when (event) {
            is RegisterEvent.NameChanged -> {
                setState { copy(name = event.name, nameError = null) }
            }

            is RegisterEvent.UsernameChanged -> {
                setState { copy(username = event.username, usernameError = null) }
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

            is RegisterEvent.CodeChanged -> {
                setState { copy(code = event.code, codeError = null) }
            }

            is RegisterEvent.TogglePasswordVisibility -> {
                setState { copy(isPasswordVisible = !isPasswordVisible) }
            }

            is RegisterEvent.SendCodeClicked -> {
                if (validateForm()) {
                    setState { copy(isLoading = true) }

                    launch {
                        val result = sendRegisterCodeUseCase(currentState.email, currentState.username)
                        setState { copy(isLoading = false) }

                        result.fold(
                            onSuccess = {
                                setState { copy(isCodeSent = true) }
                            },
                            onFailure = { error ->
                                setState { copy(emailError = error.toUiText()) }
                            }
                        )
                    }
                }
            }

            is RegisterEvent.RegisterClicked -> {
                if (currentState.code.isBlank() || currentState.code.length != 6) {
                    setState { copy(codeError = UiText.StringResourceId(Res.string.error_enter_code)) }
                    return
                }

                setState { copy(isLoading = true) }

                launch {
                    val request = RegisterRequest(
                        name = currentState.name,
                        username = currentState.username,
                        email = currentState.email,
                        password = currentState.password,
                        code = currentState.code
                    )
                    val result = registerUseCase(request)

                    setState { copy(isLoading = false) }

                    result.fold(
                        onSuccess = {
                            setEffect(RegisterEffect.NavigateToHome)
                        },
                        onFailure = { error ->
                            setState { copy(codeError = error.toUiText()) }
                        }
                    )
                }
            }

            is RegisterEvent.BackToFormClicked -> {
                setState { copy(isCodeSent = false, code = "", codeError = null) }
            }

            is RegisterEvent.LoginClicked -> {
                setEffect(RegisterEffect.NavigateBack)
            }

            is RegisterEvent.ClearForm -> {
                setState { RegisterState() }
            }
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        if (currentState.name.isBlank()) {
            setState { copy(nameError = UiText.StringResourceId(Res.string.error_name_required)) }
            isValid = false
        }

        if (currentState.username.isBlank()) {
            setState { copy(usernameError = UiText.StringResourceId(Res.string.error_username_required)) }
            isValid = false
        }

        if (currentState.email.isBlank() || !currentState.email.contains("@")) {
            setState { copy(emailError = UiText.StringResourceId(Res.string.error_email_required)) }
            isValid = false
        }

        if (currentState.password.length < 6) {
            setState { copy(passwordError = UiText.StringResourceId(Res.string.error_password_short)) }
            isValid = false
        }

        if (currentState.password != currentState.confirmPassword) {
            setState { copy(confirmPasswordError = UiText.StringResourceId(Res.string.error_passwords_mismatch)) }
            isValid = false
        }

        return isValid
    }
}
