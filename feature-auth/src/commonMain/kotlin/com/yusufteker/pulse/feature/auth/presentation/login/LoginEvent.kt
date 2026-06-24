package com.yusufteker.pulse.feature.auth.presentation.login

import com.yusufteker.pulse.core.base.UiEvent

/**
 * UI events for the Login screen.
 */
sealed interface LoginEvent : UiEvent {
    data class EmailChanged(val email: String) : LoginEvent
    data class PasswordChanged(val password: String) : LoginEvent
    data object TogglePasswordVisibility : LoginEvent
    data object LoginClicked : LoginEvent
    data object RegisterClicked : LoginEvent
    data object ClearForm : LoginEvent
}
