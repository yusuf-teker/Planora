package com.yusufteker.pulse.feature.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yusufteker.pulse.shared.api.RecurrenceRule
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepeatPickerSheet(
    initialRule: RecurrenceRule?,
    startDateMs: Long,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onRuleSelected: (RecurrenceRule?) -> Unit
) {
    var selectedType by remember {
        mutableStateOf(
            when (initialRule) {
                null -> "Yok"
                is RecurrenceRule.Daily -> "Her Gün"
                is RecurrenceRule.Weekly -> "Her Hafta"
                is RecurrenceRule.Monthly -> "Her Ay"
                is RecurrenceRule.Yearly -> "Her Yıl"
            }
        )
    }

    val tz = TimeZone.currentSystemDefault()
    val startInstant = Instant.fromEpochMilliseconds(startDateMs)
    val startDate = startInstant.toLocalDateTime(tz)

    // Sub-states
    var weeklyDays by remember {
        mutableStateOf(
            (initialRule as? RecurrenceRule.Weekly)?.daysOfWeek ?: emptySet()
        )
    }

    var isLastDayOfMonth by remember {
        mutableStateOf(
            (initialRule as? RecurrenceRule.Monthly)?.isLastDay ?: false
        )
    }

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
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = "Tekrar Kuralları",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp).align(Alignment.CenterHorizontally)
            )

            val types = listOf("Yok", "Her Gün", "Her Hafta", "Her Ay", "Her Yıl")

            FormSection {
                types.forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedType = type }
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = type,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (selectedType == type) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            if (selectedType == "Her Hafta") {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Hangi Günler?", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                FormSection {
                    val daysOfWeek = listOf(1 to "Pazartesi", 2 to "Salı", 3 to "Çarşamba", 4 to "Perşembe", 5 to "Cuma", 6 to "Cumartesi", 7 to "Pazar")
                    daysOfWeek.forEach { (dayValue, dayName) ->
                        val isSelected = weeklyDays.contains(dayValue)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    weeklyDays = if (isSelected) weeklyDays - dayValue else weeklyDays + dayValue
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(dayName)
                            if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            } else if (selectedType == "Her Ay") {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Aylık Tekrar Tipi", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                FormSection {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { isLastDayOfMonth = false }.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Ayın aynı günü")
                        if (!isLastDayOfMonth) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { isLastDayOfMonth = true }.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Ayın son günü")
                        if (isLastDayOfMonth) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            } else if (selectedType == "Her Yıl") {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Seçilen başlangıç tarihi ile aynı ay ve günde her yıl tekrarlar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val rule = when (selectedType) {
                        "Her Gün" -> RecurrenceRule.Daily()
                        "Her Hafta" -> if (weeklyDays.isEmpty()) null else RecurrenceRule.Weekly(weeklyDays)
                        "Her Ay" -> RecurrenceRule.Monthly(dayOfMonth = if (isLastDayOfMonth) null else startDate.dayOfMonth, isLastDay = isLastDayOfMonth)
                        "Her Yıl" -> RecurrenceRule.Yearly(startDate.monthNumber, startDate.dayOfMonth)
                        else -> null
                    }
                    onRuleSelected(rule)
                    onDismissRequest()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Tamam")
            }
        }
    }
}
