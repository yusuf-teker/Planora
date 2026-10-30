package com.yusufteker.planora.feature.home.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
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
import com.yusufteker.planora.core.theme.resolveThemeColor
import com.yusufteker.planora.core.ui.components.GradientText
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
    onEvent: (HomeEvent) -> Unit,
    onSearchClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        // Brand Mark "P"
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            GradientText(
                text = "P",
                colors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary
                ),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black
                )
            )
        }

        // Centered Timeline toggle
        Box(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            TimelineViewToggle(
                selected = state.viewOption,
                onSelected = {
                    onEvent(HomeEvent.ViewOptionChanged(it))
                }
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Search button
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier
                    .bounceClick()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(Res.string.search_title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Filter button
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
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = stringResource(Res.string.filter_title),
                    tint = if (isFilterActive) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}