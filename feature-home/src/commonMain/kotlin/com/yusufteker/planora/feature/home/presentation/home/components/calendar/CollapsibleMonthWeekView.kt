package com.yusufteker.planora.feature.home.presentation.home.components.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.planora.feature.home.presentation.home.components.MultiColorDot
import com.yusufteker.planora.shared.api.TaskDto
import com.yusufteker.planora.shared.api.extractBaseTaskId
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.action_next_month
import planora.core.generated.resources.action_prev_month
import planora.core.generated.resources.calendar_mode_year
import planora.core.generated.resources.day_fri
import planora.core.generated.resources.day_mon
import planora.core.generated.resources.day_sat
import planora.core.generated.resources.day_sun
import planora.core.generated.resources.day_thu
import planora.core.generated.resources.day_tue
import planora.core.generated.resources.day_wed

private fun String.toColorOrNull(): Color? {
    return try {
        Color(this.removePrefix("#").toLong(16) or 0x00000000FF000000)
    } catch (e: Exception) {
        null
    }
}

/**
 * Planora Takvim Ay ve Daraltılabilir Hafta Görünümü (Collapsible Month & Week View).
 *
 * Tam ay görünümü ile tek haftalık satıra daralma animasyonunu sağlar.
 * Ekran yukarı sürüklendiğinde veya bir gün seçildiğinde üst ay ızgarası daralarak
 * sadece seçili günün haftasını bırakır. Özel günleri (holidays) gün ızgarasında belirginleştirir.
 *
 * @param visibleMonth Gösterilen ay tarihi
 * @param selectedDate Seçili tarih
 * @param today Bugünün tarihi
 * @param tasksByDate Tarihlere göre görev haritası
 * @param sharedTasksByDate Ortak kullanıcıların tarihlere göre görev haritası
 * @param sharedUserColors Ortak kullanıcıların renk haritası (UserId -> HexColor)
 * @param selectedSharedUserIds Seçili ortak kullanıcı ID kümesi
 * @param isMyTasksSelected Kendi görevlerim seçili mi?
 * @param holidays Özel günler haritası (Ör. Resmi tatiller, yılbaşı vb.)
 * @param isCollapsed Hafta moduna daralmış durumda mı?
 * @param onToggleCollapse Daraltma/Genişletme tetikleyicisi
 * @param onDateSelected Tarih seçim olayı
 * @param onMonthChanged Ay değişim olayı
 * @param onYearHeaderClick Yıl başlığı tıklama olayı
 * @param modifier Dış düzenleyici
 */
@Composable
fun CollapsibleMonthWeekView(
    visibleMonth: LocalDate,
    selectedDate: LocalDate,
    today: LocalDate,
    tasksByDate: Map<LocalDate, List<TaskDto>>,
    sharedTasksByDate: Map<Int, Map<LocalDate, List<TaskDto>>> = emptyMap(),
    sharedUserColors: Map<Int, String> = emptyMap(),
    selectedSharedUserIds: Set<Int> = emptySet(),
    isMyTasksSelected: Boolean = true,
    holidays: Map<LocalDate, String> = emptyMap(),
    isCollapsed: Boolean,
    onToggleCollapse: (Boolean) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChanged: (LocalDate) -> Unit,
    onYearHeaderClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val daysInMonth = remember(visibleMonth) { getDaysInMonth(visibleMonth.year, visibleMonth.monthNumber) }
    val firstDayOfWeek = remember(visibleMonth) { LocalDate(visibleMonth.year, visibleMonth.monthNumber, 1).dayOfWeek.ordinal }

    val selectedDayNumber = selectedDate.dayOfMonth
    val selectedRowIndex = remember(visibleMonth, selectedDate) {
        if (selectedDate.monthNumber == visibleMonth.monthNumber && selectedDate.year == visibleMonth.year) {
            (firstDayOfWeek + selectedDayNumber - 1) / 7
        } else 0
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onYearHeaderClick() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${visibleMonth.monthNumber} / ${visibleMonth.year}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = stringResource(Res.string.calendar_mode_year),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Row {
                IconButton(onClick = {
                    val prevMonth = visibleMonth.plus(-1, DateTimeUnit.MONTH)
                    onMonthChanged(prevMonth)
                }) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = stringResource(Res.string.action_prev_month)
                    )
                }
                IconButton(onClick = {
                    val nextMonth = visibleMonth.plus(1, DateTimeUnit.MONTH)
                    onMonthChanged(nextMonth)
                }) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = stringResource(Res.string.action_next_month)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            val weekDayResList = listOf(
                Res.string.day_mon,
                Res.string.day_tue,
                Res.string.day_wed,
                Res.string.day_thu,
                Res.string.day_fri,
                Res.string.day_sat,
                Res.string.day_sun
            )
            weekDayResList.forEachIndexed { idx, resId ->
                Text(
                    text = stringResource(resId),
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

        val totalCells = firstDayOfWeek + daysInMonth
        val totalRows = (totalCells + 6) / 7

        var accumulatedDragX by remember { mutableStateOf(0f) }
        var accumulatedDragY by remember { mutableStateOf(0f) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(visibleMonth, isCollapsed) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            accumulatedDragX += dragAmount.x
                            accumulatedDragY += dragAmount.y
                        },
                        onDragEnd = {
                            val absX = kotlin.math.abs(accumulatedDragX)
                            val absY = kotlin.math.abs(accumulatedDragY)

                            if (absX > absY && absX > 35f) {
                                if (accumulatedDragX < 0) {
                                    onMonthChanged(visibleMonth.plus(1, DateTimeUnit.MONTH))
                                } else {
                                    onMonthChanged(visibleMonth.plus(-1, DateTimeUnit.MONTH))
                                }
                            } else if (absY > absX && absY > 25f) {
                                if (accumulatedDragY < 0 && !isCollapsed) {
                                    onToggleCollapse(true)
                                } else if (accumulatedDragY > 0 && isCollapsed) {
                                    onToggleCollapse(false)
                                }
                            }

                            accumulatedDragX = 0f
                            accumulatedDragY = 0f
                        },
                        onDragCancel = {
                            accumulatedDragX = 0f
                            accumulatedDragY = 0f
                        }
                    )
                }
        ) {
            for (r in 0 until totalRows) {
                val showRow = !isCollapsed || (r == selectedRowIndex)

                AnimatedVisibility(
                    visible = showRow,
                    enter = expandVertically(
                        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
                        expandFrom = Alignment.Top
                    ) + fadeIn(animationSpec = tween(280)),
                    exit = shrinkVertically(
                        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
                        shrinkTowards = Alignment.Top
                    ) + fadeOut(animationSpec = tween(220))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        for (c in 0..6) {
                            val cellIdx = r * 7 + c
                            val dayNum = cellIdx - firstDayOfWeek + 1

                            if (cellIdx >= firstDayOfWeek && dayNum <= daysInMonth) {
                                val cellDate = LocalDate(visibleMonth.year, visibleMonth.monthNumber, dayNum)
                                val isToday = cellDate == today
                                val isSelected = cellDate == selectedDate
                                val holidayName = holidays[cellDate]
                                val isHoliday = !holidayName.isNullOrBlank()
                                val isNewYear = cellDate.monthNumber == 1 && cellDate.dayOfMonth == 1

                                val myDayTasks = if (isMyTasksSelected) tasksByDate[cellDate] ?: emptyList() else emptyList()
                                val myPersonalTasks = myDayTasks.filter { it.participants.size <= 1 && it.sharedRoomIds.isEmpty() }
                                val mySharedTasks = myDayTasks.filter { it.participants.size > 1 || it.sharedRoomIds.isNotEmpty() }

                                val hasMyTasks = myPersonalTasks.isNotEmpty()
                                val hasMySharedTasks = mySharedTasks.isNotEmpty()

                                val existingBaseTaskIds = myDayTasks.map { it.id.extractBaseTaskId() }.toSet()
                                val otherTasksColors = mutableListOf<Color>()
                                val seenOtherBaseTaskIds = mutableSetOf<String>()

                                selectedSharedUserIds.forEach { userId ->
                                    val userTasks = sharedTasksByDate[userId]?.get(cellDate)
                                    if (!userTasks.isNullOrEmpty()) {
                                        sharedUserColors[userId]?.toColorOrNull()?.let { color ->
                                            for (task in userTasks) {
                                                val baseId = task.id.extractBaseTaskId()
                                                if (baseId !in existingBaseTaskIds && baseId !in seenOtherBaseTaskIds) {
                                                    seenOtherBaseTaskIds.add(baseId)
                                                    otherTasksColors.add(color)
                                                }
                                            }
                                        }
                                    }
                                }

                                val hasOtherTasks = otherTasksColors.isNotEmpty()

                                val bg = when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    isToday -> MaterialTheme.colorScheme.primaryContainer
                                    else -> Color.Transparent
                                }

                                val textColor = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                    isHoliday -> MaterialTheme.colorScheme.primary
                                    c >= 5 -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1.1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            onDateSelected(cellDate)
                                            onToggleCollapse(true)
                                        },
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
                                        if (isNewYear && !isSelected) {
                                            Text(
                                                text = "🎄",
                                                fontSize = 11.sp,
                                                modifier = Modifier.align(Alignment.TopEnd)
                                            )
                                        }
                                        Text(
                                            text = "$dayNum",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected || isToday || isHoliday) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 14.sp
                                            ),
                                            color = textColor
                                        )
                                    }

                                    if (hasMyTasks || hasMySharedTasks || hasOtherTasks) {
                                        val activeGroupCount = (if (hasMyTasks) 1 else 0) +
                                                (if (hasMySharedTasks) 1 else 0) +
                                                (if (hasOtherTasks) 1 else 0)
                                        val maxPerGroup = if (activeGroupCount > 1) 2 else 3

                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (hasMyTasks) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val count = minOf(myPersonalTasks.size, maxPerGroup)
                                                    repeat(count) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(5.dp)
                                                                .clip(CircleShape)
                                                                .background(MaterialTheme.colorScheme.primary)
                                                        )
                                                    }
                                                }
                                            }

                                            if (hasMyTasks && (hasMySharedTasks || hasOtherTasks)) {
                                                Spacer(modifier = Modifier.width(2.dp))
                                            }

                                            if (hasMySharedTasks) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val count = minOf(mySharedTasks.size, maxPerGroup)
                                                    val sharedDotColors = listOf(
                                                        MaterialTheme.colorScheme.primary,
                                                        MaterialTheme.colorScheme.secondary
                                                    )
                                                    repeat(count) {
                                                        MultiColorDot(
                                                            colors = sharedDotColors,
                                                            size = 5.dp
                                                        )
                                                    }
                                                }
                                            }

                                            if (hasMySharedTasks && hasOtherTasks) {
                                                Spacer(modifier = Modifier.width(2.dp))
                                            }

                                            if (hasOtherTasks) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val count = minOf(otherTasksColors.size, maxPerGroup)
                                                    for (i in 0 until count) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(5.dp)
                                                                .clip(CircleShape)
                                                                .background(otherTasksColors[i])
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                val isPrevMonth = cellIdx < firstDayOfWeek
                                val padDate = if (isPrevMonth) {
                                    val prevMonth = visibleMonth.plus(-1, DateTimeUnit.MONTH)
                                    val prevMonthDays = getDaysInMonth(prevMonth.year, prevMonth.monthNumber)
                                    val padDayNum = prevMonthDays - (firstDayOfWeek - 1 - cellIdx)
                                    LocalDate(prevMonth.year, prevMonth.monthNumber, padDayNum)
                                } else {
                                    val nextMonth = visibleMonth.plus(1, DateTimeUnit.MONTH)
                                    val padDayNum = cellIdx - firstDayOfWeek - daysInMonth + 1
                                    LocalDate(nextMonth.year, nextMonth.monthNumber, padDayNum)
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1.1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            val targetMonth = LocalDate(padDate.year, padDate.monthNumber, 1)
                                            onMonthChanged(targetMonth)
                                            onDateSelected(padDate)
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${padDate.dayOfMonth}",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 14.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleCollapse(!isCollapsed) }
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun getDaysInMonth(year: Int, monthNumber: Int): Int = when (monthNumber) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
    else -> 30
}
