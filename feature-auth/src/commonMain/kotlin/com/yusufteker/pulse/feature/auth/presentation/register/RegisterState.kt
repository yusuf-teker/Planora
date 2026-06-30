package com.yusufteker.pulse.feature.auth.presentation.register

import com.yusufteker.pulse.core.base.UiState
import com.yusufteker.pulse.core.ui.text.UiText

/**
 * UI state for the Register screen.
 */
data class RegisterState(
    val isLoading: Boolean = false,
    val name: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val nameError: UiText? = null,
    val usernameError: UiText? = null,
    val emailError: UiText? = null,
    val passwordError: UiText? = null,
    val confirmPasswordError: UiText? = null
) : UiState
