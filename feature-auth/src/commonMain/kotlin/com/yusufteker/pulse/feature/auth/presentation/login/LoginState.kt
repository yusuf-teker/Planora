package com.yusufteker.pulse.feature.auth.presentation.login

import com.yusufteker.pulse.core.base.UiState

/**
 * UI state for the Login screen.
 */
data class LoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null
) : UiState
