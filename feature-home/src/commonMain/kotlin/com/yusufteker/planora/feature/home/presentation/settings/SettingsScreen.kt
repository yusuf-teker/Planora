package com.yusufteker.planora.feature.home.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufteker.planora.core.base.CollectEffect
import com.yusufteker.planora.core.navigation.LocalMainNavigator
import com.yusufteker.planora.core.navigation.LocalNavigator
import com.yusufteker.planora.core.navigation.Screen
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.SwitchDefaults

/**
 * Settings screen composable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val mainNavigator = LocalMainNavigator.current
    val rootNavigator = LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    viewModel.effect.CollectEffect { effect ->
        when (effect) {
            is SettingsEffect.NavigateBack -> mainNavigator.pop()
            is SettingsEffect.NavigateToLogin -> rootNavigator.setRoot(Screen.Login)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .safeContentPadding()
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(Res.string.tab_settings),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        // Dark mode toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.settings_dark_mode_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(Res.string.settings_dark_mode_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = state.isDarkMode,
                onCheckedChange = {
                    viewModel.onEvent(SettingsEvent.DarkModeToggled(it))
                },
                colors = SwitchDefaults.colors(
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                    uncheckedIconColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 24.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Color Theme selector
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = stringResource(Res.string.settings_theme_color),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Grid of colors
            LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                items(com.yusufteker.planora.core.preferences.ThemeColor.values().size) { index ->
                    val themeColor = com.yusufteker.planora.core.preferences.ThemeColor.values()[index]
                    val isSelected = state.themeColor == themeColor
                    val colorHex = when(themeColor) {
                        com.yusufteker.planora.core.preferences.ThemeColor.DEFAULT -> null
                        com.yusufteker.planora.core.preferences.ThemeColor.BLUE -> 0xFF6C5CE7
                        com.yusufteker.planora.core.preferences.ThemeColor.RED -> 0xFFE63946
                        com.yusufteker.planora.core.preferences.ThemeColor.GREEN -> 0xFF2A9D8F
                        com.yusufteker.planora.core.preferences.ThemeColor.PURPLE -> 0xFF9D4EDD
                        com.yusufteker.planora.core.preferences.ThemeColor.PINK -> 0xFFE83E8C
                        com.yusufteker.planora.core.preferences.ThemeColor.ORANGE -> 0xFFF4A261
                        com.yusufteker.planora.core.preferences.ThemeColor.TEAL -> 0xFF00B4D8
                        com.yusufteker.planora.core.preferences.ThemeColor.INDIGO -> 0xFF3F37C9
                        com.yusufteker.planora.core.preferences.ThemeColor.AMBER -> 0xFFFFB703
                        com.yusufteker.planora.core.preferences.ThemeColor.BROWN -> 0xFF7F4F24
                    }
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .background(
                                color = if (colorHex != null) androidx.compose.ui.graphics.Color(colorHex) else MaterialTheme.colorScheme.surfaceVariant,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.onBackground else androidx.compose.ui.graphics.Color.Transparent,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                            .clickable {
                                viewModel.onEvent(SettingsEvent.ThemeColorSelected(themeColor))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (colorHex == null) {
                            androidx.compose.material3.Text(
                                text = "✕",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Logout button
        TextButton(
            onClick = { viewModel.onEvent(SettingsEvent.LogoutClicked) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp)
        ) {
            Text(
                text = stringResource(Res.string.action_logout),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Delete account button
        TextButton(
            onClick = { viewModel.onEvent(SettingsEvent.DeleteAccountClicked) },
            enabled = !state.isDeletingAccount,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp)
        ) {
            if (state.isDeletingAccount) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.padding(end = 8.dp),
                    color = MaterialTheme.colorScheme.error,
                    strokeWidth = 2.dp
                )
            }
            Text(
                text = stringResource(Res.string.settings_delete_account),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(110.dp))


        if (state.showDeleteConfirmDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { viewModel.onEvent(SettingsEvent.DeleteAccountDismissed) },
                title = {
                    Text(text = stringResource(Res.string.settings_delete_account_confirm_title))
                },
                text = {
                    Text(text = stringResource(Res.string.settings_delete_account_confirm_desc))
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.onEvent(SettingsEvent.DeleteAccountConfirmed) }
                    ) {
                        Text(
                            text = stringResource(Res.string.action_delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.onEvent(SettingsEvent.DeleteAccountDismissed) }
                    ) {
                        Text(text = stringResource(Res.string.cancel))
                    }
                }
            )
        }
    }
}

