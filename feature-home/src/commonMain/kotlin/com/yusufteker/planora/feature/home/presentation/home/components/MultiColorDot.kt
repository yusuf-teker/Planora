package com.yusufteker.planora.feature.home.presentation.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MultiColorDot(
    colors: List<Color>,
    size: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val radius = size.toPx() / 2f
        val centerOffset = Offset(radius, radius)
        
        when {
            colors.isEmpty() -> {
                // Do nothing
            }
            colors.size == 1 -> {
                drawCircle(color = colors[0], radius = radius, center = centerOffset)
            }
            colors.size == 2 -> {
                // Sol yarı (0-180 derece)
                drawArc(
                    color = colors[0],
                    startAngle = 90f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset.Zero,
                    size = Size(size.toPx(), size.toPx())
                )
                // Sağ yarı (180-360 derece)
                drawArc(
                    color = colors[1],
                    startAngle = 270f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset.Zero,
                    size = Size(size.toPx(), size.toPx())
                )
            }
            colors.size == 3 -> {
                val sweep = 120f
                for (i in 0..2) {
                    drawArc(
                        color = colors[i],
                        startAngle = -90f + (i * sweep),
                        sweepAngle = sweep,
                        useCenter = true,
                        topLeft = Offset.Zero,
                        size = Size(size.toPx(), size.toPx())
                    )
                }
            }
            colors.size == 4 -> {
                val sweep = 90f
                for (i in 0..3) {
                    drawArc(
                        color = colors[i],
                        startAngle = -90f + (i * sweep),
                        sweepAngle = sweep,
                        useCenter = true,
                        topLeft = Offset.Zero,
                        size = Size(size.toPx(), size.toPx())
                    )
                }
            }
            else -> {
                // Gradient for 5+ users
                val brush = Brush.sweepGradient(
                    colors = colors,
                    center = centerOffset
                )
                drawCircle(brush = brush, radius = radius, center = centerOffset)
            }
        }
    }
}
