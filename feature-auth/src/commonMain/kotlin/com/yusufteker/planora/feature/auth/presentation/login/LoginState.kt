package com.yusufteker.planora.feature.auth.presentation.login

import com.yusufteker.planora.core.base.UiState
import com.yusufteker.planora.core.ui.text.UiText

/**
 * UI state for the Login screen.
 */
data class LoginState(
    val identifier: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val identifierError: UiText? = null,
    val passwordError: UiText? = null
) : UiState
