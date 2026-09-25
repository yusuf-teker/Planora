package com.yusufteker.planora.feature.home.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.yusufteker.planora.core.ui.components.bounceClick
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yusufteker.planora.core.navigation.Screen
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

import com.yusufteker.planora.core.domain.usecase.getPlatformName
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.*

private val BottomBarRowHeight = 64.dp
private val AiButtonSize = 52.dp

/**
 * Bottom padding for the floating AI button to ensure its vertical center aligns
 * exactly on the top edge border of the bottom bar.
 * (RowHeight - half of ButtonSize = 64.dp - 26.dp = 38.dp)
 */
private val AiButtonBottomPadding = BottomBarRowHeight - (AiButtonSize / 2)

@Composable
fun PlanoraBottomBar(
    currentDestination: Screen.MainDestination?,
    isDark: Boolean,
    onNavigate: (Screen.MainDestination) -> Unit,
    onAiButtonClick: () -> Unit,
    pendingRequestsCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = if (isDark) Color(0xDC141419) else Color(0xF0FFFFFF) // Translucent glassmorphism

    val pillShape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp)
    val borderBrush = Brush.linearGradient(
        colors = if (isDark) {
            listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.03f))
        } else {
            listOf(Color.White.copy(alpha = 0.9f), primaryColor.copy(alpha = 0.2f))
        }
    )

    val navBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val isIos = remember { getPlatformName().equals("ios", ignoreCase = true) }
    // iOS: Lowered to eliminate excessive dead space below home indicator (34dp - 12dp = 22dp offset).
    // Android: Preserves the approved original positioning (navBarsBottom + 12dp).
    val effectiveBottomPadding = if (isIos) {
        (navBarsBottom - 12.dp).coerceAtLeast(0.dp)
    } else {
        navBarsBottom + 12.dp
    }

    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = effectiveBottomPadding)
    ) {
        Surface(
            color = surfaceColor,
            shape = pillShape,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderBrush),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 12.dp,
                    shape = pillShape,
                    spotColor = if (isDark) Color.Black else primaryColor.copy(alpha = 0.25f),
                    ambientColor = if (isDark) Color.Black else primaryColor.copy(alpha = 0.15f)
                )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BottomBarRowHeight),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home
                PlanoraBottomNavItem(
                    label = stringResource(Res.string.tab_home),
                    selectedIcon = Icons.Filled.Home,
                    unselectedIcon = Icons.Outlined.Home,
                    isSelected = currentDestination is Screen.MainDestination.Home,
                    onClick = { onNavigate(Screen.MainDestination.Home) },
                    modifier = Modifier.weight(1f)
                )

                // Plans
                PlanoraBottomNavItem(
                    label = stringResource(Res.string.title_plan_rooms),
                    selectedIcon = Icons.Filled.DateRange,
                    unselectedIcon = Icons.Outlined.DateRange,
                    isSelected = currentDestination is Screen.MainDestination.PlanRooms,
                    onClick = { onNavigate(Screen.MainDestination.PlanRooms) },
                    modifier = Modifier.weight(1f)
                )

                // Space for middle AI action button
                Spacer(modifier = Modifier.weight(1f))

                // Notes
                PlanoraBottomNavItem(
                    label = stringResource(Res.string.title_notes),
                    selectedIcon = Icons.Filled.Edit,
                    unselectedIcon = Icons.Outlined.Edit,
                    isSelected = currentDestination is Screen.MainDestination.Notes,
                    onClick = { onNavigate(Screen.MainDestination.Notes) },
                    modifier = Modifier.weight(1f)
                )

                // Profile
                PlanoraBottomNavItem(
                    label = stringResource(Res.string.tab_profile),
                    selectedIcon = Icons.Filled.Person,
                    unselectedIcon = Icons.Outlined.Person,
                    isSelected = currentDestination is Screen.MainDestination.Profile || currentDestination is Screen.MainDestination.Settings,
                    onClick = { onNavigate(Screen.MainDestination.Profile) },
                    modifier = Modifier.weight(1f),
                    badgeCount = pendingRequestsCount
                )
            }
        }


        val aiButtonBgColor = if (isDark) {
            Color(0xFF18171E)
        } else {
            MaterialTheme.colorScheme.surface
        }

        Box(
            modifier = Modifier
                .padding(bottom = AiButtonBottomPadding)
                .size(AiButtonSize)
                .shadow(
                    elevation = 6.dp,
                    shape = CircleShape,
                    spotColor = primaryColor.copy(alpha = 0.4f),
                    ambientColor = primaryColor.copy(alpha = 0.2f)
                )
                .background(
                    color = aiButtonBgColor,
                    shape = CircleShape
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    ),
                    shape = CircleShape
                )
                .bounceClick()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onAiButtonClick
                ),
            contentAlignment = Alignment.Center
        ) {
            PlanoraActionIcon(
                modifier = Modifier.size(36.dp),
                strokeWidth = 2.dp,
                drawOuterRing = false,
                gradientColors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary
                )
            )
        }
    }
}

@Composable
private fun PlanoraBottomNavItem(
    label: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0
) {
    val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .height(64.dp)
            .bounceClick()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box {
                Icon(
                    imageVector = if (isSelected) selectedIcon else unselectedIcon,
                    contentDescription = label,
                    tint = contentColor
                )
                if (badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                            .align(Alignment.TopEnd)
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor
            )
        }
    }
}

@Composable
fun PlanoraActionIcon(
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 2.dp,
    drawOuterRing: Boolean = false,
    gradientColors: List<Color> = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    )
) {
    val infiniteTransition = rememberInfiniteTransition()

    // 0f - 1f döngüsel animasyon süresi (2400 ms)
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Yıldızlar için yumuşak parıldama
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Gradient dönme açısı animasyonu
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val strokePx = strokeWidth.toPx()
        val radius = (canvasWidth / 2) - (strokePx / 2)
        val centerOffset = Offset(canvasWidth / 2, canvasHeight / 2)

        val rad = (rotationAngle * (PI / 180f)).toFloat()
        val startX = centerOffset.x - radius * cos(rad)
        val startY = centerOffset.y - radius * sin(rad)
        val endX = centerOffset.x + radius * cos(rad)
        val endY = centerOffset.y + radius * sin(rad)

        val brush = Brush.linearGradient(
            colors = gradientColors,
            start = Offset(startX, startY),
            end = Offset(endX, endY)
        )

        // 1. İstenirse Dış Çemberi Çiz
        if (drawOuterRing) {
            drawCircle(
                brush = brush,
                radius = radius,
                style = Stroke(width = strokePx)
            )

            drawCircle(
                color = Color.White.copy(alpha = glowAlpha * 0.4f),
                radius = radius,
                style = Stroke(width = strokePx)
            )
        }

        // 2. 3 Adet Animasyonlu AI Yıldızı (Sparkle)
        // Her yıldız sırayla belirecek: 1. -> 2. -> 3. ve ardından sönecekler.
        val stars = listOf(
            // Yıldız 1: Ana büyük yıldız (merkez)
            StarConfig(
                cx = canvasWidth * 0.50f,
                cy = canvasHeight * 0.54f,
                baseRadius = canvasWidth * 0.22f,
                appearStart = 0.00f,
                appearEnd = 0.20f
            ),
            // Yıldız 2: Orta boy yıldız (sağ üst)
            StarConfig(
                cx = canvasWidth * 0.68f,
                cy = canvasHeight * 0.30f,
                baseRadius = canvasWidth * 0.13f,
                appearStart = 0.20f,
                appearEnd = 0.40f
            ),
            // Yıldız 3: Küçük yıldız (sol üst)
            StarConfig(
                cx = canvasWidth * 0.32f,
                cy = canvasHeight * 0.34f,
                baseRadius = canvasWidth * 0.09f,
                appearStart = 0.40f,
                appearEnd = 0.60f
            )
        )

        for (star in stars) {
            val p = animProgress
            val alpha = when {
                p < star.appearStart -> 0f
                p in star.appearStart..star.appearEnd -> (p - star.appearStart) / (star.appearEnd - star.appearStart)
                p in star.appearEnd..star.fadeStart -> 1f
                p in star.fadeStart..star.fadeEnd -> 1f - (p - star.fadeStart) / (star.fadeEnd - star.fadeStart)
                else -> 0f
            }

            val scale = when {
                p < star.appearStart -> 0.3f
                p in star.appearStart..star.appearEnd -> 0.3f + 0.7f * ((p - star.appearStart) / (star.appearEnd - star.appearStart))
                p in star.appearEnd..star.fadeStart -> 1f
                p in star.fadeStart..star.fadeEnd -> 1f - 0.3f * ((p - star.fadeStart) / (star.fadeEnd - star.fadeStart))
                else -> 0.3f
            }

            if (alpha > 0f) {
                val currentRadius = star.baseRadius * scale
                val sparklePath = Path().apply {
                    val cx = star.cx
                    val cy = star.cy
                    val r = currentRadius
                    moveTo(cx, cy - r)
                    quadraticTo(cx, cy, cx + r, cy)
                    quadraticTo(cx, cy, cx, cy + r)
                    quadraticTo(cx, cy, cx - r, cy)
                    quadraticTo(cx, cy, cx, cy - r)
                    close()
                }

                // Yıldız Dolgusu (Fill)
                drawPath(
                    path = sparklePath,
                    brush = brush,
                    alpha = alpha
                )

                // Yıldız Vurgu Çizgisi (White glow highlight)
                drawPath(
                    path = sparklePath,
                    color = Color.White.copy(alpha = alpha * glowAlpha),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }
    }
}

private data class StarConfig(
    val cx: Float,
    val cy: Float,
    val baseRadius: Float,
    val appearStart: Float,
    val appearEnd: Float,
    val fadeStart: Float = 0.75f,
    val fadeEnd: Float = 0.95f
)


