package com.yusufteker.pulse.feature.home.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yusufteker.pulse.core.utils.TimelineViewOption
import com.yusufteker.pulse.feature.home.presentation.home.HomeEvent
import com.yusufteker.pulse.feature.home.presentation.home.HomeState

@Composable
fun HomeTopBar(
    state: HomeState,
    onEvent: (HomeEvent) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Title on the left
        Text(
            text = "Pulsy",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )

        // Centered Timeline toggle
        Box(
            modifier = Modifier.padding(horizontal = 8.dp).weight(1f),
            contentAlignment = Alignment.Center
        ) {
            TimelineViewToggle(
                selected = state.viewOption,
                onSelected = {
                    onEvent(HomeEvent.ViewOptionChanged(it))
                }
            )
        }

        // Filter button on the right
        IconButton(
            onClick = { onEvent(HomeEvent.ToggleFilterSheet(true)) },
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    if (
                        state.filterOptions.showOnlyNextRecurring ||
                        state.filterOptions.showCompleted
                    ) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Filtreler",
                tint = if (
                    state.filterOptions.showOnlyNextRecurring ||
                    !state.filterOptions.showCompleted
                ) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(20.dp)
            )
        }
    }
}