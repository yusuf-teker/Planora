package com.yusufteker.planora.feature.auth.presentation.register

import com.yusufteker.planora.core.base.UiEvent

/**
 * UI events for the Register screen.
 */
sealed interface RegisterEvent : UiEvent {
    data class NameChanged(val name: String) : RegisterEvent
    data class UsernameChanged(val username: String) : RegisterEvent
    data class EmailChanged(val email: String) : RegisterEvent
    data class PasswordChanged(val password: String) : RegisterEvent
    data class ConfirmPasswordChanged(val confirmPassword: String) : RegisterEvent
    data class CodeChanged(val code: String) : RegisterEvent
    data object TogglePasswordVisibility : RegisterEvent
    data object SendCodeClicked : RegisterEvent
    data object RegisterClicked : RegisterEvent
    data object LoginClicked : RegisterEvent
    data object BackToFormClicked : RegisterEvent
    data object ClearForm : RegisterEvent
}
