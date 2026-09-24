package com.yusufteker.planora.feature.home.presentation.plan_room_detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.planora.shared.api.ItemDetails
import com.yusufteker.planora.shared.api.TaskDto
import com.yusufteker.planora.shared.api.UserProfileResponse
import kotlinx.datetime.*
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.*

/**
 * Plan odası takvim görünümü bileşeni.
 *
 * Ay navigasyonu, haftanın günleri ve gün hücrelerini görüntüler.
 * Gün hücreleri dashboard takvimi ile birebir aynı görsel dilde (32dp yuvarlak ve 5dp etkinlik noktaları) sunulur.
 */
@Composable
fun CalendarComponent(
    currentMonth: LocalDate,
    selectedDate: LocalDate?,
    tasks: List<TaskDto>,
    memberProfiles: Map<Int, UserProfileResponse> = emptyMap(),
    onDateSelected: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(Icons.Default.ChevronLeft, contentDescription = stringResource(Res.string.action_prev_month))
            }
            
            Text(
                text = "${getCalendarMonthName(currentMonth.monthNumber)} ${currentMonth.year}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = onNextMonth) {
                Icon(Icons.Default.ChevronRight, contentDescription = stringResource(Res.string.action_next_month))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Days of week
        val daysOfWeek = listOf(
            stringResource(Res.string.day_mon),
            stringResource(Res.string.day_tue),
            stringResource(Res.string.day_wed),
            stringResource(Res.string.day_thu),
            stringResource(Res.string.day_fri),
            stringResource(Res.string.day_sat),
            stringResource(Res.string.day_sun)
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            daysOfWeek.forEachIndexed { idx, day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (idx >= 5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Calendar Rows (Non-lazy layout for smooth outer scrolling)
        val days = remember(currentMonth) { getCalendarDays(currentMonth) }
        val dayRows = remember(days) { days.chunked(7) }
        val timeZone = remember { TimeZone.currentSystemDefault() }
        val nowMs = com.yusufteker.planora.core.utils.getCurrentTimeMs()
        val today = remember(nowMs) {
            Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(timeZone).date
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            dayRows.forEach { rowDays ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    rowDays.forEach { date ->
                        if (date != null) {
                            val isSelected = date == selectedDate
                            val isToday = date == today
                            
                            // Find tasks for this day
                            val dayTasks = tasks.filter { task ->
                                val effectiveTime = (task.specificDetails as? ItemDetails.Task)?.deadline ?: task.startTime
                                val taskDate = Instant.fromEpochMilliseconds(effectiveTime)
                                    .toLocalDateTime(timeZone).date
                                taskDate == date
                            }
                            
                            Box(modifier = Modifier.weight(1f)) {
                                CalendarCell(
                                    date = date,
                                    isSelected = isSelected,
                                    isToday = isToday,
                                    tasks = dayTasks,
                                    onClick = { onDateSelected(date) }
                                )
                            }
                        } else {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1.1f))
                        }
                    }
                    if (rowDays.size < 7) {
                        repeat(7 - rowDays.size) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1.1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    tasks: List<TaskDto>,
    onClick: () -> Unit
) {
    val bg = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }

    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        date.dayOfWeek.isoDayNumber >= 6 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${date.dayOfMonth}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp
                ),
                color = textColor
            )
        }

        if (tasks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val count = minOf(tasks.size, 3)
                repeat(count) { idx ->
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    if (idx < count - 1) {
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.height(7.dp))
        }
    }
}

// Utility to get grid cells (including empty padding for start of month)
fun getCalendarDays(month: LocalDate): List<LocalDate?> {
    val startOfMonth = LocalDate(month.year, month.month, 1)
    val nextMonth = startOfMonth.plus(1, DateTimeUnit.MONTH)
    val endOfMonth = nextMonth.minus(1, DateTimeUnit.DAY)
    
    val daysInMonth = endOfMonth.dayOfMonth
    val firstDayOfWeek = startOfMonth.dayOfWeek.isoDayNumber // 1 = Monday, 7 = Sunday
    
    val days = mutableListOf<LocalDate?>()
    
    // Add empty spaces for days before the 1st of the month
    for (i in 1 until firstDayOfWeek) {
        days.add(null)
    }
    
    // Add actual days
    for (i in 1..daysInMonth) {
        days.add(LocalDate(month.year, month.month, i))
    }
    
    // Fill the rest of the grid row with nulls
    val remaining = 7 - (days.size % 7)
    if (remaining < 7) {
        for (i in 1..remaining) {
            days.add(null)
        }
    }
    
    return days
}

@Composable
private fun getCalendarMonthName(month: Int): String {
    return when(month) {
        1 -> stringResource(Res.string.month_jan)
        2 -> stringResource(Res.string.month_feb)
        3 -> stringResource(Res.string.month_mar)
        4 -> stringResource(Res.string.month_apr)
        5 -> stringResource(Res.string.month_may)
        6 -> stringResource(Res.string.month_jun)
        7 -> stringResource(Res.string.month_jul)
        8 -> stringResource(Res.string.month_aug)
        9 -> stringResource(Res.string.month_sep)
        10 -> stringResource(Res.string.month_oct)
        11 -> stringResource(Res.string.month_nov)
        12 -> stringResource(Res.string.month_dec)
        else -> ""
    }
}
