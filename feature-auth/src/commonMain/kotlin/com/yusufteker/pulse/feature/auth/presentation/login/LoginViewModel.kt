package com.yusufteker.pulse.feature.auth.presentation.login

import com.yusufteker.pulse.core.base.BaseViewModel
import com.yusufteker.pulse.core.ui.text.UiText
import com.yusufteker.pulse.feature.auth.domain.usecase.LoginUseCase
import com.yusufteker.pulse.shared.api.AuthRequest
import pulse.core.generated.resources.Res
import pulse.core.generated.resources.error_email_required
import pulse.core.generated.resources.error_login_failed
import pulse.core.generated.resources.error_password_required

/**
 * ViewModel for the Login screen.
 */
class LoginViewModel(
    private val loginUseCase: LoginUseCase
) : BaseViewModel<LoginState, LoginEvent, LoginEffect>(
    initialState = LoginState()
) {

    override fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.IdentifierChanged -> {
                setState { copy(identifier = event.identifier, identifierError = null) }
            }

            is LoginEvent.PasswordChanged -> {
                setState { copy(password = event.password, passwordError = null) }
            }

            is LoginEvent.TogglePasswordVisibility -> {
                setState { copy(isPasswordVisible = !isPasswordVisible) }
            }

            is LoginEvent.LoginClicked -> {
                if (validateForm()) {
                    // Yükleme animasyonunu başlat
                    setState { copy(isLoading = true) }
                    
                    // Arka planda sunucuya istek at (Coroutine Launch)
                    launch {
                        val result = loginUseCase(AuthRequest(currentState.identifier, currentState.password))
                        
                        // İşlem bittiğinde yükleme animasyonunu durdur
                        setState { copy(isLoading = false) }
                        
                        result.fold(
                            onSuccess = {
                                // Başarılıysa doğrudan ana sayfaya yönlendir
                                setEffect(LoginEffect.NavigateToHome)
                            },
                            onFailure = { error ->
                                // Hata durumunda UI'da hatayı göster
                                setState { copy(identifierError = UiText.StringResourceId(Res.string.error_login_failed, error.message ?: "Unknown")) }
                            }
                        )
                    }
                }
            }

            is LoginEvent.RegisterClicked -> {
                setEffect(LoginEffect.NavigateToRegister)
            }

            is LoginEvent.ClearForm -> {
                setState { LoginState() }
            }
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        if (currentState.identifier.isBlank()) {
            setState { copy(identifierError = UiText.StringResourceId(Res.string.error_email_required)) }
            isValid = false
        }

        if (currentState.password.isBlank()) {
            setState { copy(passwordError = UiText.StringResourceId(Res.string.error_password_required)) }
            isValid = false
        }

        return isValid
    }
}
