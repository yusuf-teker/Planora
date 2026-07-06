package com.yusufteker.pulse.feature.home.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun CalendarView(
    tasksByDate: Map<LocalDate, List<TaskDto>>,
    selectedDate: LocalDate?,
    visibleMonth: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChanged: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    val initialMonth = visibleMonth ?: LocalDate(today.year, today.monthNumber, 1)
    
    // Pager state for infinite scrolling (virtually)
    // We'll set a large page count and start in the middle
    val initialPage = 5000
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 10000 })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        val monthOffset = pagerState.currentPage - initialPage
        val currentMonthDate = getMonthDateWithOffset(initialMonth, monthOffset)
        onMonthChanged(currentMonthDate)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                }
            }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Önceki Ay")
            }
            
            val currentMonthOffset = pagerState.currentPage - initialPage
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
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
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

        // Calendar Grid Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val monthOffset = page - initialPage
            val monthDate = getMonthDateWithOffset(initialMonth, monthOffset)
            CalendarMonthGrid(
                monthDate = monthDate,
                today = today,
                selectedDate = selectedDate,
                tasksByDate = tasksByDate,
                onDateSelected = onDateSelected
            )
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
