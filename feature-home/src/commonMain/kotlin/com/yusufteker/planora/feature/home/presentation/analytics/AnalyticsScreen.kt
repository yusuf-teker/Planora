package com.yusufteker.planora.feature.home.presentation.analytics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufteker.planora.core.base.CollectEffect
import com.yusufteker.planora.core.theme.premiumColor
import com.yusufteker.planora.core.theme.premiumGradient
import com.yusufteker.planora.core.ui.components.GradientText
import com.yusufteker.planora.shared.api.TaskPriority
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.*

/**
 * Gelişmiş İstatistikler ve Verimlilik Grafikleri Ekranı.
 *
 * Kullanıcının planlama performansını, haftalık tamamlama grafiğini,
 * görev öncelik dengesini ve streak (kesintisiz plan serisi) metriklerini görselleştirir.
 *
 * @param viewModel İstatistik durumunu ve hesaplamalarını yöneten ViewModel.
 * @param onNavigateBack Geri butonuna basıldığında tetiklenen callback.
 * @param onNavigateToPremium Planora Premium paywall ekranına yönlendirme callback'i.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPremium: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    viewModel.effect.CollectEffect { effect ->
        when (effect) {
            AnalyticsEffect.NavigateBack -> onNavigateBack()
            AnalyticsEffect.NavigateToPremium -> onNavigateToPremium()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    GradientText(
                        text = stringResource(Res.string.analytics_title),
                        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onEvent(AnalyticsEvent.BackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back)
                        )
                    }
                },
                actions = {
                    if (state.isPremium) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = premiumColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, premiumColor.copy(alpha = 0.4f)),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "PRO",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = premiumColor
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.totalTasks == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Insights,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = stringResource(Res.string.analytics_empty_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(Res.string.analytics_empty_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // ── 1. KPI Metrik Kartları ──
                    KpiMetricsGrid(state = state)

                    // ── 2. Haftalık Tamamlama Çubuk Grafiği ──
                    WeeklyProductivityChartCard(weekly = state.weeklyProductivity)

                    // ── 3. Öncelik Dağılımı ──
                    PriorityBreakdownCard(priorityMap = state.priorityBreakdown, total = state.totalTasks)

                    // ── 4. Premium Derin Analitik veya Pro Kilit Kartı ──
                    if (state.isPremium) {
                        ProInsightsUnlockedCard(state = state)
                    } else {
                        ProInsightsLockedBanner(
                            onUpgradeClicked = { viewModel.onEvent(AnalyticsEvent.UpgradeClicked) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * 4 Anahtar Performans Göstergesini (KPI) 2x2 ızgara formatında listeler.
 */
@Composable
private fun KpiMetricsGrid(state: AnalyticsState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiCard(
                modifier = Modifier.weight(1f),
                title = stringResource(Res.string.analytics_summary_total),
                value = state.totalTasks.toString(),
                icon = Icons.Default.Insights,
                iconTint = MaterialTheme.colorScheme.primary
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                title = stringResource(Res.string.analytics_summary_completed),
                value = state.completedTasks.toString(),
                icon = Icons.Default.CheckCircle,
                iconTint = Color(0xFF10B981) // Task Green
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val percentage = (state.completionRate * 100).toInt()
            KpiCard(
                modifier = Modifier.weight(1f),
                title = stringResource(Res.string.analytics_summary_rate),
                value = "%$percentage",
                icon = Icons.Default.AutoAwesome,
                iconTint = Color(0xFF8B5CF6)
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                title = stringResource(Res.string.analytics_summary_streak),
                value = stringResource(Res.string.analytics_streak_days, state.currentStreakDays),
                icon = Icons.Default.Whatshot,
                iconTint = Color(0xFFFF5722) // Flame Orange
            )
        }
    }
}

@Composable
private fun KpiCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Son 7 günün günlük tamamlanan görev çubuk grafiği kartı.
 */
@Composable
private fun WeeklyProductivityChartCard(weekly: List<DayProductivity>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = stringResource(Res.string.analytics_weekly_chart_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(Res.string.analytics_weekly_chart_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            val maxCount = weekly.maxOfOrNull { it.totalCount }.takeIf { (it ?: 0) > 0 } ?: 1

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                weekly.forEach { day ->
                    val ratio = (day.completedCount.toFloat() / maxCount).coerceIn(0.08f, 1f)
                    val animatedHeight by animateFloatAsState(
                        targetValue = ratio,
                        animationSpec = tween(durationMillis = 600)
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = day.completedCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .fillMaxHeight(fraction = animatedHeight * 0.75f)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(
                                    if (day.isToday) {
                                        Brush.verticalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary
                                            )
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(0xFF10B981),
                                                Color(0xFF059669)
                                            )
                                        )
                                    }
                                )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = day.dayLabel,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
                            color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Görevlerin öncelik dağılımını gösteren kart.
 */
@Composable
private fun PriorityBreakdownCard(
    priorityMap: Map<TaskPriority, Int>,
    total: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(Res.string.analytics_priority_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            val highCount = priorityMap[TaskPriority.HIGH] ?: 0
            val mediumCount = priorityMap[TaskPriority.MEDIUM] ?: 0
            val lowCount = priorityMap[TaskPriority.LOW] ?: 0

            PriorityProgressRow(
                label = stringResource(Res.string.analytics_priority_high),
                count = highCount,
                total = total,
                color = Color(0xFFEF4444)
            )
            PriorityProgressRow(
                label = stringResource(Res.string.analytics_priority_medium),
                count = mediumCount,
                total = total,
                color = Color(0xFFF59E0B)
            )
            PriorityProgressRow(
                label = stringResource(Res.string.analytics_priority_low),
                count = lowCount,
                total = total,
                color = Color(0xFF3B82F6)
            )
        }
    }
}

@Composable
private fun PriorityProgressRow(
    label: String,
    count: Int,
    total: Int,
    color: Color
) {
    val progress = if (total > 0) count.toFloat() / total else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(500))

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$count (${(progress * 100).toInt()}%)",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}

/**
 * Premium kullanıcılar için açık olan PRO Derin Analitik Kartı.
 * 30 günlük tamamlama sparkline grafiği, zirve saat, en verimli/verimsiz gün
 * ve tarihsel en uzun streak bilgilerini gösterir.
 */
@Composable
private fun ProInsightsUnlockedCard(state: AnalyticsState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = premiumColor.copy(alpha = 0.06f),
        border = BorderStroke(1.2.dp, Brush.linearGradient(premiumGradient))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Header ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = stringResource(Res.string.analytics_pro_unlocked_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = premiumColor
                    )
                    Text(
                        text = stringResource(Res.string.analytics_pro_unlocked_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // ── 30-Day Sparkline Chart ──
            if (state.monthlyTrend.isNotEmpty()) {
                MonthlyTrendSparkline(monthly = state.monthlyTrend)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // ── Habit Pattern Grid ──
            Text(
                text = stringResource(Res.string.analytics_pro_habit_section),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProStatChip(
                        modifier = Modifier.weight(1f),
                        label = stringResource(Res.string.analytics_pro_peak_hour),
                        value = state.peakHourText,
                        color = Color(0xFF8B5CF6)
                    )
                    ProStatChip(
                        modifier = Modifier.weight(1f),
                        label = stringResource(Res.string.analytics_pro_avg_daily),
                        value = ((state.avgDailyCompletions * 10).toInt() / 10.0).toString(),
                        color = Color(0xFF10B981)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProStatChip(
                        modifier = Modifier.weight(1f),
                        label = stringResource(Res.string.analytics_pro_best_day),
                        value = state.bestDayLabel.take(3),
                        color = Color(0xFF3B82F6)
                    )
                    ProStatChip(
                        modifier = Modifier.weight(1f),
                        label = stringResource(Res.string.analytics_pro_longest_streak),
                        value = stringResource(Res.string.analytics_streak_days_count, state.longestStreakDays),
                        color = Color(0xFFFF5722)
                    )
                }
            }
        }
    }
}

/**
 * 30 günlük tamamlama verilerini kompakt bir sparkline (çizgi grafik) olarak görselleştirir.
 * Her çubuk bir günü temsil eder; bugün primary renkle vurgulanır.
 */
@Composable
private fun MonthlyTrendSparkline(monthly: List<DayProductivity>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.analytics_pro_monthly_trend),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(Res.string.analytics_pro_monthly_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }

        val maxCount = monthly.maxOfOrNull { it.completedCount }.takeIf { (it ?: 0) > 0 } ?: 1

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            monthly.forEach { day ->
                val ratio = (day.completedCount.toFloat() / maxCount).coerceIn(0.05f, 1f)
                val animatedRatio by animateFloatAsState(
                    targetValue = ratio,
                    animationSpec = tween(700)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 0.5.dp)
                        .fillMaxHeight(fraction = animatedRatio)
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(
                            if (day.isToday) MaterialTheme.colorScheme.primary
                            else premiumColor.copy(alpha = 0.5f)
                        )
                )
            }
        }

        // X-axis labels: show every 5th label for readability
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            monthly.forEachIndexed { index, day ->
                val showLabel = index == 0 || index == 14 || index == 29
                Text(
                    text = if (showLabel) day.dayLabel else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = if (index == 0) TextAlign.Start
                                else if (index == 29) TextAlign.End
                                else TextAlign.Center
                )
            }
        }
    }
}

/**
 * Küçük istatistik çipi: etiket ve değer içerir, arka planı belirtilen renkle ton verilmiş.
 */
@Composable
private fun ProStatChip(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

/**
 * Ücretsiz kullanıcılar için derin analitik kilit kartı.
 */
@Composable
private fun ProInsightsLockedBanner(onUpgradeClicked: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onUpgradeClicked),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = premiumColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(Res.string.analytics_pro_banner_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = stringResource(Res.string.analytics_pro_banner_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onUpgradeClicked,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = premiumColor,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.analytics_pro_banner_action),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
