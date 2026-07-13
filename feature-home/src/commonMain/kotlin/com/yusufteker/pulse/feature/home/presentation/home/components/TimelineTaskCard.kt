package com.yusufteker.pulse.feature.home.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.yusufteker.pulse.core.utils.formatTime
import com.yusufteker.pulse.core.utils.rotateVertically
import com.yusufteker.pulse.shared.api.TaskDto
import com.yusufteker.pulse.shared.api.TaskType
import com.yusufteker.pulse.shared.api.TaskStatus
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun TimelineTaskCard(
    task: TaskDto,
    showDate: Boolean = false,
    sharedUserAvatar: String? = null,
    sharedUserColor: Color? = null,
    onClick: () -> Unit = {}
) {
    val typeColor = when (task.type) {
        TaskType.EVENT -> Color(0xFF6366F1)  // Indigo
        TaskType.TASK -> Color(0xFF10B981)   // Emerald
        TaskType.NOTE -> Color(0xFFF59E0B)   // Amber
    }

    val isCompleted = task.status == TaskStatus.COMPLETED
    val animatedAlpha by animateFloatAsState(targetValue = if (isCompleted) 0.5f else 1f, label = "alpha")
    
    val backgroundColor = if (MaterialTheme.colorScheme.background.red < 0.5f) {
        // Dark mode
        Color(0x33FFFFFF) // subtle white glass on dark
    } else {
        // Light mode
        Color(0x80F0F0F8) // subtle gray glass on light
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 12.dp)
            .graphicsLayer { alpha = animatedAlpha },
        verticalAlignment = Alignment.Top
    ) {
        // Time column (left)
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.width(56.dp)
        ) {
            val primaryTime = (task.specificDetails as? com.yusufteker.pulse.shared.api.ItemDetails.Task)?.deadline ?: task.startTime
            Text(
                text = formatTime(primaryTime),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            // Show endTime only if it's not a Task (since tasks use deadline and their endTime is null or irrelevant here),
            // or if we have an endTime explicitly defined for non-tasks.
            task.endTime?.let {
                if (task.type != TaskType.TASK || it != task.startTime) {
                    Text(
                        text = formatTime(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            if (showDate) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = com.yusufteker.pulse.core.utils.formatShortDate(primaryTime),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (!task.isSynced) {
                Spacer(modifier = Modifier.height(4.dp))
                Icon(
                    imageVector = Icons.Default.SyncProblem,
                    contentDescription = "Not Synced",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Row (verticalAlignment = Alignment.CenterVertically){
            Text(
                modifier = Modifier.rotateVertically(),
                text = task.type.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = typeColor
            )
            // Vertical line indicator
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(typeColor.copy(alpha = 0.8f))
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Content column (right)
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                // Title
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (sharedUserAvatar != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    com.yusufteker.pulse.core.ui.components.AvatarImage(
                        avatarId = sharedUserAvatar,
                        modifier = Modifier.size(24.dp).clip(CircleShape).border(1.dp, sharedUserColor ?: Color.Transparent, CircleShape)
                    )
                }
            }

            // Description
            if (!task.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.description ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Footer (Participant count if any)
            val participantsList = task.participants.filterValues { it != "PENDING" }.values.toList()
            if (participantsList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(modifier = Modifier.height(24.dp).padding(top = 4.dp)) {
                    participantsList.take(4).forEachIndexed { index, name ->
                        val initialName = name.take(1).uppercase()
                        Box(
                            modifier = Modifier
                                .offset(x = (index * 16).dp)
                                .zIndex((4 - index).toFloat())
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .border(2.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initialName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (participantsList.size > 4) {
                        Box(
                            modifier = Modifier
                                .offset(x = (4 * 16).dp)
                                .zIndex(0f)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary)
                                .border(2.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+${participantsList.size - 4}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
