package com.yusufteker.pulse.feature.home.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yusufteker.pulse.core.ui.components.AvatarImage
import com.yusufteker.pulse.feature.home.presentation.home.AccessibleUser

private fun String.toColorOrNull(): Color? {
    return try {
        Color(this.removePrefix("#").toLong(16) or 0x00000000FF000000)
    } catch (e: Exception) {
        null
    }
}

@Composable
fun SharedUserChipRow(
    accessibleUsers: List<AccessibleUser>,
    selectedUserIds: Set<Int>,
    onToggleUser: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (accessibleUsers.isEmpty()) return

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            // "Me" Chip
            UserChip(
                name = "Ben",
                avatarId = null,
                color = MaterialTheme.colorScheme.primary,
                isSelected = true,
                onClick = { /* Me is always selected for now */ }
            )
        }
        
        items(accessibleUsers, key = { it.userId }) { user ->
            val isSelected = selectedUserIds.contains(user.userId)
            val userColor = user.color.toColorOrNull() ?: MaterialTheme.colorScheme.secondary
            UserChip(
                name = user.name,
                avatarId = user.avatarId,
                color = userColor,
                isSelected = isSelected,
                onClick = { onToggleUser(user.userId) }
            )
        }
    }
}

@Composable
private fun UserChip(
    name: String,
    avatarId: String?,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) color.copy(alpha = 0.1f) else Color.Transparent
    val borderColor = if (isSelected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val textColor = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (avatarId != null) {
            AvatarImage(
                avatarId = avatarId,
                modifier = Modifier.size(20.dp).clip(CircleShape)
            )
        } else {
            // Placeholder for "Me"
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}
