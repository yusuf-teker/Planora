package com.yusufteker.pulse.feature.home.presentation.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yusufteker.pulse.core.utils.TimelineViewOption
import org.jetbrains.compose.resources.stringResource
import pulsy.core.generated.resources.Res
import pulsy.core.generated.resources.timeline_option_calendar
import pulsy.core.generated.resources.timeline_option_date
import pulsy.core.generated.resources.timeline_option_period

@Composable
fun TimelineViewToggle(
    selected: TimelineViewOption,
    onSelected: (TimelineViewOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = TimelineViewOption.entries
    val selectedIndex = when (selected) {
        TimelineViewOption.DATE -> 0
        TimelineViewOption.RELATIVE -> 1
        TimelineViewOption.CALENDAR -> 2
    }

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        val tabWidth = maxWidth / options.size

        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * selectedIndex,
            animationSpec = spring(
                dampingRatio = 0.8f,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "timelineToggleOffset"
        )

        // Smoothly moving indicator background
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .height(32.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary)
        )

        // Options Row
        Row(
            modifier = Modifier.width(maxWidth),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { option ->
                val isSelected = selected == option
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(durationMillis = 250),
                    label = "timelineToggleTextColor"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelected(option) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (option) {
                            TimelineViewOption.DATE -> stringResource(Res.string.timeline_option_date)
                            TimelineViewOption.RELATIVE -> stringResource(Res.string.timeline_option_period)
                            TimelineViewOption.CALENDAR -> stringResource(Res.string.timeline_option_calendar)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = textColor
                    )
                }
            }
        }
    }
}