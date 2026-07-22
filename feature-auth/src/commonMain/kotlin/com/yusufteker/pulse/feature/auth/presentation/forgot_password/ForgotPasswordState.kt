package com.yusufteker.pulse.feature.auth.presentation.forgot_password

import com.yusufteker.pulse.core.base.UiState
import com.yusufteker.pulse.core.ui.text.UiText

/**
 * UI State for the Forgot Password flow.
 *
 * @property email The email address typed by the user.
 * @property code The 6-digit verification code typed by the user.
 * @property newPassword The new password typed by the user.
 * @property isPasswordVisible Whether the new password text is visible or hidden.
 * @property isCodeSent Whether step 1 (sending code) is completed and step 2 (verifying code/setting new password) is active.
 * @property isLoading Indicates if a network request is in progress.
 * @property emailError Error text for email validation.
 * @property codeError Error text for code validation.
 * @property passwordError Error text for password validation.
 * @property successMessage Optional success message displayed to user.
 */
data class ForgotPasswordState(
    val email: String = "",
    val code: String = "",
    val newPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isCodeSent: Boolean = false,
    val isLoading: Boolean = false,
    val emailError: UiText? = null,
    val codeError: UiText? = null,
    val passwordError: UiText? = null,
    val successMessage: UiText? = null
) : UiState

