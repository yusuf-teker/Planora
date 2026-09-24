package com.yusufteker.planora.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yusufteker.planora.core.theme.LocalIsDarkTheme
import com.yusufteker.planora.core.theme.PlanoraColors

/**
 * Modern Dribbble-style Glassmorphic Card.
 *
 * Provides a translucent glass visual effect, dynamic border glow, elevation, and optional click bounce.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color? = null,
    borderGradient: List<Color>? = null,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 6.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalIsDarkTheme.current

    val glassBg = backgroundColor ?: if (isDark) {
        PlanoraColors.SurfaceVariantDark.copy(alpha = 0.65f)
    } else {
        PlanoraColors.SurfaceLight.copy(alpha = 0.85f)
    }

    val defaultBorderGradient = if (isDark) {
        listOf(
            Color.White.copy(alpha = 0.15f),
            Color.White.copy(alpha = 0.03f)
        )
    } else {
        listOf(
            Color.White.copy(alpha = 0.8f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        )
    }

    val actualBorderBrush = Brush.linearGradient(borderGradient ?: defaultBorderGradient)

    val cardModifier = modifier
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = if (isDark) Color.Black else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            spotColor = if (isDark) Color.Black else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        )
        .clip(shape)
        .background(glassBg)
        .border(width = borderWidth, brush = actualBorderBrush, shape = shape)
        .then(
            if (onClick != null) {
                Modifier
                    .bounceClick()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
            } else {
                Modifier
            }
        )

    Box(
        modifier = cardModifier,
        content = content
    )
}
