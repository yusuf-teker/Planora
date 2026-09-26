package com.yusufteker.planora.feature.home.presentation.calendar_import.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.planora.core.calendar.CalendarImportItem
import com.yusufteker.planora.core.utils.formatShortDate
import com.yusufteker.planora.core.utils.formatTime
import com.yusufteker.planora.shared.api.TaskType
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.calendar_import_all_day
import planora.core.generated.resources.calendar_import_type_event
import planora.core.generated.resources.calendar_import_type_task

/**
 * Modern card displaying a single imported calendar item with quick selection,
 * type switching (Event <-> Task), and edit action.
 *
 * @param item The calendar event item to display.
 * @param onToggleSelect Callback when selection state is toggled.
 * @param onToggleType Callback when event/task type is switched.
 * @param onEditClick Callback when edit button is tapped.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarImportItemCard(
    item: CalendarImportItem,
    onToggleSelect: () -> Unit,
    onToggleType: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = item.isSelected
    val borderColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleSelect),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
            }
        ),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Custom Rounded Checkbox
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        )
                        .clickable(onClick = onToggleSelect),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title and Timing
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val timeText = if (item.isAllDay) {
                        "${formatShortDate(item.startTimeEpochMillis)} • ${stringResource(Res.string.calendar_import_all_day)}"
                    } else {
                        val start = "${formatShortDate(item.startTimeEpochMillis)}, ${formatTime(item.startTimeEpochMillis)}"
                        val end = item.endTimeEpochMillis?.let { " - ${formatTime(it)}" } ?: ""
                        start + end
                    }

                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )

                    val location = item.location
                    if (!location.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = location,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    val description = item.description
                    if (!description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Pills Row: Type Selector & Calendar Source Tag
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(start = 36.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Interactive Type Chip (Tapping directly toggles between Event and Task)
                val isEvent = item.targetType == TaskType.EVENT
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isEvent) {
                        Color(0xFF3B82F6).copy(alpha = 0.12f)
                    } else {
                        Color(0xFF10B981).copy(alpha = 0.12f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isEvent) Color(0xFF3B82F6).copy(alpha = 0.3f)
                        else Color(0xFF10B981).copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.clickable(onClick = onToggleType)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isEvent) Icons.Default.CalendarMonth else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isEvent) Color(0xFF3B82F6) else Color(0xFF10B981),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isEvent) stringResource(Res.string.calendar_import_type_event)
                            else stringResource(Res.string.calendar_import_type_task),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (isEvent) Color(0xFF3B82F6) else Color(0xFF10B981)
                            )
                        )
                    }
                }

                // Calendar Source Badge (e.g., Google Calendar, iCloud)
                val calName = item.calendarName
                if (!calName.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Text(
                            text = calName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
