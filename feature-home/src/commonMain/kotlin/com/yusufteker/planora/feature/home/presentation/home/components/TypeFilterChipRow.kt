package com.yusufteker.planora.feature.home.presentation.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yusufteker.planora.core.ui.components.bounceClick
import com.yusufteker.planora.core.utils.TimelineTypeFilter
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.filter_type_all
import planora.core.generated.resources.filter_type_events
import planora.core.generated.resources.filter_type_tasks

/**
 * Ana akışta ve takvimde öğeleri "Tümü", "Etkinlikler" ve "Görevler" türlerine göre
 * tek dokunuşla (sıfır sürtünmeyle) filtrelemeyi sağlayan yatay çip satırı.
 *
 * @param selectedFilter Şu anda aktif olan filtre türü ([TimelineTypeFilter]).
 * @param onFilterSelected Filtre seçimi değiştiğinde çağrılan lambda.
 * @param modifier Dış düzenleyici.
 */
@Composable
fun TypeFilterChipRow(
    selectedFilter: TimelineTypeFilter,
    onFilterSelected: (TimelineTypeFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = listOf(
        Triple(TimelineTypeFilter.ALL, stringResource(Res.string.filter_type_all), Icons.Default.Apps),
        Triple(TimelineTypeFilter.EVENTS_ONLY, stringResource(Res.string.filter_type_events), Icons.Default.Event),
        Triple(TimelineTypeFilter.TASKS_ONLY, stringResource(Res.string.filter_type_tasks), Icons.Default.CheckCircle)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        filters.forEach { (filter, label, icon) ->
            val isSelected = selectedFilter == filter
            TypeFilterChipItem(
                label = label,
                icon = icon,
                isSelected = isSelected,
                onClick = { onFilterSelected(filter) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Tekil filtre çipi görsel bileşeni.
 */
@Composable
private fun TypeFilterChipItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    val bgColor by animateColorAsState(
        targetValue = if (isSelected) primaryColor else surfaceVariant.copy(alpha = 0.5f),
        animationSpec = tween(durationMillis = 200),
        label = "chipBgColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) onPrimary else onSurfaceVariant,
        animationSpec = tween(durationMillis = 200),
        label = "chipContentColor"
    )

    val borderColor = if (isSelected) primaryColor else Color.Transparent

    Row(
        modifier = modifier
            .height(34.dp)
            .bounceClick()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1
        )
    }
}
