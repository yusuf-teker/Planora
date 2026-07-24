package com.yusufteker.planora.feature.home.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderPickerSheet(
    selectedReminders: List<Int>,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onReminderToggled: (Int) -> Unit
) {
    val options = listOf(
        5 to Res.string.reminder_5_min,
        15 to Res.string.reminder_15_min,
        30 to Res.string.reminder_30_min,
        60 to Res.string.reminder_1_hour,
        1440 to Res.string.reminder_1_day,
        10080 to Res.string.reminder_1_week
    )

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, top = 8.dp)
        ) {
            Text(
                text = stringResource(Res.string.reminders_label),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

            options.forEach { (minutes, labelRes) ->
                val isSelected = selectedReminders.contains(minutes)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onReminderToggled(minutes) }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(labelRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
