package com.yusufteker.pulse.feature.auth.presentation.login

import com.yusufteker.pulse.core.base.BaseViewModel
import com.yusufteker.pulse.feature.auth.domain.usecase.LoginUseCase
import com.yusufteker.pulse.shared.api.AuthRequest

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
                if (validateForm()) {
                    // Yükleme animasyonunu başlat
                    setState { copy(isLoading = true) }
                    
                    // Arka planda sunucuya istek at (Coroutine Launch)
                    launch {
                        val result = loginUseCase(AuthRequest(currentState.email, currentState.password))
                        
                        // İşlem bittiğinde yükleme animasyonunu durdur
                        setState { copy(isLoading = false) }
                        
                        result.fold(
                            onSuccess = {
                                // Başarılıysa doğrudan ana sayfaya yönlendir
                                setEffect(LoginEffect.NavigateToHome)
                            },
                            onFailure = { error ->
                                // Hata durumunda UI'da hatayı göster
                                setState { copy(emailError = "Giriş başarısız: ${error.message}") }
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
