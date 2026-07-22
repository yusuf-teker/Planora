package com.yusufteker.pulse.feature.auth.presentation.forgot_password

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufteker.pulse.core.base.CollectEffect
import com.yusufteker.pulse.core.navigation.LocalNavigator
import com.yusufteker.pulse.core.navigation.Screen
import com.yusufteker.pulse.core.theme.PulsyTheme
import org.jetbrains.compose.resources.stringResource
import pulsy.core.generated.resources.Res
import pulsy.core.generated.resources.action_reset_password
import pulsy.core.generated.resources.action_send_code
import pulsy.core.generated.resources.email
import pulsy.core.generated.resources.forgot_password_step2_subtitle
import pulsy.core.generated.resources.forgot_password_subtitle
import pulsy.core.generated.resources.forgot_password_title
import pulsy.core.generated.resources.hide_password
import pulsy.core.generated.resources.new_password_label
import pulsy.core.generated.resources.reset_code_label
import pulsy.core.generated.resources.show_password

/**
 * Screen composable for the Forgot Password & Reset Password workflow.
 *
 * Steps user through entering email, receiving 6-digit OTP code, and resetting password.
 */
@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel
) {
    val navigator = LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    viewModel.effect.CollectEffect { effect ->
        when (effect) {
            is ForgotPasswordEffect.NavigateToLogin -> navigator.navigate(Screen.Login)
            is ForgotPasswordEffect.ShowToast -> {
                // Toast or snackbar can be shown here
            }
        }
    }

    ForgotPasswordContent(
        state = state,
        onEvent = viewModel::onEvent
    )
}

@Composable
private fun ForgotPasswordContent(
    state: ForgotPasswordState,
    onEvent: (ForgotPasswordEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeContentPadding()
            .padding(24.dp)
    ) {
        // Top Bar Back Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (state.isCodeSent) {
                        onEvent(ForgotPasswordEvent.BackToEmailClicked)
                    } else {
                        onEvent(ForgotPasswordEvent.BackToLoginClicked)
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title & Description
        Text(
            text = stringResource(Res.string.forgot_password_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (state.isCodeSent) {
                stringResource(Res.string.forgot_password_step2_subtitle)
            } else {
                stringResource(Res.string.forgot_password_subtitle)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (!state.isCodeSent) {
            // STEP 1: Enter Email
            OutlinedTextField(
                value = state.email,
                onValueChange = { onEvent(ForgotPasswordEvent.EmailChanged(it)) },
                label = { Text(stringResource(Res.string.email)) },
                isError = state.emailError != null,
                supportingText = state.emailError?.let { error ->
                    { Text(error.asString()) }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onEvent(ForgotPasswordEvent.SendCodeClicked) },
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.action_send_code),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        } else {
            // STEP 2: Enter 6-digit Code & New Password
            OutlinedTextField(
                value = state.code,
                onValueChange = { if (it.length <= 6) onEvent(ForgotPasswordEvent.CodeChanged(it)) },
                label = { Text(stringResource(Res.string.reset_code_label)) },
                isError = state.codeError != null,
                supportingText = state.codeError?.let { error ->
                    { Text(error.asString()) }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.newPassword,
                onValueChange = { onEvent(ForgotPasswordEvent.NewPasswordChanged(it)) },
                label = { Text(stringResource(Res.string.new_password_label)) },
                isError = state.passwordError != null,
                supportingText = state.passwordError?.let { error ->
                    { Text(error.asString()) }
                },
                visualTransformation = if (state.isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(
                        onClick = { onEvent(ForgotPasswordEvent.TogglePasswordVisibility) }
                    ) {
                        Icon(
                            imageVector = if (state.isPasswordVisible) {
                                Icons.Rounded.VisibilityOff
                            } else {
                                Icons.Rounded.Visibility
                            },
                            contentDescription = if (state.isPasswordVisible) {
                                stringResource(Res.string.hide_password)
                            } else {
                                stringResource(Res.string.show_password)
                            }
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onEvent(ForgotPasswordEvent.ResetPasswordClicked) },
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.action_reset_password),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = { onEvent(ForgotPasswordEvent.BackToEmailClicked) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = state.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}


