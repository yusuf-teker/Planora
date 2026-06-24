package com.yusufteker.pulse.feature.auth.presentation.register

import com.yusufteker.pulse.core.base.UiEvent

/**
 * UI events for the Register screen.
 */
sealed interface RegisterEvent : UiEvent {
    data class NameChanged(val name: String) : RegisterEvent
    data class EmailChanged(val email: String) : RegisterEvent
    data class PasswordChanged(val password: String) : RegisterEvent
    data class ConfirmPasswordChanged(val confirmPassword: String) : RegisterEvent
    data object TogglePasswordVisibility : RegisterEvent
    data object RegisterClicked : RegisterEvent
    data object LoginClicked : RegisterEvent
}
