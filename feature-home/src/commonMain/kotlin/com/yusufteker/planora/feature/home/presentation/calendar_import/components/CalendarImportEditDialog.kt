package com.yusufteker.planora.feature.home.presentation.calendar_import.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yusufteker.planora.core.calendar.CalendarImportItem
import com.yusufteker.planora.shared.api.TaskPriority
import com.yusufteker.planora.shared.api.TaskType
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.cancel
import planora.core.generated.resources.calendar_import_edit_save
import planora.core.generated.resources.calendar_import_edit_title
import planora.core.generated.resources.calendar_import_event_desc_label
import planora.core.generated.resources.calendar_import_event_loc_label
import planora.core.generated.resources.calendar_import_event_priority_label
import planora.core.generated.resources.calendar_import_event_title_label
import planora.core.generated.resources.calendar_import_event_type_label
import planora.core.generated.resources.calendar_import_type_event
import planora.core.generated.resources.calendar_import_type_task

/**
 * Edit dialog enabling users to fine-tune event details (title, description, location,
 * type: Event vs Task, and task priority) prior to final batch import.
 *
 * @param item The initial item being edited.
 * @param onSave Callback invoked with the modified [CalendarImportItem].
 * @param onDismiss Callback invoked when the user cancels or dismisses the dialog.
 */
@Composable
fun CalendarImportEditDialog(
    item: CalendarImportItem,
    onSave: (CalendarImportItem) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(item.title) }
    var description by remember { mutableStateOf(item.description ?: "") }
    var location by remember { mutableStateOf(item.location ?: "") }
    var targetType by remember { mutableStateOf(item.targetType) }
    var priority by remember { mutableStateOf(item.priority) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.calendar_import_edit_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Title Field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(Res.string.calendar_import_event_title_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Type Toggle (Event vs Task)
                Column {
                    Text(
                        text = stringResource(Res.string.calendar_import_event_type_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Event Option
                        val isEvent = targetType == TaskType.EVENT
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { targetType = TaskType.EVENT },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isEvent) Color(0xFF3B82F6).copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = BorderStroke(
                                1.dp,
                                if (isEvent) Color(0xFF3B82F6) else Color.Transparent
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = if (isEvent) Color(0xFF3B82F6) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(Res.string.calendar_import_type_event),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isEvent) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isEvent) Color(0xFF3B82F6) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        // Task Option
                        val isTask = targetType == TaskType.TASK
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { targetType = TaskType.TASK },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isTask) Color(0xFF10B981).copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = BorderStroke(
                                1.dp,
                                if (isTask) Color(0xFF10B981) else Color.Transparent
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isTask) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(Res.string.calendar_import_type_task),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isTask) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isTask) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }

                // If Task, Priority Selector
                if (targetType == TaskType.TASK) {
                    Column {
                        Text(
                            text = stringResource(Res.string.calendar_import_event_priority_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TaskPriority.entries.forEach { p ->
                                val selected = priority == p
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { priority = p },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                                ) {
                                    Text(
                                        text = p.name,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // Location Field
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text(stringResource(Res.string.calendar_import_event_loc_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Description Field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(Res.string.calendar_import_event_desc_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        item.copy(
                            title = title.trim().ifBlank { item.title },
                            description = description.trim().ifBlank { null },
                            location = location.trim().ifBlank { null },
                            targetType = targetType,
                            priority = priority
                        )
                    )
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(Res.string.calendar_import_edit_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
