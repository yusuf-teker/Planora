package com.yusufteker.planora.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Swipe-to-delete wrapper bileşeni.
 *
 * Sürükleme sırasında çöp kutusu ikonu kademeli olarak döner (-45 derece eşik noktasında).
 * Silme işlemi SADECE parmak kaldırıldığında ve eşik geçilmişse tetiklenir.
 * Eşik geçilmemişse kart geri yerine döner.
 *
 * @param modifier Modifier for layout adjustments.
 * @param shape Shape of the background and content container.
 * @param deleteThresholdFraction Silme eşiği (0.0-1.0 arası, varsayılan %40).
 * @param onDelete Parmak bırakıldığında ve eşik geçilmişse tetiklenen silme callback'i.
 * @param content Sarmalanan UI içeriği.
 */
@Composable
fun SwipeToDeleteWrapper(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    deleteThresholdFraction: Float = 0.4f,
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    var containerWidth by remember { mutableFloatStateOf(1f) }
    var isDeleted by remember { mutableStateOf(false) }

    val currentOffset = offsetX.value
    // Sürükleme ilerlemesi: 0f (başlangıç) -> 1f (tamamen sola kaydırılmış)
    val progress = if (containerWidth > 0f) (-currentOffset / containerWidth).coerceIn(0f, 1f) else 0f

    // Çöp ikonu rotasyonu: sürükledikçe kademeli döner, eşik noktasında tam -45 derece
    val rotation = if (progress <= deleteThresholdFraction) {
        (progress / deleteThresholdFraction) * -45f
    } else {
        -45f
    }

    // Kırmızı arka plan opaklığı: sürükledikçe artar
    val redAlpha = (progress * 2f).coerceIn(0f, 0.8f)

    if (!isDeleted) {
        Box(
            modifier = modifier
                .onSizeChanged { containerWidth = it.width.toFloat() }
        ) {
            // Arka plan katmanı: kırmızı + çöp ikonu
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(Color.Red.copy(alpha = redAlpha)),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (progress > 0.01f) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White.copy(alpha = (progress * 3f).coerceIn(0f, 1f)),
                        modifier = Modifier
                            .padding(16.dp)
                            .rotate(rotation)
                    )
                }
            }

            // Ön plan: asıl içerik
            Box(
                modifier = Modifier
                    .offset { IntOffset(currentOffset.roundToInt(), 0) }
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            coroutineScope.launch {
                                // Sadece sola sürüklemeye izin ver (offset negatif olur)
                                val newValue = (offsetX.value + delta).coerceAtMost(0f)
                                offsetX.snapTo(newValue)
                            }
                        },
                        onDragStopped = {
                            val pastThreshold = progress >= deleteThresholdFraction
                            if (pastThreshold) {
                                // Eşik geçildi: kartı tamamen sola kaydır ve sil
                                coroutineScope.launch {
                                    offsetX.animateTo(
                                        targetValue = -containerWidth,
                                        animationSpec = tween(200)
                                    )
                                    isDeleted = true
                                    onDelete()
                                }
                            } else {
                                // Eşik geçilmedi: geri yerine dön
                                coroutineScope.launch {
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = tween(200)
                                    )
                                }
                            }
                        }
                    )
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                content()
            }
        }
    }
}
