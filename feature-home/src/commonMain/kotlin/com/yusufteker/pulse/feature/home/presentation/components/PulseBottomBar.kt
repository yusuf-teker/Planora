package com.yusufteker.pulse.feature.home.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yusufteker.pulse.core.navigation.Screen
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// KMP resource import'unuzu eklemeyi unutmayın
// import org.jetbrains.compose.resources.painterResource
// import com.yusufteker.pulse.core.navigation.Screen

@Composable
fun PulseBottomBar(
    currentDestination: Screen.MainDestination?,
    isDark: Boolean,
    onNavigate: (Screen.MainDestination) -> Unit,
    onAiButtonClick: () -> Unit,
    pendingRequestsCount: Int = 0
) {
    // Temanın ana rengini alarak Pulse efektine uyguluyoruz
    val pulseGlowColor = MaterialTheme.colorScheme.primary
    val surfaceColor = if (isDark) Color(0xCC000000) else Color(0xE6FFFFFF) // Glassmorphism opacity

    val infiniteTransition = rememberInfiniteTransition()
    val buttonScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            color = surfaceColor,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // TEMA UYUMLU CUSTOM BORDER
                // Kenarlarda görünmez, ortada AI butonunun altında parlayan Pulse efekti
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    pulseGlowColor.copy(alpha = 0.6f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .height(64.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Home
                    PulseBottomNavItem(
                        label = "Home",
                        selectedIcon = Icons.Filled.Home,
                        unselectedIcon = Icons.Outlined.Home,
                        isSelected = currentDestination is Screen.MainDestination.Home,
                        onClick = { onNavigate(Screen.MainDestination.Home) },
                        modifier = Modifier.weight(1f)
                    )

                    // Plans
                    PulseBottomNavItem(
                        label = "Plans",
                        selectedIcon = Icons.Filled.DateRange,
                        unselectedIcon = Icons.Outlined.DateRange,
                        isSelected = currentDestination is Screen.MainDestination.PlanRooms,
                        onClick = { onNavigate(Screen.MainDestination.PlanRooms) },
                        modifier = Modifier.weight(1f)
                    )

                    // Ortadaki AI butonu için boşluk
                    Spacer(modifier = Modifier.weight(1f))

                    // Notes
                    PulseBottomNavItem(
                        label = "Notes",
                        selectedIcon = Icons.Filled.Edit,
                        unselectedIcon = Icons.Outlined.Edit,
                        isSelected = currentDestination is Screen.MainDestination.Notes,
                        onClick = { onNavigate(Screen.MainDestination.Notes) },
                        modifier = Modifier.weight(1f)
                    )

                    // Profile
                    PulseBottomNavItem(
                        label = "Profile",
                        selectedIcon = Icons.Filled.Person,
                        unselectedIcon = Icons.Outlined.Person,
                        isSelected = currentDestination is Screen.MainDestination.Profile || currentDestination is Screen.MainDestination.Settings,
                        onClick = { onNavigate(Screen.MainDestination.Profile) },
                        modifier = Modifier.weight(1f),
                        badgeCount = pendingRequestsCount
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .padding(bottom = 40.dp) // 64.dp'lik çubuğun üzerine taşması için
                .windowInsetsPadding(WindowInsets.navigationBars)
                .graphicsLayer {
                    scaleX = buttonScale
                    scaleY = buttonScale
                }
                .size(48.dp)
                .shadow(
                    elevation = 8.dp, // Derinliği biraz artırarak daha iyi bir süzülme efekti verebiliriz
                    shape = CircleShape,
                    spotColor = pulseGlowColor,
                    ambientColor = pulseGlowColor
                )
                .background(
                    color = surfaceColor,
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onAiButtonClick
                ),
            contentAlignment = Alignment.Center
        ) {
            PulseActionIcon()
        }
    }
}

@Composable
private fun PulseBottomNavItem(
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
fun PulseActionIcon(
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 2.dp, // Kalınlık parametresi eklendi (İstediğiniz gibi inceltebilirsiniz)
    gradientColors: List<Color> = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    )
) {
    val infiniteTransition = rememberInfiniteTransition()

    // 0f'den 1f'e kadar path çizimi
    val pathProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Çember ve çizgi için yavaş yanıp sönen bir beyaz parlama
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Gradient'in de yavaşça dönmesi için açı animasyonu
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val pathMeasure = remember { PathMeasure() }
    val animatedPath = remember { Path() }

    Canvas(modifier = modifier.size(48.dp)) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Dp değerini Canvas'ın anlayacağı piksel (Px) değerine çeviriyoruz
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

        // 1. Dış Çemberi Çiz
        drawCircle(
            brush = brush,
            radius = radius,
            style = Stroke(width = strokePx) // Parametreden gelen kalınlık kullanılıyor
        )
        
        // 1.1 Çember üzerinde animasyonlu beyaz parıldama efekti
        drawCircle(
            color = Color.White.copy(alpha = glowAlpha),
            radius = radius,
            style = Stroke(width = strokePx)
        )

        // 2. İçteki Nabız (Pulse) Çizgisini Çiz
        val basePath = Path().apply {
            val midY = canvasHeight * 0.5f

            moveTo(canvasWidth * 0.25f, midY)
            lineTo(canvasWidth * 0.38f, midY)
            lineTo(canvasWidth * 0.45f, canvasHeight * 0.30f)
            lineTo(canvasWidth * 0.52f, canvasHeight * 0.70f)
            lineTo(canvasWidth * 0.58f, canvasHeight * 0.40f)
            lineTo(canvasWidth * 0.62f, midY)
            lineTo(canvasWidth * 0.75f, midY)
        }

        pathMeasure.setPath(basePath, false)
        animatedPath.reset()
        pathMeasure.getSegment(
            startDistance = 0f,
            stopDistance = pathMeasure.length * pathProgress,
            destination = animatedPath,
            startWithMoveTo = true
        )

        drawPath(
            path = animatedPath,
            brush = brush,
            style = Stroke(
                width = strokePx, // Parametreden gelen kalınlık kullanılıyor
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        
        // 2.1 İç çizgi üzerinde animasyonlu beyaz parıldama efekti
        drawPath(
            path = animatedPath,
            color = Color.White.copy(alpha = glowAlpha * 0.5f), // İç parıldama dışarıya göre biraz daha hafif
            style = Stroke(
                width = strokePx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

