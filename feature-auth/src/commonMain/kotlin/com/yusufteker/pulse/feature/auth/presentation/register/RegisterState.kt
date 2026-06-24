package com.yusufteker.pulse.feature.auth.presentation.register

import com.yusufteker.pulse.core.base.UiState

/**
 * UI state for the Register screen.
 */
data class RegisterState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null
) : UiState
