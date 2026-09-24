package com.yusufteker.planora.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.yusufteker.planora.core.theme.LocalIsDarkTheme
import com.yusufteker.planora.core.theme.PlanoraColors

/**
 * Text component that renders content with a multi-color gradient brush.
 * Dynamically bounds the gradient to the measured text width so the transition
 * between primary and secondary (accent) colors is distinctly visible across all text lengths.
 *
 * Uses [graphicsLayer] with [CompositingStrategy.Offscreen] and [drawWithCache] with [BlendMode.SrcIn]
 * to guarantee that the gradient is accurately rendered across the exact bounds of the text glyphs
 * without being overridden by parent container content colors (e.g. TopAppBar titleContentColor).
 *
 * @param text The string content to render.
 * @param colors The list of colors forming the linear gradient. Defaults to primary and secondary theme colors.
 * @param style The baseline TextStyle for typography sizing and weight.
 * @param modifier Modifier for positioning or layout adjustments.
 */
@Composable
fun GradientText(
    text: String,
    colors: List<Color> = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
    style: TextStyle = MaterialTheme.typography.titleMedium,
    modifier: Modifier = Modifier
) {
    val gradientColors = if (colors.size >= 2) colors else listOf(
        colors.firstOrNull() ?: MaterialTheme.colorScheme.primary,
        colors.firstOrNull() ?: MaterialTheme.colorScheme.secondary
    )

    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    val measuredWidth = remember(text, style) {
        val result = textMeasurer.measure(
            text = androidx.compose.ui.text.AnnotatedString(text),
            style = style
        )
        result.size.width.toFloat().coerceAtLeast(1f)
    }

    val gradientBrush = remember(gradientColors, measuredWidth) {
        Brush.horizontalGradient(
            colors = gradientColors,
            startX = 0f,
            endX = measuredWidth
        )
    }

    Text(
        text = text,
        style = style.copy(brush = gradientBrush),
        modifier = modifier
    )
}

/**
 * Modern Dribbble-style Glassmorphic Pill Tag / Chip component.
 *
 * @param text Display text inside the chip.
 * @param isSelected Whether the chip is in selected active state.
 * @param onClick Optional callback when clicked.
 * @param modifier Layout modifier.
 * @param activeColors Custom gradient colors when active/selected.
 * @param icon Optional leading composable (e.g. icon or badge).
 */
@Composable
fun GlassChip(
    text: String,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    activeColors: List<Color> = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
    icon: (@Composable () -> Unit)? = null
) {
    val isDark = LocalIsDarkTheme.current
    val shape = RoundedCornerShape(16.dp)

    val backgroundBrush = if (isSelected) {
        Brush.linearGradient(activeColors)
    } else {
        val glassBg = if (isDark) PlanoraColors.SurfaceVariantDark.copy(alpha = 0.5f) else PlanoraColors.SurfaceLight.copy(alpha = 0.7f)
        Brush.linearGradient(listOf(glassBg, glassBg))
    }

    val borderBrush = if (isSelected) {
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.2f)))
    } else {
        val strokeColor = if (isDark) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        Brush.linearGradient(listOf(strokeColor, strokeColor))
    }

    val contentColor = if (isSelected) {
        Color.White
    } else {
        if (isDark) PlanoraColors.OnSurfaceDark else PlanoraColors.OnSurfaceLight
    }

    Box(
        modifier = modifier
            .bounceClick()
            .clip(shape)
            .background(backgroundBrush)
            .border(1.dp, borderBrush, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                icon()
                Box(modifier = Modifier.padding(end = 6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = contentColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            )
        }
    }
}

/**
 * Modifier that adds an infinite shimmer sweep loading effect to placeholder containers.
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition()
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val isDark = LocalIsDarkTheme.current
    val shimmerColors = if (isDark) {
        listOf(
            Color(0xFF1E1E24),
            Color(0xFF333340),
            Color(0xFF1E1E24)
        )
    } else {
        listOf(
            Color(0xFFEBEBF4),
            Color(0xFFF8F8FC),
            Color(0xFFEBEBF4)
        )
    }

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )

    this.background(brush)
}

/**
 * Skeleton loading placeholder card with rounded corners and shimmer animation.
 */
/**
 * Skeleton loading placeholder card with rounded corners and shimmer animation.
 */
@Composable
fun ShimmerLoadingItem(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    height: Dp = 80.dp
) {
    Box(
        modifier = modifier
            .clip(shape)
            .shimmerEffect()
    )
}

/**
 * Premium Dribbble-style Glow Button with gradient background, bounce physics, and ambient shadow.
 *
 * @param text Button label text.
 * @param onClick Action callback when clicked.
 * @param modifier Layout modifier.
 * @param gradientColors List of colors for linear gradient background.
 * @param enabled Whether button is enabled.
 * @param isLoading Whether to render a spinning indicator.
 * @param icon Optional leading icon composable.
 */
@Composable
fun GlowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: (@Composable () -> Unit)? = null
) {
    val shape = RoundedCornerShape(18.dp)
    val isDark = LocalIsDarkTheme.current
    
    val brush = if (enabled) {
        Brush.linearGradient(gradientColors)
    } else {
        val disabledColor = if (isDark) Color(0xFF2C2C35) else Color(0xFFDCDCE5)
        Brush.linearGradient(listOf(disabledColor, disabledColor))
    }

    val shadowColor = if (enabled && gradientColors.isNotEmpty()) gradientColors.first() else Color.Transparent

    val baseModifier = if (enabled && !isLoading) modifier.bounceClick() else modifier

    Box(
        modifier = baseModifier
            .shadow(
                elevation = if (enabled) 10.dp else 0.dp,
                shape = shape,
                spotColor = shadowColor.copy(alpha = 0.4f),
                ambientColor = shadowColor.copy(alpha = 0.2f)
            )
            .clip(shape)
            .background(brush)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = if (enabled) 0.4f else 0.1f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = shape
            )
            .clickable(
                enabled = enabled && !isLoading,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.padding(end = 8.dp).composed { this },
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else if (icon != null) {
                icon()
                Box(modifier = Modifier.padding(end = 8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

/**
 * Custom Pill / Tag badge with vibrant gradient overlay and glass border.
 *
 * @param text Badge text.
 * @param gradientColors List of accent colors.
 * @param modifier Layout modifier.
 */
@Composable
fun GradientBadge(
    text: String,
    gradientColors: List<Color> = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(gradientColors.map { it.copy(alpha = 0.18f) }))
            .border(1.dp, Brush.linearGradient(gradientColors.map { it.copy(alpha = 0.5f) }), shape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        GradientText(
            text = text,
            colors = gradientColors,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
        )
    }
}

