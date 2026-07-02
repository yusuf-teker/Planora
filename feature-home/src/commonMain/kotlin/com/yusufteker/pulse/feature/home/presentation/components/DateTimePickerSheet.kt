package com.yusufteker.pulse.feature.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import androidx.compose.foundation.layout.Arrangement


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerSheet(
    initialTimeMs: Long?,
    timeOnly: Boolean = false,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onDateTimeSelected: (Long) -> Unit
) {
    val initialMs = initialTimeMs ?: com.yusufteker.pulse.core.utils.getCurrentTimeMs()
    val tz = TimeZone.currentSystemDefault()
    val initialDateTime = Instant.fromEpochMilliseconds(initialMs).toLocalDateTime(tz)

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMs,
        initialDisplayMode = DisplayMode.Picker
    )
    
    val timePickerState = rememberTimePickerState(
        initialHour = initialDateTime.hour,
        initialMinute = initialDateTime.minute,
        is24Hour = true
    )

    var showTimePicker by remember { mutableStateOf(timeOnly) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth(0.15f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!showTimePicker) {
                Text(
                    text = "Tarih Seçin",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                DatePicker(
                    state = datePickerState,
                    modifier = Modifier.fillMaxWidth(),
                    showModeToggle = false,
                    title = null,
                    headline = null
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("İptal")
                    }
                    Button(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.padding(start = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("İleri")
                    }
                }
            } else {
                Text(
                    text = "Saat Seçin",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                // Using TimeInput for a cleaner, dial-free look which is more modern/universal
                TimeInput(state = timePickerState)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 32.dp),
                    horizontalArrangement = if (timeOnly) Arrangement.End else Arrangement.SpaceBetween
                ) {
                    if (!timeOnly) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("Geri")
                        }
                    }
                    Row {
                        TextButton(onClick = onDismissRequest) {
                            Text("İptal")
                        }
                        Button(
                            onClick = {
                                val selectedDateMs = datePickerState.selectedDateMillis ?: initialMs
                                // Combine date and time
                                val date = Instant.fromEpochMilliseconds(selectedDateMs).toLocalDateTime(TimeZone.UTC)
                                val resultDateTime = LocalDateTime(
                                    year = date.year,
                                    monthNumber = date.monthNumber,
                                    dayOfMonth = date.dayOfMonth,
                                    hour = timePickerState.hour,
                                    minute = timePickerState.minute,
                                    second = 0,
                                    nanosecond = 0
                                )
                                onDateTimeSelected(resultDateTime.toInstant(tz).toEpochMilliseconds())
                                onDismissRequest()
                            },
                            modifier = Modifier.padding(start = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Tamam")
                        }
                    }
                }
            }
        }
    }
}
