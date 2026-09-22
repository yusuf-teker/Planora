package com.yusufteker.planora.feature.home.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufteker.planora.core.base.CollectEffect
import com.yusufteker.planora.core.navigation.LocalMainNavigator
import com.yusufteker.planora.core.navigation.LocalNavigator
import com.yusufteker.planora.core.navigation.Screen
import com.yusufteker.planora.core.preferences.ThemeColor
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

        val sessionPreferences = org.koin.compose.koinInject<com.yusufteker.planora.core.preferences.SessionPreferences>()
        val isPremiumUser by sessionPreferences.isPremiumFlow.collectAsStateWithLifecycle(initialValue = false)

        // Planora Premium Banner
        androidx.compose.material3.Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .clickable { rootNavigator.navigate(Screen.Premium) },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = if (isPremiumUser) {
                    androidx.compose.ui.graphics.Color(0xFFFFD700).copy(alpha = 0.15f)
                } else {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                }
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = if (isPremiumUser) androidx.compose.ui.graphics.Color(0xFFFFD700) else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPremiumUser) androidx.compose.ui.graphics.Color(0xFFFFD700)
                            else MaterialTheme.colorScheme.primary
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isPremiumUser) stringResource(Res.string.settings_premium_active_title)
                               else stringResource(Res.string.settings_premium_card_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isPremiumUser) stringResource(Res.string.settings_premium_active_desc)
                               else stringResource(Res.string.settings_premium_card_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

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
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val themeOptions = remember {
                listOf(
                    ThemeColorOption(ThemeColor.DEFAULT, Res.string.theme_color_default, listOf(Color(0xFF1D9BF0), Color(0xFF00CEC9))),
                    ThemeColorOption(ThemeColor.BLUE, Res.string.theme_color_blue, listOf(Color(0xFF6C5CE7), Color(0xFF8E84E8))),
                    ThemeColorOption(ThemeColor.RED, Res.string.theme_color_red, listOf(Color(0xFFE63946), Color(0xFFFF6B6B))),
                    ThemeColorOption(ThemeColor.GREEN, Res.string.theme_color_green, listOf(Color(0xFF2A9D8F), Color(0xFF52B788))),
                    ThemeColorOption(ThemeColor.PURPLE, Res.string.theme_color_purple, listOf(Color(0xFF9D4EDD), Color(0xFFC77DFF))),
                    ThemeColorOption(ThemeColor.PINK, Res.string.theme_color_pink, listOf(Color(0xFFE83E8C), Color(0xFFFF758F))),
                    ThemeColorOption(ThemeColor.ORANGE, Res.string.theme_color_orange, listOf(Color(0xFFF4A261), Color(0xFFE76F51))),
                    ThemeColorOption(ThemeColor.TEAL, Res.string.theme_color_teal, listOf(Color(0xFF00B4D8), Color(0xFF48CAE4))),
                    ThemeColorOption(ThemeColor.INDIGO, Res.string.theme_color_indigo, listOf(Color(0xFF3F37C9), Color(0xFF4895EF))),
                    ThemeColorOption(ThemeColor.AMBER, Res.string.theme_color_amber, listOf(Color(0xFFFFB703), Color(0xFFFB8500))),
                    ThemeColorOption(ThemeColor.BROWN, Res.string.theme_color_brown, listOf(Color(0xFF7F4F24), Color(0xFF936639)))
                )
            }

            val chunkedOptions = remember(themeOptions) { themeOptions.chunked(4) }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                chunkedOptions.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        rowItems.forEach { option ->
                            val isSelected = state.themeColor == option.color
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                    .clickable { viewModel.onEvent(SettingsEvent.ThemeColorSelected(option.color)) }
                                    .padding(vertical = 4.dp, horizontal = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            brush = if (isSelected) {
                                                Brush.linearGradient(option.previewGradient)
                                            } else {
                                                androidx.compose.ui.graphics.SolidColor(
                                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                                )
                                            },
                                            shape = CircleShape
                                        )
                                        .padding(if (isSelected) 4.dp else 2.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(option.previewGradient)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = stringResource(option.titleRes),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        val emptySlots = 4 - rowItems.size
                        if (emptySlots > 0) {
                            repeat(emptySlots) {
                                Spacer(modifier = Modifier.weight(1f).padding(horizontal = 2.dp))
                            }
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

/**
 * Encapsulates theme color configuration for the settings palette.
 */
private data class ThemeColorOption(
    val color: ThemeColor,
    val titleRes: org.jetbrains.compose.resources.StringResource,
    val previewGradient: List<Color>
)

