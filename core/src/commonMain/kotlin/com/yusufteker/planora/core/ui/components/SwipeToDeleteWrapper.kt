package com.yusufteker.planora.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.size
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.action_complete
import planora.core.generated.resources.action_copy
import kotlin.math.roundToInt

/**
 * Gelişmiş Swipe Wrapper Bileşeni.
 *
 * - Sola kaydırma (Left Swipe): Orijinal Swipe-To-Delete mantığı çalışır (Kırmızı arka plan + Dönen Çöp İkonu).
 * - Sağa kaydırma (Right Swipe): Soldan 2 aksiyon butonu çıkar ("Tamamla" ve "Kopyala").
 * - Ön plandaki cam kartın saydamlığından arka planın sızmaması için %100 opak yüzey katmanı eklenmiştir.
 *
 * @param modifier Dış alan düzenleyicisi
 * @param shape Kart ve arka plan köşe yuvarlatma biçimi
 * @param deleteThresholdFraction Silme eylemi için gereken kaydırma oranı
 * @param onDelete Silme aksiyonu tetiklendiğinde çağrılan fonksiyon
 * @param onToggleStatus Tamamlama/Durum değiştirme aksiyonu tetiklendiğinde çağrılan fonksiyon
 * @param onQuickDuplicate Hızlı kopyalama aksiyonu tetiklendiğinde çağrılan fonksiyon
 * @param content Kapsanan kart içeriği
 */
@Composable
fun SwipeToDeleteWrapper(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    deleteThresholdFraction: Float = 0.4f,
    onDelete: (() -> Unit)? = null,
    onToggleStatus: (() -> Unit)? = null,
    onQuickDuplicate: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val offsetX = remember { Animatable(0f) }
    var containerWidth by remember { mutableFloatStateOf(1f) }
    var isDeleted by remember { mutableStateOf(false) }

    val buttonCount = (if (onToggleStatus != null) 1 else 0) + (if (onQuickDuplicate != null) 1 else 0)
    val maxRightDp = if (buttonCount >= 2) 210.dp else 115.dp
    val maxRightPx = remember(density, buttonCount) { with(density) { maxRightDp.toPx() } }
    val rightThresholdPx = remember(density) { with(density) { 60.dp.toPx() } }

    val currentOffset = offsetX.value

    // Sola sürükleme ilerlemesi (Delete için): 0f -> 1f
    val progress = if (containerWidth > 0f && currentOffset < 0f) (-currentOffset / containerWidth).coerceIn(0f, 1f) else 0f

    val rotation = if (progress <= deleteThresholdFraction) {
        (progress / deleteThresholdFraction) * -45f
    } else {
        -45f
    }

    val redAlpha = (progress * 2f).coerceIn(0f, 0.8f)

    if (!isDeleted) {
        Box(
            modifier = modifier
                .onSizeChanged { containerWidth = it.width.toFloat() }
        ) {
            // ARKA PLAN KATMANLARI

            // 1. Sola Kaydırma (DELETE) Arka Planı
            if (currentOffset < 0f && onDelete != null) {
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
            }

            // 2. Sağa Kaydırma (TAMAMLA + KOPYALA) Arka Planı
            if (currentOffset > 0f && (onToggleStatus != null || onQuickDuplicate != null)) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (onToggleStatus != null) {
                            Surface(
                                onClick = {
                                    coroutineScope.launch { offsetX.animateTo(0f, tween(150)) }
                                    onToggleStatus()
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF10B981),
                                contentColor = Color.White,
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = stringResource(Res.string.action_complete),
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(Res.string.action_complete),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        if (onQuickDuplicate != null) {
                            Surface(
                                onClick = {
                                    coroutineScope.launch { offsetX.animateTo(0f, tween(150)) }
                                    onQuickDuplicate()
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF6366F1),
                                contentColor = Color.White,
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = stringResource(Res.string.action_copy),
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(Res.string.action_copy),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ÖN PLAN (ASIL İÇERİK)
            // Saydam kartların arka plan renklerini sızdırmaması için 100% opak Opak Surface eklenmiştir.
            Surface(
                modifier = Modifier
                    .offset { IntOffset(currentOffset.roundToInt(), 0) }
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            coroutineScope.launch {
                                val minAllowed = if (onDelete != null) -containerWidth else 0f
                                val maxAllowed = if (onToggleStatus != null || onQuickDuplicate != null) maxRightPx else 0f
                                val newValue = (offsetX.value + delta).coerceIn(minAllowed, maxAllowed)
                                offsetX.snapTo(newValue)
                            }
                        },
                        onDragStopped = {
                            if (offsetX.value < 0f && onDelete != null) {
                                val pastThreshold = progress >= deleteThresholdFraction
                                if (pastThreshold) {
                                    coroutineScope.launch {
                                        offsetX.animateTo(-containerWidth, tween(200))
                                        isDeleted = true
                                        onDelete()
                                    }
                                } else {
                                    coroutineScope.launch { offsetX.animateTo(0f, tween(200)) }
                                }
                            } else if (offsetX.value > 0f) {
                                if (offsetX.value >= rightThresholdPx) {
                                    coroutineScope.launch { offsetX.animateTo(maxRightPx, tween(200)) }
                                } else {
                                    coroutineScope.launch { offsetX.animateTo(0f, tween(200)) }
                                }
                            }
                        }
                    )
                    .then(
                        if (currentOffset > 10f) {
                            Modifier.clickable {
                                coroutineScope.launch { offsetX.animateTo(0f, tween(150)) }
                            }
                        } else Modifier
                    ),
                shape = shape,
                color = MaterialTheme.colorScheme.background // 100% opak arka plan sızıntısı önleyici
            ) {
                content()
            }
        }
    }
}
