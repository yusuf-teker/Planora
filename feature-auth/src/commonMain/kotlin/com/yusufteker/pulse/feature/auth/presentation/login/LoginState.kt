package com.yusufteker.pulse.feature.auth.presentation.login

import com.yusufteker.pulse.core.base.UiState
import com.yusufteker.pulse.core.ui.text.UiText

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
