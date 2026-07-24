package com.yusufteker.planora.feature.auth.presentation.login

import com.yusufteker.planora.core.base.UiEvent

/**
 * UI events for the Login screen.
 */
sealed interface LoginEvent : UiEvent {
    data class IdentifierChanged(val identifier: String) : LoginEvent
    data class PasswordChanged(val password: String) : LoginEvent
    data object TogglePasswordVisibility : LoginEvent
    data object LoginClicked : LoginEvent
    data object RegisterClicked : LoginEvent
    data object ClearForm : LoginEvent
}
