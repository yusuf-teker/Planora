package com.yusufteker.pulse.feature.home.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.pulse.shared.api.TaskDto
import kotlinx.coroutines.launch
import com.yusufteker.pulse.core.utils.getCurrentTimeMs
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun CalendarView(
    tasksByDate: Map<LocalDate, List<TaskDto>>,
    selectedDate: LocalDate?,
    visibleMonth: LocalDate?,
    upcomingTasks: List<TaskDto>,
    hasLoadedTasks: Boolean,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChanged: (LocalDate) -> Unit,
    onTaskClick: (TaskDto) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { Instant.fromEpochMilliseconds(getCurrentTimeMs()).toLocalDateTime(TimeZone.currentSystemDefault()).date }
    val initialMonth = remember { visibleMonth ?: LocalDate(today.year, today.monthNumber, 1) }
    
    // LazyColumn state for infinite scrolling (virtually)
    // We'll set a large item count and start in the middle
    val initialPage = 5000
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialPage)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { index ->
            val monthOffset = index - initialPage
            val currentMonthDate = getMonthDateWithOffset(initialMonth, monthOffset)
            onMonthChanged(currentMonthDate)
        }
    }

    LaunchedEffect(selectedDate) {
        if (selectedDate != null) {
            val monthOffset = (selectedDate.year - initialMonth.year) * 12 + (selectedDate.monthNumber - initialMonth.monthNumber)
            val targetPage = initialPage + monthOffset
            if (listState.firstVisibleItemIndex != targetPage) {
                listState.animateScrollToItem(targetPage)
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Header (Optional, if we want to keep the chevron navigation to jump months)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                coroutineScope.launch {
                    listState.animateScrollToItem(listState.firstVisibleItemIndex - 1)
                }
            }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Önceki Ay")
            }
            
            val currentMonthOffset = listState.firstVisibleItemIndex - initialPage
            val displayMonth = getMonthDateWithOffset(initialMonth, currentMonthOffset)
            val monthName = getMonthNameTurkish(displayMonth.monthNumber)
            
            Text(
                text = "$monthName ${displayMonth.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            IconButton(onClick = {
                coroutineScope.launch {
                    listState.animateScrollToItem(listState.firstVisibleItemIndex + 1)
                }
            }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Sonraki Ay")
            }
        }

        // Days of week header
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            val days = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
            days.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Vertical List of Calendars
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(10000) { page ->
                val monthOffset = page - initialPage
                val monthDate = getMonthDateWithOffset(initialMonth, monthOffset)
                
                Column {
                    // Render the Month Name if you want a separator inside the list (optional)
                    // We already have a sticky header-like row above, but a label inside helps for continuous scrolling
                    Text(
                        text = "${getMonthNameTurkish(monthDate.monthNumber)} ${monthDate.year}",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    CalendarMonthGrid(
                        monthDate = monthDate,
                        today = today,
                        selectedDate = selectedDate,
                        tasksByDate = tasksByDate,
                        onDateSelected = onDateSelected
                    )

                    // INLINE TASKS
                    if (selectedDate != null && selectedDate.monthNumber == monthDate.monthNumber && selectedDate.year == monthDate.year) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (hasLoadedTasks && upcomingTasks.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Bugün etkinlik yok.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                upcomingTasks.forEach { task ->
                                    TimelineTaskCard(
                                        task = task,
                                        showDate = false,
                                        onClick = { onTaskClick(task) }
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarMonthGrid(
    monthDate: LocalDate,
    today: LocalDate,
    selectedDate: LocalDate?,
    tasksByDate: Map<LocalDate, List<TaskDto>>,
    onDateSelected: (LocalDate) -> Unit
) {
    val daysInMonth = getDaysInMonth(monthDate.year, monthDate.monthNumber)
    val startDayOfWeek = monthDate.dayOfWeek.ordinal + 1 // 1=Mon, 7=Sun
    val totalCells = daysInMonth + startDayOfWeek - 1
    val rows = (totalCells + 6) / 7

    Column {
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    val dayNum = cellIndex - startDayOfWeek + 2
                    
                    if (dayNum in 1..daysInMonth) {
                        val date = LocalDate(monthDate.year, monthDate.monthNumber, dayNum)
                        CalendarDayCell(
                            date = date,
                            isToday = date == today,
                            isSelected = date == selectedDate,
                            tasks = tasksByDate[date] ?: emptyList(),
                            onClick = { onDateSelected(date) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // Empty cell
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(0.8f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    tasks: List<TaskDto>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }
    
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onBackground
    }
    
    Column(
        modifier = modifier
            .aspectRatio(0.8f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
        
        Spacer(modifier = Modifier.height(2.dp))

        // Avatars / Markers for tasks

        if (tasks.isNotEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Noktalar ve + işareti yan yana
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically // Elemanları dikeyde mükemmel ortalar
                ) {
                    val maxAvatarsToShow = 2
                    val avatarsCount = minOf(tasks.size, maxAvatarsToShow)

                    for (i in 0 until avatarsCount) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 1.dp) // Dikey padding yok, yüksekliği daralttık
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }

                    if (tasks.size > maxAvatarsToShow) {
                        Text(
                            text = "+",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 1.dp),
                            style = TextStyle(
                                fontSize = 10.sp,
                                lineHeight = 10.sp, // Satır yüksekliğini font boyutuyla aynı tutuyoruz
                                lineHeightStyle = LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Center,
                                    trim = LineHeightStyle.Trim.Both // KMP'de metnin alt/üst boşluklarını keser
                                )
                            )
                        )
                    }
                }

                for (task in tasks.take(1)) {
                    Text(
                        text = task.title,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall.copy(
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both // Görev ismindeki boşluğu da keser
                            )
                        ),
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

private fun getDaysInMonth(year: Int, monthNumber: Int): Int {
    return when (monthNumber) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) 29 else 28
        else -> 31
    }
}

private fun getMonthDateWithOffset(start: LocalDate, offset: Int): LocalDate {
    var m = start.monthNumber - 1 + offset
    var y = start.year
    
    if (m >= 0) {
        y += m / 12
        m = m % 12
    } else {
        y += (m - 11) / 12
        m = (m % 12 + 12) % 12
    }
    
    return LocalDate(y, m + 1, 1)
}

private fun getMonthNameTurkish(monthNumber: Int): String {
    return when (monthNumber) {
        1 -> "Ocak"
        2 -> "Şubat"
        3 -> "Mart"
        4 -> "Nisan"
        5 -> "Mayıs"
        6 -> "Haziran"
        7 -> "Temmuz"
        8 -> "Ağustos"
        9 -> "Eylül"
        10 -> "Ekim"
        11 -> "Kasım"
        12 -> "Aralık"
        else -> ""
    }
}
