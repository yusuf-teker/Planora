package com.yusufteker.planora.feature.home.presentation.home.components

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
import com.yusufteker.planora.core.utils.TimelineViewOption
import com.yusufteker.planora.feature.home.presentation.home.HomeEvent
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.*
import com.yusufteker.planora.feature.home.presentation.home.HomeState

import com.yusufteker.planora.core.ui.components.bounceClick

@Composable
fun HomeTopBar(
    state: HomeState,
    onEvent: (HomeEvent) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Title on the left with ultra gradient text
        com.yusufteker.planora.core.ui.components.GradientText(
            text = "Planora",
            colors = com.yusufteker.planora.core.theme.PlanoraColors.GradientPrimary,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Black
            )
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
        val isFilterActive = !state.filterOptions.showCompleted ||
                !state.filterOptions.showRoomTasks ||
                !state.filterOptions.showOnlyNextRecurring

        IconButton(
            onClick = { onEvent(HomeEvent.ToggleFilterSheet(true)) },
            modifier = Modifier
                .bounceClick()
                .clip(CircleShape)
                .background(
                    if (isFilterActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    }
                )
                .size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = stringResource(Res.string.filter_title),
                tint = if (isFilterActive) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(20.dp)
            )
        }
    }
}