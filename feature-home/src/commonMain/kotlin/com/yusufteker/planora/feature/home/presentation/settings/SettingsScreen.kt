package com.yusufteker.planora.feature.home.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufteker.planora.core.base.CollectEffect
import com.yusufteker.planora.core.navigation.LocalMainNavigator
import com.yusufteker.planora.core.navigation.LocalNavigator
import com.yusufteker.planora.core.navigation.Screen
import com.yusufteker.planora.core.preferences.SessionPreferences
import com.yusufteker.planora.core.preferences.ThemeColor
import com.yusufteker.planora.core.preferences.isPremiumTheme
import com.yusufteker.planora.core.theme.premiumColor
import com.yusufteker.planora.core.theme.premiumGradient
import com.yusufteker.planora.core.theme.resolveThemeColor
import com.yusufteker.planora.core.ui.components.GradientText
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import planora.core.generated.resources.Res
import planora.core.generated.resources.*
import planora.core.generated.resources.action_delete
import planora.core.generated.resources.action_logout
import planora.core.generated.resources.cancel
import planora.core.generated.resources.premium_feature_unlocked
import planora.core.generated.resources.settings_app_version
import planora.core.generated.resources.settings_dark_mode_desc
import planora.core.generated.resources.settings_dark_mode_title
import planora.core.generated.resources.settings_delete_account
import planora.core.generated.resources.settings_delete_account_confirm_desc
import planora.core.generated.resources.settings_delete_account_confirm_title
import planora.core.generated.resources.settings_notifications
import planora.core.generated.resources.settings_section_account
import planora.core.generated.resources.settings_section_app_info
import planora.core.generated.resources.settings_section_appearance
import planora.core.generated.resources.settings_subscription_free
import planora.core.generated.resources.settings_subscription_label
import planora.core.generated.resources.settings_subscription_pro_active
import planora.core.generated.resources.settings_theme_color
import planora.core.generated.resources.settings_theme_color_subtitle
import planora.core.generated.resources.settings_theme_primary_color
import planora.core.generated.resources.settings_theme_secondary_color
import planora.core.generated.resources.settings_trash_subtitle
import planora.core.generated.resources.settings_trash_title
import planora.core.generated.resources.export_tasks_title
import planora.core.generated.resources.export_tasks_desc
import planora.core.generated.resources.export_tasks_premium_label
import planora.core.generated.resources.tab_settings
import planora.core.generated.resources.theme_color_amber
import planora.core.generated.resources.theme_color_blue
import planora.core.generated.resources.theme_color_brown
import planora.core.generated.resources.theme_color_cyberpunk
import planora.core.generated.resources.theme_color_default
import planora.core.generated.resources.theme_color_green
import planora.core.generated.resources.theme_color_indigo
import planora.core.generated.resources.theme_color_midnight
import planora.core.generated.resources.theme_color_nordic
import planora.core.generated.resources.theme_color_orange
import planora.core.generated.resources.theme_color_pink
import planora.core.generated.resources.theme_color_purple
import planora.core.generated.resources.theme_color_red
import planora.core.generated.resources.theme_color_rose_gold
import planora.core.generated.resources.theme_color_teal

/**
 * Modern, dünya standartlarında Planora Ayarlar Ekranı.
 *
 * Kullanıcı profili özeti, VIP Üyelik durumu, görünüm/tema ayarları,
 * uygulama tercihleri ve hesap işlemlerini düzenli kart grupları halinde sunar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val mainNavigator = LocalMainNavigator.current
    val rootNavigator = LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    val sessionPreferences = koinInject<SessionPreferences>()
    val isPremiumUser by sessionPreferences.isPremiumFlow.collectAsStateWithLifecycle(initialValue = false)

    viewModel.effect.CollectEffect { effect ->
        when (effect) {
            is SettingsEffect.NavigateBack -> mainNavigator.pop()
            is SettingsEffect.NavigateToLogin -> rootNavigator.setRoot(Screen.Login)
            is SettingsEffect.NavigateToPremium -> rootNavigator.navigate(Screen.Premium)
            is SettingsEffect.NavigateToTrash -> rootNavigator.navigate(Screen.Trash)
            is SettingsEffect.NavigateToAnalytics -> rootNavigator.navigate(Screen.Analytics)
            is SettingsEffect.NavigateToPlanComparison -> rootNavigator.navigate(Screen.PlanComparison)
            is SettingsEffect.ExportTasks -> Unit
        }
    }

    val proCrimson = premiumColor
    val primaryHex = resolveThemeColor(state.themeColor)
    val secondaryHex = resolveThemeColor(state.secondaryThemeColor)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, topBar = {
            TopAppBar(
                title = {
                    GradientText(
                        text = stringResource(Res.string.tab_settings), colors = listOf(
                            primaryHex, secondaryHex
                        ), style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }, colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── 2. Abonelik Satırı (Minimalist & Şık) ──
            Card(
                modifier = Modifier.fillMaxWidth()
                    .clickable { rootNavigator.navigate(Screen.Premium) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isPremiumUser) proCrimson.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant.copy(
                        alpha = 0.4f
                    )
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsIconBox(
                        icon = Icons.Default.Star,
                        containerColor = if (isPremiumUser) proCrimson.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.12f
                        ),
                        iconTint = if (isPremiumUser) Color(0xFFFF3B30) else MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.settings_subscription_label),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isPremiumUser) {
                                stringResource(Res.string.settings_subscription_pro_active)
                            } else {
                                stringResource(Res.string.settings_subscription_free)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isPremiumUser) FontWeight.Bold else FontWeight.Normal,
                            color = if (isPremiumUser) Color(0xFFFF3B30) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // ── 3. Görünüm & Kişiselleştirme Grubu ──
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(Res.string.settings_section_appearance),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Karanlık Mod Satırı
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SettingsIconBox(
                                icon = Icons.Default.DarkMode,
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                iconTint = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(Res.string.settings_dark_mode_title),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(Res.string.settings_dark_mode_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = state.isDarkMode, onCheckedChange = {
                                    viewModel.onEvent(SettingsEvent.DarkModeToggled(it))
                                }, colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    checkedBorderColor = Color.Transparent,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(
                                        alpha = 0.5f
                                    )
                                )
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        // Tema Vurgu Rengi Başlığı ve Palet
                        var isThemePaletteExpanded by remember { mutableStateOf(false) }
                        val isPrimaryTab = state.activeThemeTab == ThemeColorTarget.PRIMARY
                        val chevronRotation by animateFloatAsState(
                            targetValue = if (isThemePaletteExpanded) 180f else 0f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                .clickable { isThemePaletteExpanded = !isThemePaletteExpanded }
                                .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                SettingsIconBox(
                                    icon = Icons.Default.Palette,
                                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                    iconTint = MaterialTheme.colorScheme.secondary
                                )

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(Res.string.settings_theme_color),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(Res.string.settings_theme_color_subtitle),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Dual color indicator pill
                                    Box(
                                        modifier = Modifier.size(width = 30.dp, height = 18.dp)
                                            .clip(RoundedCornerShape(9.dp)).background(
                                                Brush.horizontalGradient(
                                                    listOf(primaryHex, secondaryHex)
                                                )
                                            ).border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(
                                                    alpha = 0.5f
                                                ),
                                                shape = RoundedCornerShape(9.dp)
                                            )
                                    )

                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp).rotate(chevronRotation)
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = isThemePaletteExpanded,
                                enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                                exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                                ) {
                                    // ── Canlı Önizleme & Renk Hedefi Seçim Kartı ─────────────────
                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp)).background(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        primaryHex.copy(alpha = 0.12f),
                                                        secondaryHex.copy(alpha = 0.12f)
                                                    )
                                                )
                                            ).border(
                                                width = 1.dp, brush = Brush.horizontalGradient(
                                                    listOf(
                                                        primaryHex.copy(alpha = 0.35f),
                                                        secondaryHex.copy(alpha = 0.35f)
                                                    )
                                                ), shape = RoundedCornerShape(12.dp)
                                            ).padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.CenterVertically,

                                        ) {
                                            // Birincil Renk Seçici Buton
                                            Box(
                                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    if (isPrimaryTab) primaryHex
                                                    else primaryHex.copy(alpha = 0.15f)
                                                ).border(
                                                    width = if (isPrimaryTab) 2.dp else 1.dp,
                                                    color = if (isPrimaryTab) MaterialTheme.colorScheme.onSurface.copy(
                                                        alpha = 0.5f
                                                    ) else primaryHex.copy(alpha = 0.3f),
                                                    shape = RoundedCornerShape(10.dp)
                                                ).clickable {
                                                    viewModel.onEvent(
                                                        SettingsEvent.ThemeTabSelected(
                                                            ThemeColorTarget.PRIMARY
                                                        )
                                                    )
                                                }.padding(horizontal = 10.dp, vertical = 6.dp),
                                                contentAlignment = Alignment.Center) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    if (isPrimaryTab) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                    Text(
                                                        text = stringResource(Res.string.settings_theme_primary_color),
                                                        color = if (isPrimaryTab) Color.White else primaryHex,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isPrimaryTab) FontWeight.Bold else FontWeight.Medium
                                                    )
                                                }
                                            }

                                            // Vurgu Rengi Seçici Buton
                                            Box(
                                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    if (!isPrimaryTab) secondaryHex
                                                    else secondaryHex.copy(alpha = 0.15f)
                                                ).border(
                                                    width = if (!isPrimaryTab) 2.dp else 1.dp,
                                                    color = if (!isPrimaryTab) MaterialTheme.colorScheme.onSurface.copy(
                                                        alpha = 0.5f
                                                    ) else secondaryHex.copy(alpha = 0.3f),
                                                    shape = RoundedCornerShape(10.dp)
                                                ).clickable {
                                                    viewModel.onEvent(
                                                        SettingsEvent.ThemeTabSelected(
                                                            ThemeColorTarget.SECONDARY
                                                        )
                                                    )
                                                }.padding(horizontal = 10.dp, vertical = 6.dp),
                                                contentAlignment = Alignment.Center) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    if (!isPrimaryTab) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                    Text(
                                                        text = stringResource(Res.string.settings_theme_secondary_color),
                                                        color = if (!isPrimaryTab) Color.White else secondaryHex,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (!isPrimaryTab) FontWeight.Bold else FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Renk Paleti Izgarası
                                    val themeOptions = remember {
                                        listOf(
                                            ThemeColorOption(
                                                ThemeColor.DEFAULT,
                                                Res.string.theme_color_default,
                                                listOf(Color(0xFF6366F1), Color(0xFF10B981))
                                            ), ThemeColorOption(
                                                ThemeColor.BLUE,
                                                Res.string.theme_color_blue,
                                                listOf(Color(0xFF6C5CE7), Color(0xFF8E84E8))
                                            ), ThemeColorOption(
                                                ThemeColor.RED,
                                                Res.string.theme_color_red,
                                                listOf(Color(0xFFE63946), Color(0xFFFF6B6B))
                                            ), ThemeColorOption(
                                                ThemeColor.GREEN,
                                                Res.string.theme_color_green,
                                                listOf(Color(0xFF10B981), Color(0xFF34D399))
                                            ), ThemeColorOption(
                                                ThemeColor.PURPLE,
                                                Res.string.theme_color_purple,
                                                listOf(Color(0xFF9D4EDD), Color(0xFFC77DFF))
                                            ), ThemeColorOption(
                                                ThemeColor.PINK,
                                                Res.string.theme_color_pink,
                                                listOf(Color(0xFFE83E8C), Color(0xFFFF758F))
                                            ), ThemeColorOption(
                                                ThemeColor.ORANGE,
                                                Res.string.theme_color_orange,
                                                listOf(Color(0xFFF4A261), Color(0xFFE76F51))
                                            ), ThemeColorOption(
                                                ThemeColor.TEAL,
                                                Res.string.theme_color_teal,
                                                listOf(Color(0xFF00B4D8), Color(0xFF48CAE4))
                                            ), ThemeColorOption(
                                                ThemeColor.INDIGO,
                                                Res.string.theme_color_indigo,
                                                listOf(Color(0xFF3F37C9), Color(0xFF4895EF))
                                            ), ThemeColorOption(
                                                ThemeColor.AMBER,
                                                Res.string.theme_color_amber,
                                                listOf(Color(0xFFFFB703), Color(0xFFFB8500))
                                            ), ThemeColorOption(
                                                ThemeColor.BROWN,
                                                Res.string.theme_color_brown,
                                                listOf(Color(0xFF7F4F24), Color(0xFF936639))
                                            ),
                                            // Premium Themes 👑
                                            ThemeColorOption(
                                                ThemeColor.MIDNIGHT_BLACK,
                                                Res.string.theme_color_midnight,
                                                listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))
                                            ), ThemeColorOption(
                                                ThemeColor.CYBERPUNK_NEON,
                                                Res.string.theme_color_cyberpunk,
                                                listOf(Color(0xFF00FF88), Color(0xFFFF0080))
                                            ), ThemeColorOption(
                                                ThemeColor.ROSE_GOLD,
                                                Res.string.theme_color_rose_gold,
                                                listOf(Color(0xFFE8A0B0), Color(0xFFD4A070))
                                            ), ThemeColorOption(
                                                ThemeColor.NORDIC_MINIMALIST,
                                                Res.string.theme_color_nordic,
                                                listOf(Color(0xFF5B8A8A), Color(0xFF8A7B5B))
                                            )
                                        )
                                    }

                                    val chunkedOptions =
                                        remember(themeOptions) { themeOptions.chunked(4) }

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        chunkedOptions.forEach { rowItems ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                rowItems.forEach { option ->
                                                    val isSelected = if (isPrimaryTab) {
                                                        state.themeColor == option.color
                                                    } else {
                                                        state.secondaryThemeColor == option.color
                                                    }
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        modifier = Modifier.weight(1f)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .clickable {
                                                                viewModel.onEvent(
                                                                    SettingsEvent.ThemeColorSelected(
                                                                        option.color
                                                                    )
                                                                )
                                                            }.padding(
                                                                vertical = 4.dp, horizontal = 2.dp
                                                            )) {
                                                        val isPremiumTheme =
                                                            option.color.isPremiumTheme()

                                                        Box(
                                                            modifier = Modifier.size(50.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            // Main theme color circle
                                                            Box(
                                                                modifier = Modifier.size(42.dp)
                                                                    .clip(CircleShape).border(
                                                                        width = if (isSelected) 3.dp else if (isPremiumTheme) 2.dp else 1.dp,
                                                                        brush = when {
                                                                            isPremiumTheme && isSelected -> Brush.linearGradient(
                                                                                premiumGradient
                                                                            )

                                                                            isPremiumTheme -> SolidColor(
                                                                                premiumColor
                                                                            )

                                                                            isSelected -> SolidColor(
                                                                                if (isPrimaryTab) primaryHex else secondaryHex
                                                                            )

                                                                            else -> SolidColor(
                                                                                MaterialTheme.colorScheme.outlineVariant.copy(
                                                                                    alpha = 0.4f
                                                                                )
                                                                            )
                                                                        },
                                                                        shape = CircleShape
                                                                    )
                                                                    .padding(if (isSelected) 4.dp else 2.dp)
                                                                    .clip(CircleShape).background(
                                                                        Brush.linearGradient(
                                                                            option.previewGradient
                                                                        )
                                                                    ),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                if (isSelected) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Check,
                                                                        contentDescription = null,
                                                                        tint = Color.White,
                                                                        modifier = Modifier.size(20.dp)
                                                                    )
                                                                }
                                                            }

                                                            // Notification-style star badge on top-right for Premium themes
                                                            if (isPremiumTheme) {
                                                                Box(
                                                                    modifier = Modifier.align(
                                                                            Alignment.TopEnd
                                                                        ).offset(
                                                                            x = 1.dp, y = (-1).dp
                                                                        ).size(17.dp)
                                                                        .clip(CircleShape)
                                                                        .background(premiumColor)
                                                                        .border(
                                                                            width = 1.5.dp,
                                                                            color = MaterialTheme.colorScheme.surface,
                                                                            shape = CircleShape
                                                                        ),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Star,
                                                                        contentDescription = "Premium",
                                                                        tint = Color.White,
                                                                        modifier = Modifier.size(10.dp)
                                                                    )
                                                                }
                                                            }
                                                        }

                                                        Spacer(modifier = Modifier.height(4.dp))

                                                        Text(
                                                            text = stringResource(option.titleRes),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (isPremiumTheme) premiumColor else if (isSelected) (if (isPrimaryTab) primaryHex else secondaryHex) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                            fontWeight = if (isSelected || isPremiumTheme) FontWeight.Bold else FontWeight.Normal,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            textAlign = TextAlign.Center,
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                }

                                                val emptySlots = 4 - rowItems.size
                                                if (emptySlots > 0) {
                                                    repeat(emptySlots) {
                                                        Spacer(
                                                            modifier = Modifier.weight(1f)
                                                                .padding(horizontal = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── 4. Tercihler & Uygulama Bilgisi Grubu ──
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(Res.string.settings_section_app_info),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Çöp Kutusu (Recycle Bin)
                        Row(
                            modifier = Modifier.fillMaxWidth()
                            .clickable { viewModel.onEvent(SettingsEvent.TrashClicked) }
                            .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            SettingsIconBox(
                                icon = Icons.Default.DeleteSweep,
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                iconTint = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(Res.string.settings_trash_title),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(Res.string.settings_trash_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        // Verimlilik ve İstatistikler (Productivity & Insights)
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { viewModel.onEvent(SettingsEvent.AnalyticsClicked) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SettingsIconBox(
                                icon = Icons.Default.Insights,
                                containerColor = Color(0xFF00B4D8).copy(alpha = 0.12f),
                                iconTint = Color(0xFF00B4D8)
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(Res.string.profile_analytics_item),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(Res.string.profile_analytics_item_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        // Plan Karşılaştırması (Plan Comparison Matrix)
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { viewModel.onEvent(SettingsEvent.PlanComparisonClicked) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SettingsIconBox(
                                icon = Icons.Default.Star,
                                containerColor = proCrimson.copy(alpha = 0.12f),
                                iconTint = proCrimson
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(Res.string.settings_comparison_label),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(Res.string.settings_comparison_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        // Görev Dışa Aktarma (Premium)
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { viewModel.onEvent(SettingsEvent.ExportTasksClicked) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SettingsIconBox(
                                icon = Icons.Default.Download,
                                containerColor = Color(0xFF6366F1).copy(alpha = 0.12f),
                                iconTint = Color(0xFF6366F1)
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(Res.string.export_tasks_title),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(Res.string.export_tasks_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        // Bildirimler
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SettingsIconBox(
                                icon = Icons.Default.Notifications,
                                containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                iconTint = MaterialTheme.colorScheme.tertiary
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Text(
                                text = stringResource(Res.string.settings_notifications),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = stringResource(Res.string.premium_feature_unlocked),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        // Uygulama Sürümü
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SettingsIconBox(
                                icon = Icons.Default.Info,
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                iconTint = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Text(
                                text = stringResource(Res.string.settings_app_version),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = "v1.0.0 (Build 42)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── 5. Hesap ve Güvenlik Grubu (Danger Zone) ──
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(Res.string.settings_section_account),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Çıkış Yap
                        Row(
                            modifier = Modifier.fillMaxWidth()
                            .clickable { viewModel.onEvent(SettingsEvent.LogoutClicked) }
                            .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            SettingsIconBox(
                                icon = Icons.AutoMirrored.Filled.Logout,
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                iconTint = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Text(
                                text = stringResource(Res.string.action_logout),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        // Hesabı Sil
                        Row(
                            modifier = Modifier.fillMaxWidth()
                            .clickable(enabled = !state.isDeletingAccount) {
                                viewModel.onEvent(SettingsEvent.DeleteAccountClicked)
                            }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            SettingsIconBox(
                                icon = Icons.Default.Delete,
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                iconTint = MaterialTheme.colorScheme.error
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Text(
                                text = stringResource(Res.string.settings_delete_account),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )

                            if (state.isDeletingAccount) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.error,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(110.dp))
        }

        // ── Hesap Silme Onay İletişim Kutusu ──
        if (state.showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.onEvent(SettingsEvent.DeleteAccountDismissed) },
                title = {
                    Text(
                        text = stringResource(Res.string.settings_delete_account_confirm_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(text = stringResource(Res.string.settings_delete_account_confirm_desc))
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.onEvent(SettingsEvent.DeleteAccountConfirmed) }) {
                        Text(
                            text = stringResource(Res.string.action_delete),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.onEvent(SettingsEvent.DeleteAccountDismissed) }) {
                        Text(text = stringResource(Res.string.cancel))
                    }
                })
        }

        if (state.showExportBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.onEvent(SettingsEvent.SetExportSheetVisible(false)) },
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(Res.string.export_options_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 18.dp)
                    )

                    // CSV / Excel Option
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.onEvent(SettingsEvent.ExportTasksWithFormat(com.yusufteker.planora.core.export.ExportFormat.CSV))
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SettingsIconBox(
                                icon = Icons.Default.Download,
                                containerColor = Color(0xFF10B981).copy(alpha = 0.15f),
                                iconTint = Color(0xFF10B981)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(Res.string.export_format_csv_title),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(Res.string.export_format_csv_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Formatted Report / Document Option
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.onEvent(SettingsEvent.ExportTasksWithFormat(com.yusufteker.planora.core.export.ExportFormat.REPORT))
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SettingsIconBox(
                                icon = Icons.Default.Download,
                                containerColor = Color(0xFF6366F1).copy(alpha = 0.15f),
                                iconTint = Color(0xFF6366F1)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(Res.string.export_format_report_title),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(Res.string.export_format_report_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Ayarlar satırlarında kullanılan şık ikon kutusu.
 */
@Composable
private fun SettingsIconBox(
    icon: ImageVector, containerColor: Color, iconTint: Color
) {
    Box(
        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Tema rengi palet yapılandırması.
 */
private data class ThemeColorOption(
    val color: ThemeColor,
    val titleRes: org.jetbrains.compose.resources.StringResource,
    val previewGradient: List<Color>
)


