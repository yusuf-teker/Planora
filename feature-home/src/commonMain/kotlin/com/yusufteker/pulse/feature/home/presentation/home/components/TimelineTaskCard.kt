package com.yusufteker.pulse.feature.home.presentation.home.components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.yusufteker.pulse.core.utils.formatTime
import com.yusufteker.pulse.shared.api.TaskDto
import com.yusufteker.pulse.shared.api.TaskStatus
import com.yusufteker.pulse.shared.api.TaskType
import org.jetbrains.compose.resources.stringResource
import pulsy.core.generated.resources.Res
import pulsy.core.generated.resources.shared

@Composable
fun TimelineTaskCard(
    task: TaskDto,
    showDate: Boolean = false,
    sharedUserAvatar: String? = null,
    sharedUserColor: Color? = null,
    sharedUserProfileImageUrl: String? = null,
    onClick: () -> Unit = {}
) {
    val isCompleted = task.status == TaskStatus.COMPLETED

    val typeColor = when (task.type) {
        TaskType.EVENT -> Color(0xFF6366F1)  // Indigo
        TaskType.TASK -> Color(0xFF10B981)   // Emerald
        TaskType.NOTE -> Color(0xFFF59E0B)   // Amber
        TaskType.FOLDER -> Color(0xFF22C55E) // Lime
    }

    val animatedAlpha by animateFloatAsState(targetValue = if (isCompleted) 0.85f else 1f, label = "alpha")
    
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val backgroundColor = if (isDark) {
        Color.White.copy(alpha = if (isCompleted) 0.02f else 0.05f) // subtle glass, darker if completed
    } else {
        if (isCompleted) Color(0xFFF3F4F6) else Color.White // solid white, gray if completed
    }
    
    val borderColor = if (isDark) {
        Color.White.copy(alpha = if (isCompleted) 0.05f else 0.1f)
    } else {
        Color.Black.copy(alpha = if (isCompleted) 0.02f else 0.05f)
    }

    val startDateStr = com.yusufteker.pulse.core.utils.formatShortDate(task.startTime)
    val endDateStr = if (task.endTime != null) com.yusufteker.pulse.core.utils.formatShortDate(task.endTime!!) else null
    val isMultiDay = task.endTime != null && startDateStr != endDateStr

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(24.dp)) // Extra round for premium feel
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .graphicsLayer { alpha = animatedAlpha },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Time Badge (Pill)
        val primaryTime = (task.specificDetails as? com.yusufteker.pulse.shared.api.ItemDetails.Task)?.deadline ?: task.startTime
        val formattedTime = formatTime(primaryTime)
        
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(84.dp) // Tall pill
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(typeColor.copy(alpha = 0.25f), typeColor.copy(alpha = 0.05f))
                    )
                )
                .border(1.dp, typeColor.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val icon = when (task.type) {
                    TaskType.EVENT -> Icons.Default.Event
                    TaskType.TASK -> Icons.Default.CheckCircle
                    TaskType.NOTE -> Icons.Default.Edit
                    TaskType.FOLDER -> Icons.Default.Group
                }
                Icon(
                    imageVector = icon, 
                    contentDescription = null, 
                    tint = typeColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = formattedTime.replace(" AM", "").replace(" PM", ""),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = typeColor
                )
                
                if (formattedTime.contains("AM") || formattedTime.contains("PM")) {
                    Text(
                        text = if (formattedTime.contains("AM")) "AM" else "PM",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = typeColor.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (task.endTime != null && !isMultiDay && task.type == TaskType.EVENT) {
                    val endFormatted = formatTime(task.endTime!!).replace(" AM", "").replace(" PM", "")
                    Text(
                        text = endFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = typeColor.copy(alpha = 0.8f),
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Right: Content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Header: Type Chip & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Type Chip
                    Text(
                        text = task.type.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = typeColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(typeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    
                    if (isMultiDay && endDateStr != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$startDateStr - $endDateStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = typeColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(typeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    
                    if (isCompleted) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TAMAMLANDI",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF10B981), // Emerald
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (showDate) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = com.yusufteker.pulse.core.utils.formatShortDate(primaryTime),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (task.sharedRoomIds.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.tertiaryContainer,
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "ORTAK ODA",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (!task.isSynced) {
                    Icon(
                        imageVector = Icons.Default.SyncProblem,
                        contentDescription = "Not Synced",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Description or End Time
            if (!task.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.description ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val participantsList = task.participants
            val isShared = task.participants.size > 1 || task.sharedRoomIds.isNotEmpty()

            if (isShared || sharedUserAvatar != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // Right Side: Avatars/Participants
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy((-8).dp)
                    ) {
                        if (sharedUserAvatar != null || sharedUserProfileImageUrl != null) {
                            com.yusufteker.pulse.core.ui.components.AvatarImage(
                                avatarId = sharedUserAvatar,
                                profileImageUrl = sharedUserProfileImageUrl,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, sharedUserColor ?: Color.Transparent, CircleShape)
                                    .zIndex(1f)
                            )
                        } else if (participantsList.isNotEmpty()) {
                            participantsList.take(3).forEachIndexed { index, participant ->
                                com.yusufteker.pulse.core.ui.components.AvatarImage(
                                    avatarId = participant.avatarId,
                                    profileImageUrl = participant.profileImageUrl,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, MaterialTheme.colorScheme.background, CircleShape)
                                        .zIndex(3f - index)
                                )
                            }
                            if (participantsList.size > 3) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(1.dp, MaterialTheme.colorScheme.background, CircleShape)
                                        .zIndex(0f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+${participantsList.size - 3}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
