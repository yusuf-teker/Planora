package com.yusufteker.planora.feature.home.presentation.home.components.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.planora.shared.api.TaskDto
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.action_next_month
import planora.core.generated.resources.action_prev_month
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

/**
 * Planora Takvim Yıl Görünümü (Year Grid View).
 *
 * 12 ayı 3x4 dikey ızgara halinde gösterir. Sağa/sola kaydırarak yıllar değişir.
 * Bir aya tıklandığında ay görünümüne geçiş sağlar.
 *
 * @param selectedDate Seçili tarih
 * @param today Bugünün tarihi
 * @param tasksByDate Tarihlere göre görev haritası
 * @param onMonthClick Bir aya tıklandığında tetiklenen olay (o aya Zoom yapar)
 * @param onDateClick Tekil güne tıklandığında tetiklenen olay
 * @param modifier Dış düzenleyici
 */
@Composable
fun YearGridView(
    selectedDate: LocalDate?,
    today: LocalDate,
    tasksByDate: Map<LocalDate, List<TaskDto>>,
    onMonthClick: (LocalDate) -> Unit,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialYear = remember { selectedDate?.year ?: today.year }
    val baseYear = initialYear - 2
    val pagerState = rememberPagerState(initialPage = 2, pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize()) {
        // Yıl Başlığı ve Oklar (Ör. 2026)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val currentYear = baseYear + pagerState.currentPage

            Text(
                text = "$currentYear",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Row {
                IconButton(
                    onClick = {
                        if (pagerState.currentPage > 0) {
                            coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = stringResource(Res.string.action_prev_month)
                    )
                }
                IconButton(
                    onClick = {
                        if (pagerState.currentPage < 4) {
                            coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = stringResource(Res.string.action_next_month)
                    )
                }
            }
        }

        // Yıllar Arası Sağa/Sola Swipable Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            val displayYear = baseYear + page

            // 12 Ayın 3x4 Izgara Şeklinde Dizilimi
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(12) { monthIdx ->
                    val monthNumber = monthIdx + 1
                    val monthDate = LocalDate(displayYear, monthNumber, 1)

                    MiniMonthCard(
                        monthDate = monthDate,
                        today = today,
                        selectedDate = selectedDate,
                        tasksByDate = tasksByDate,
                        onMonthClick = { onMonthClick(monthDate) },
                        onDateClick = onDateClick
                    )
                }
            }
        }
    }
}

/**
 * Tek bir ayın mini ızgara görünümünü temsil eder.
 */
@Composable
private fun MiniMonthCard(
    monthDate: LocalDate,
    today: LocalDate,
    selectedDate: LocalDate?,
    tasksByDate: Map<LocalDate, List<TaskDto>>,
    onMonthClick: () -> Unit,
    onDateClick: (LocalDate) -> Unit
) {
    val monthNameRes = getMonthNameRes(monthDate.monthNumber)
    val daysInMonth = getDaysInMonth(monthDate.year, monthDate.monthNumber)
    val firstDayOfWeek = LocalDate(monthDate.year, monthDate.monthNumber, 1).dayOfWeek.ordinal // 0: Mon, 6: Sun

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMonthClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
        ) {
            // Ay İsmi (Ör. "Oca", "Tem")
            Text(
                text = stringResource(monthNameRes),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = if (monthDate.year == today.year && monthDate.monthNumber == today.monthNumber) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
            )

            // Gün isimleri başlığı (1 2 3 4 5 6 7)
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("1", "2", "3", "4", "5", "6", "7").forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Mini Gün Izgarası (7 sütun)
            val totalCells = firstDayOfWeek + daysInMonth
            val rows = (totalCells + 6) / 7

            Column {
                for (r in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (c in 0..6) {
                            val cellIdx = r * 7 + c
                            val dayNum = cellIdx - firstDayOfWeek + 1

                            if (cellIdx >= firstDayOfWeek && dayNum <= daysInMonth) {
                                val cellDate = LocalDate(monthDate.year, monthDate.monthNumber, dayNum)
                                val isToday = cellDate == today
                                val isSelected = cellDate == selectedDate

                                val bg = when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    isToday -> MaterialTheme.colorScheme.primaryContainer
                                    else -> Color.Transparent
                                }

                                val textColor = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                    c == 6 || c == 5 -> Color(0xFF60A5FA) // Hafta sonu mavi renkte
                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(0.5.dp)
                                        .clip(CircleShape)
                                        .background(bg)
                                        .clickable { onDateClick(cellDate) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$dayNum",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 7.sp,
                                            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = textColor
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getMonthNameRes(monthNumber: Int) = when (monthNumber) {
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

private fun getDaysInMonth(year: Int, monthNumber: Int): Int = when (monthNumber) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
    else -> 30
}
