package com.yusufteker.planora.feature.home.presentation.components

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
import com.yusufteker.planora.shared.api.RecurrenceRule
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.StringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.*

enum class RepeatType {
    NONE, DAILY, WEEKLY, MONTHLY, YEARLY
}

@Composable
fun RepeatType.toText(): String = when (this) {
    RepeatType.NONE -> stringResource(Res.string.repeat_none)
    RepeatType.DAILY -> stringResource(Res.string.repeat_daily)
    RepeatType.WEEKLY -> stringResource(Res.string.repeat_weekly)
    RepeatType.MONTHLY -> stringResource(Res.string.repeat_monthly)
    RepeatType.YEARLY -> stringResource(Res.string.repeat_yearly)
}

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
                null -> RepeatType.NONE
                is RecurrenceRule.Daily -> RepeatType.DAILY
                is RecurrenceRule.Weekly -> RepeatType.WEEKLY
                is RecurrenceRule.Monthly -> RepeatType.MONTHLY
                is RecurrenceRule.Yearly -> RepeatType.YEARLY
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
                text = stringResource(Res.string.title_repeat_rules),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp).align(Alignment.CenterHorizontally)
            )

            val types = RepeatType.entries

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
                            text = type.toText(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (selectedType == type) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            if (selectedType == RepeatType.WEEKLY) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(Res.string.title_repeat_days), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                FormSection {
                    val daysOfWeek = listOf(
                        1 to stringResource(Res.string.day_monday),
                        2 to stringResource(Res.string.day_tuesday),
                        3 to stringResource(Res.string.day_wednesday),
                        4 to stringResource(Res.string.day_thursday),
                        5 to stringResource(Res.string.day_friday),
                        6 to stringResource(Res.string.day_saturday),
                        7 to stringResource(Res.string.day_sunday)
                    )
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
            } else if (selectedType == RepeatType.MONTHLY) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(Res.string.title_repeat_monthly_type), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                FormSection {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { isLastDayOfMonth = false }.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(Res.string.repeat_monthly_same_day))
                        if (!isLastDayOfMonth) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { isLastDayOfMonth = true }.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(Res.string.repeat_monthly_last_day_of_month))
                        if (isLastDayOfMonth) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            } else if (selectedType == RepeatType.YEARLY) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(Res.string.repeat_yearly_desc), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val rule = when (selectedType) {
                        RepeatType.DAILY -> RecurrenceRule.Daily()
                        RepeatType.WEEKLY -> if (weeklyDays.isEmpty()) null else RecurrenceRule.Weekly(weeklyDays)
                        RepeatType.MONTHLY -> RecurrenceRule.Monthly(dayOfMonth = if (isLastDayOfMonth) null else startDate.dayOfMonth, isLastDay = isLastDayOfMonth)
                        RepeatType.YEARLY -> RecurrenceRule.Yearly(startDate.monthNumber, startDate.dayOfMonth)
                        else -> null
                    }
                    onRuleSelected(rule)
                    onDismissRequest()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(Res.string.action_ok))
            }
        }
    }
}
