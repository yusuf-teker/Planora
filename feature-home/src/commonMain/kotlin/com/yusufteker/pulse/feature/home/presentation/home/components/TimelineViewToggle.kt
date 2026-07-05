package com.yusufteker.pulse.feature.home.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yusufteker.pulse.core.utils.TimelineViewOption

@Composable
fun TimelineViewToggle(
    selected: TimelineViewOption,
    onSelected: (TimelineViewOption) -> Unit
) {

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        TimelineToggleItem(
            text = "Tarih",
            selected = selected == TimelineViewOption.DATE
        ) {
            onSelected(TimelineViewOption.DATE)
        }

        TimelineToggleItem(
            text = "Periyot",
            selected = selected == TimelineViewOption.RELATIVE
        ) {
            onSelected(TimelineViewOption.RELATIVE)
        }

        TimelineToggleItem(
            text = "Takvim",
            selected = selected == TimelineViewOption.CALENDAR
        ) {
            onSelected(TimelineViewOption.CALENDAR)
        }
    }
}

@Composable
private fun TimelineToggleItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected)
                    MaterialTheme.colorScheme.primary
                else
                    Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {

        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color =
                if (selected)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}