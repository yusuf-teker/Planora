package com.yusufteker.pulse.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * A beautiful, performant particle burst effect using Jetpack Compose Canvas.
 */
@Composable
fun ParticleBurstEffect(
    modifier: Modifier = Modifier,
    particleColor: Color = Color(0xFF1D9BF0),
    particleCount: Int = 40,
    onAnimationEnd: () -> Unit = {}
) {
    val transition = rememberInfiniteTransition()
    
    // One-shot animation state
    var isPlaying by remember { mutableStateOf(true) }
    
    val progress by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        finishedListener = {
            onAnimationEnd()
        }
    )

    // Generate random particles once
    val particles = remember {
        List(particleCount) {
            val angle = Random.nextFloat() * 2 * Math.PI
            val distance = Random.nextFloat() * 300f + 50f
            val size = Random.nextFloat() * 12f + 4f
            Particle(
                angle = angle.toFloat(),
                maxDistance = distance,
                size = size
            )
        }
    }

    if (progress > 0f && progress < 1f) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            
            particles.forEach { particle ->
                // As progress goes 0 -> 1, distance increases, alpha decreases
                val currentDistance = particle.maxDistance * progress
                val currentAlpha = 1f - progress
                
                val x = center.x + currentDistance * cos(particle.angle)
                val y = center.y + currentDistance * sin(particle.angle)
                
                drawCircle(
                    color = particleColor.copy(alpha = currentAlpha),
                    radius = particle.size * (1f - progress * 0.5f), // Shrink slightly
                    center = Offset(x, y)
                )
            }
        }
    }
}

private data class Particle(
    val angle: Float,
    val maxDistance: Float,
    val size: Float
)
