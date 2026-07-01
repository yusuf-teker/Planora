package com.yusufteker.pulse.feature.home.presentation.plan_room_detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.yusufteker.pulse.shared.api.TaskDto
import com.yusufteker.pulse.shared.api.UserProfileResponse
import kotlinx.datetime.*
import org.jetbrains.compose.resources.stringResource
import pulse.core.generated.resources.Res
import pulse.core.generated.resources.*

@Composable
fun CalendarComponent(
    currentMonth: LocalDate,
    selectedDate: LocalDate?,
    tasks: List<TaskDto>,
    memberProfiles: Map<Int, UserProfileResponse>,
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
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
            }
            
            Text(
                text = "${currentMonth.month.name} ${currentMonth.year}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = onNextMonth) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
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
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Calendar Grid
        val days = remember(currentMonth) { getCalendarDays(currentMonth) }
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(days) { date ->
                if (date != null) {
                    val isSelected = date == selectedDate
                    val isToday = date == kotlinx.datetime.Instant.fromEpochMilliseconds(com.yusufteker.pulse.core.utils.getCurrentTimeMs()).toLocalDateTime(TimeZone.currentSystemDefault()).date
                    
                    // Find tasks for this day
                    val dayTasks = tasks.filter { task ->
                        val taskDate = Instant.fromEpochMilliseconds(task.startTime)
                            .toLocalDateTime(TimeZone.currentSystemDefault()).date
                        taskDate == date
                    }
                    
                    CalendarCell(
                        date = date,
                        isSelected = isSelected,
                        isToday = isToday,
                        tasks = dayTasks,
                        memberProfiles = memberProfiles,
                        onClick = { onDateSelected(date) }
                    )
                } else {
                    Box(modifier = Modifier.aspectRatio(1f)) // Empty cell
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
    memberProfiles: Map<Int, UserProfileResponse>,
    onClick: () -> Unit
) {
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        else -> Color.Transparent
    }
    
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        isToday -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            modifier = Modifier.padding(top = 4.dp)
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Overlapping Avatars for tasks
        if (tasks.isNotEmpty()) {
            val creators = tasks.map { it.creatorId }.distinct().take(3)
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
            ) {
                creators.forEachIndexed { index, creatorId ->
                    val profile = memberProfiles[creatorId]
                    val initial = profile?.name?.take(1)?.uppercase() ?: "?"
                    
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .offset(x = if (index > 0) (-4 * index).dp else 0.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(1.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                
                if (creators.size == 3 && tasks.map { it.creatorId }.distinct().size > 3) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .offset(x = (-4 * 3).dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(1.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
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
    
    // Fill the rest of the grid row with nulls (optional, LazyVerticalGrid handles it, but good for completeness)
    val remaining = 7 - (days.size % 7)
    if (remaining < 7) {
        for (i in 1..remaining) {
            days.add(null)
        }
    }
    
    return days
}
