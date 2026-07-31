package com.yusufteker.planora.feature.home.presentation.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.planora.core.theme.PlanoraTheme
import com.yusufteker.planora.core.utils.getCurrentTimeMs
import com.yusufteker.planora.shared.api.TaskDto
import com.yusufteker.planora.shared.api.extractBaseTaskId
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.action_next_month
import planora.core.generated.resources.action_prev_month
import planora.core.generated.resources.day_fri
import planora.core.generated.resources.day_mon
import planora.core.generated.resources.day_sat
import planora.core.generated.resources.day_sun
import planora.core.generated.resources.day_thu
import planora.core.generated.resources.day_tue
import planora.core.generated.resources.day_wed
import planora.core.generated.resources.empty_events_today
import planora.core.generated.resources.holiday_label
import planora.core.generated.resources.month_apr
import planora.core.generated.resources.month_aug
import planora.core.generated.resources.month_dec
import planora.core.generated.resources.month_feb
import planora.core.generated.resources.month_jan
import planora.core.generated.resources.month_jul
import planora.core.generated.resources.month_jun
import planora.core.generated.resources.month_mar
import planora.core.generated.resources.month_may
import planora.core.generated.resources.month_nov
import planora.core.generated.resources.month_oct
import planora.core.generated.resources.month_sep
import planora.core.generated.resources.today
@Composable
fun CalendarView(
    tasksByDate: Map<LocalDate, List<TaskDto>>,
    sharedTasksByDate: Map<Int, Map<LocalDate, List<TaskDto>>>,
    sharedUserColors: Map<Int, String>,
    accessibleUsers: List<com.yusufteker.planora.feature.home.presentation.home.AccessibleUser>,
    selectedSharedUserIds: Set<Int>,
    selectedDate: LocalDate?,
    visibleMonth: LocalDate?,
    holidays: Map<LocalDate, String> = emptyMap(),
    upcomingTasks: List<TaskDto>,
    hasLoadedTasks: Boolean,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChanged: (LocalDate) -> Unit,
    onTaskClick: (TaskDto) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember {
        Instant.fromEpochMilliseconds(getCurrentTimeMs())
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
    val initialMonth = remember { visibleMonth ?: LocalDate(today.year, today.monthNumber, 1) }

    // LazyColumn state for infinite scrolling (virtually)
    // We'll set a large item count and start in the middle
    // Performans: 10000 yerine 2400 item (100 yıl ileri/geri, yeterli)
    val initialPage = 1200
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
        if (selectedDate != null && !listState.isScrollInProgress) {
            val monthOffset =
                (selectedDate.year - initialMonth.year) * 12 + (selectedDate.monthNumber - initialMonth.monthNumber)
            val targetPage = initialPage + monthOffset
            if (listState.firstVisibleItemIndex != targetPage) {
                listState.animateScrollToItem(targetPage)
            }
        }
    }

    val monthOffsetForToday =
        (today.year - initialMonth.year) * 12 + (today.monthNumber - initialMonth.monthNumber)
    val todayPage = initialPage + monthOffsetForToday

    val showFab by remember(todayPage) {
        derivedStateOf {
            val visibleItemsInfo = listState.layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) return@derivedStateOf false

            val firstVisible = visibleItemsInfo.first().index
            val lastVisible = visibleItemsInfo.last().index

            todayPage < firstVisible || todayPage > lastVisible
        }
    }

    val isTodayAbove by remember(todayPage) {
        derivedStateOf {
            val visibleItemsInfo = listState.layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) return@derivedStateOf false

            val firstVisible = visibleItemsInfo.first().index
            todayPage < firstVisible
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Header (Optional, if we want to keep the chevron navigation to jump months)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                coroutineScope.launch {
                    listState.animateScrollToItem(listState.firstVisibleItemIndex - 1)
                }
            }) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = stringResource(Res.string.action_prev_month)
                )
            }

            val displayMonth by remember {
                derivedStateOf {
                    getMonthDateWithOffset(
                        initialMonth, listState.firstVisibleItemIndex - initialPage
                    )
                }
            }
            val monthName = stringResource(getMonthNameRes(displayMonth.monthNumber))

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
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = stringResource(Res.string.action_next_month)
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            val days = listOf(
                Res.string.day_mon,
                Res.string.day_tue,
                Res.string.day_wed,
                Res.string.day_thu,
                Res.string.day_fri,
                Res.string.day_sat,
                Res.string.day_sun
            )
            days.forEach { dayRes ->
                Text(
                    text = stringResource(dayRes),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Vertical List of Calendars
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(2400) { page ->
                    val monthOffset = page - initialPage
                    val monthDate = getMonthDateWithOffset(initialMonth, monthOffset)

                    Column {
                        // Render the Month Name if you want a separator inside the list (optional)
                        // We already have a sticky header-like row above, but a label inside helps for continuous scrolling
                        Text(
                            text = "${stringResource(getMonthNameRes(monthDate.monthNumber))} ${monthDate.year}",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.primary
                        )

                        CalendarMonthGrid(
                            monthDate = monthDate,
                            today = today,
                            selectedDate = selectedDate,
                            tasksByDate = tasksByDate,
                            sharedTasksByDate = sharedTasksByDate,
                            sharedUserColors = sharedUserColors,
                            selectedSharedUserIds = selectedSharedUserIds,
                            holidays = holidays,
                            onDateSelected = onDateSelected
                        )

                        // INLINE TASKS & HOLIDAY BADGE
                        if (selectedDate != null && selectedDate.monthNumber == monthDate.monthNumber && selectedDate.year == monthDate.year) {
                            val holidayName = holidays[selectedDate]
                            if (!holidayName.isNullOrBlank()) {
                                val isSelectedNewYear = selectedDate.monthNumber == 1 && selectedDate.dayOfMonth == 1
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondary.copy(
                                            alpha = 0.6f
                                        )
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 14.dp, vertical = 10.dp
                                        ), verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isSelectedNewYear) {
                                            Box(modifier = Modifier.size(44.dp)) {
                                                NewYearSnow(modifier = Modifier.fillMaxSize())
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                        } else {
                                            Text(
                                                text = "🎉 ",
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = stringResource(Res.string.holiday_label),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer.copy(
                                                    alpha = 0.8f
                                                )
                                            )
                                            Text(
                                                text = holidayName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            DayScheduleGrid(
                                selectedDate = selectedDate,
                                tasks = upcomingTasks,
                                accessibleUsers = accessibleUsers,
                                onTaskClick = onTaskClick,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                            Spacer(modifier = Modifier.height(16.dp))
                        } else {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            } // closes LazyColumn

            if (showFab) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(todayPage)
                            onDateSelected(today)
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomEnd)
                        .padding(end = 88.dp, bottom = 100.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        imageVector = if (isTodayAbove) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = stringResource(Res.string.today)
                    )
                }
            }
        } // closes Box
    } // closes Column
} // closes CalendarView

@Composable
private fun CalendarMonthGrid(
    monthDate: LocalDate,
    today: LocalDate,
    selectedDate: LocalDate?,
    tasksByDate: Map<LocalDate, List<TaskDto>>,
    sharedTasksByDate: Map<Int, Map<LocalDate, List<TaskDto>>>,
    sharedUserColors: Map<Int, String>,
    selectedSharedUserIds: Set<Int>,
    holidays: Map<LocalDate, String>,
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
                        val dayTasks = tasksByDate[date] ?: emptyList()
                        val existingBaseTaskIds = dayTasks.map { it.id.extractBaseTaskId() }.toSet()

                        val otherTasksColors = mutableListOf<Color>()
                        val seenOtherBaseTaskIds = mutableSetOf<String>()

                        selectedSharedUserIds.forEach { userId ->
                            val userTasks = sharedTasksByDate[userId]?.get(date)
                            if (!userTasks.isNullOrEmpty()) {
                                sharedUserColors[userId]?.let { colorStr ->
                                    try {
                                        val color = Color(
                                            colorStr.removePrefix("#")
                                                .toLong(16) or 0x00000000FF000000
                                        )
                                        for (task in userTasks) {
                                            val baseId = task.id.extractBaseTaskId()
                                            if (baseId !in existingBaseTaskIds && baseId !in seenOtherBaseTaskIds) {
                                                seenOtherBaseTaskIds.add(baseId)
                                                otherTasksColors.add(color)
                                            }
                                        }
                                    } catch (e: Exception) {
                                    }
                                }
                            }
                        }

                        CalendarDayCell(
                            date = date,
                            isToday = date == today,
                            isSelected = date == selectedDate,
                            holidayName = holidays[date],
                            tasks = dayTasks,
                            sharedColors = otherTasksColors,
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
    holidayName: String? = null,
    tasks: List<TaskDto>,
    sharedColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isHoliday = !holidayName.isNullOrBlank()
    val isNewYear = date.monthNumber == 1 && date.dayOfMonth == 1

    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }

    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onBackground
    }

    Box(
        modifier = modifier
            .aspectRatio(0.8f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
    ) {
        if (isNewYear) {
            Text(
                text = "🎄",
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 2.dp)
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Box(
                modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isToday || isHoliday || isNewYear) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        isNewYear && !isSelected -> MaterialTheme.colorScheme.primary
                        isHoliday -> MaterialTheme.colorScheme.primary
                        isToday -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onBackground
                    }
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Avatars / Markers for tasks
            val myTasks = tasks.filter { it.participants.size <= 1 && it.sharedRoomIds.isEmpty() }
            val sharedTasks =
                tasks.filter { it.participants.size > 1 || it.sharedRoomIds.isNotEmpty() }

            val hasMyTasks = myTasks.isNotEmpty()
            val hasSharedTasks = sharedTasks.isNotEmpty()
            val hasOtherTasks = sharedColors.isNotEmpty()

            if (hasMyTasks || hasSharedTasks || hasOtherTasks) {
                val activeGroupCount = (if (hasMyTasks) 1 else 0) +
                        (if (hasSharedTasks) 1 else 0) +
                        (if (hasOtherTasks) 1 else 0)

                val maxPerGroup = if (activeGroupCount > 1) 2 else 3

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Satır 1: Tüm noktaları yan yana ve gruplu şekilde diziyoruz (Tek yatay Row)
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Grup 1: Kişisel Görevler
                        if (hasMyTasks) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val count = minOf(myTasks.size, maxPerGroup)
                                repeat(count) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }

                        // Ayraç: Grup 1 ve sonraki grup arası mesafe
                        if (hasMyTasks && (hasSharedTasks || hasOtherTasks)) {
                            Spacer(modifier = Modifier.width(3.dp))
                        }

                        // Grup 2: Ortak Görevler
                        if (hasSharedTasks) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val count = minOf(sharedTasks.size, maxPerGroup)
                                val sharedDotColors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                                repeat(count) {
                                    MultiColorDot(
                                        colors = sharedDotColors,
                                        size = 6.dp
                                    )
                                }
                            }
                        }

                        // Ayraç: Grup 2 ve Grup 3 arası mesafe
                        if (hasSharedTasks && hasOtherTasks) {
                            Spacer(modifier = Modifier.width(3.dp))
                        }

                        // Grup 3: Diğer Kullanıcıların Görevleri
                        if (hasOtherTasks) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val count = minOf(sharedColors.size, maxPerGroup)
                                for (i in 0 until count) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(sharedColors[i])
                                    )
                                }
                            }
                        }
                    }

                    // Satır 2: Görev ismi (En fazla 1 satır, toplam hücre içeriği 2 satırı geçemez)
                    if (tasks.isNotEmpty()) {
                        Text(
                            text = tasks.first().title,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                                lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
                                    alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
                                    trim = androidx.compose.ui.text.style.LineHeightStyle.Trim.Both
                                )
                            ),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
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

private fun getMonthNameRes(monthNumber: Int): org.jetbrains.compose.resources.StringResource {
    return when (monthNumber) {
        1 -> Res.string.month_jan
        2 -> Res.string.month_feb
        3 -> Res.string.month_mar
        4 -> Res.string.month_apr
        5 -> Res.string.month_may
        6 -> Res.string.month_jun
        7 -> Res.string.month_jul
        8 -> Res.string.month_aug
        9 -> Res.string.month_sep
        10 -> Res.string.month_oct
        11 -> Res.string.month_nov
        12 -> Res.string.month_dec
        else -> Res.string.month_jan
    }
}


