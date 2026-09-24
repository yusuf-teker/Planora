package com.yusufteker.planora.feature.home.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yusufteker.planora.core.theme.PlanoraColors

/**
 * Görev/Etkinlik kartlarına çift yönlü kaydırma hareketleri (Swipe gestures) ekleyen kapsayıcı bileşen.
 * Sağa kaydırıldığında (StartToEnd): Durumu tamamlandı/bekliyor yapar (Yeşil onay).
 * Sola kaydırıldığında (EndToStart): Hızlı kopyalama / tarih seçici veya silme aksiyonunu tetikler.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTaskItem(
    onSwipeRightToComplete: (() -> Unit)? = null,
    onSwipeLeftToDuplicate: (() -> Unit)? = null,
    onSwipeLeftToDelete: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    if (onSwipeRightToComplete == null && onSwipeLeftToDuplicate == null && onSwipeLeftToDelete == null) {
        content()
        return
    }

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.3f },
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onSwipeRightToComplete?.invoke()
                    false // Kart silinmesin, yerine dönsün
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    if (onSwipeLeftToDuplicate != null) {
                        onSwipeLeftToDuplicate.invoke()
                    } else if (onSwipeLeftToDelete != null) {
                        onSwipeLeftToDelete.invoke()
                    }
                    false // Kart silinmesin, yerine dönsün
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = onSwipeRightToComplete != null,
        enableDismissFromEndToStart = onSwipeLeftToDuplicate != null || onSwipeLeftToDelete != null,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color by animateColorAsState(
                targetValue = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> PlanoraColors.TaskColor
                    SwipeToDismissBoxValue.EndToStart -> {
                        if (onSwipeLeftToDuplicate != null) PlanoraColors.EventColor
                        else Color(0xFFEF4444) // Red for Delete
                    }
                    else -> Color.Transparent
                },
                label = "swipeBgColor"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(color),
                contentAlignment = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    else -> Alignment.Center
                }
            ) {
                if (direction == SwipeToDismissBoxValue.StartToEnd && onSwipeRightToComplete != null) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Complete",
                        tint = Color.White,
                        modifier = Modifier.padding(start = 24.dp)
                    )
                } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                    if (onSwipeLeftToDuplicate != null) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Duplicate",
                            tint = Color.White,
                            modifier = Modifier.padding(end = 24.dp)
                        )
                    } else if (onSwipeLeftToDelete != null) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.White,
                            modifier = Modifier.padding(end = 24.dp)
                        )
                    }
                }
            }
        },
        content = {
            content()
        }
    )
}
