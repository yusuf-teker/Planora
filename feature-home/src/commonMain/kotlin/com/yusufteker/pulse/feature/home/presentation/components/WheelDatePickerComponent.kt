package com.yusufteker.pulse.feature.home.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import org.jetbrains.compose.resources.stringResource
import pulsy.core.generated.resources.Res
import pulsy.core.generated.resources.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> WheelPicker(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    itemToString: (T) -> String,
    modifier: Modifier = Modifier,
    visibleItemsCount: Int = 3,
    itemHeight: Dp = 40.dp
) {
    val initialIndex = items.indexOf(selectedItem).takeIf { it >= 0 } ?: 0
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val isScrollInProgress = listState.isScrollInProgress

    LaunchedEffect(isScrollInProgress) {
        if (!isScrollInProgress) {
            val centerIndex = listState.firstVisibleItemIndex
            if (centerIndex in items.indices) {
                onItemSelected(items[centerIndex])
            }
        }
    }
    
    // When selectedItem changes externally, animate to it
    LaunchedEffect(selectedItem) {
        val index = items.indexOf(selectedItem)
        if (index >= 0 && index != listState.firstVisibleItemIndex) {
            listState.animateScrollToItem(index)
        }
    }

    val halfCount = visibleItemsCount / 2
    
    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        modifier = modifier.height(itemHeight * visibleItemsCount)
    ) {
        items(count = halfCount) {
            Box(modifier = Modifier.height(itemHeight))
        }
        items(count = items.size) { index ->
            // Recompute firstVisibleItemIndex for reactive UI changes
            val currentCenter = listState.firstVisibleItemIndex
            val isSelected = currentCenter == index
            Box(
                modifier = Modifier.height(itemHeight).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = itemToString(items[index]),
                    fontSize = if (isSelected) 18.sp else 16.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
        items(count = halfCount) {
            Box(modifier = Modifier.height(itemHeight))
        }
    }
}

@Composable
fun WheelDatePicker(
    modifier: Modifier = Modifier,
    initialDateMillis: Long,
    minDateMillis: Long? = null,
    maxDateMillis: Long? = null,
    onDateSelected: (Long) -> Unit
) {
    val currentInstant = Instant.fromEpochMilliseconds(initialDateMillis)
    val currentDate = currentInstant.toLocalDateTime(TimeZone.currentSystemDefault()).date
    
    var selectedYear by remember { mutableStateOf(currentDate.year) }
    var selectedMonth by remember { mutableStateOf(currentDate.monthNumber) }
    var selectedDay by remember { mutableStateOf(currentDate.dayOfMonth) }

    val minDate = minDateMillis?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).date }
    val maxDate = maxDateMillis?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).date }

    val minYear = minDate?.year ?: (currentDate.year - 10)
    val maxYear = maxDate?.year ?: (currentDate.year + 10)
    val years = (minYear..maxYear).toList()

    val minMonth = if (selectedYear == minDate?.year) minDate.monthNumber else 1
    val maxMonth = if (selectedYear == maxDate?.year) maxDate.monthNumber else 12
    val months = (minMonth..maxMonth).toList()

    LaunchedEffect(selectedYear, minMonth, maxMonth) {
        if (selectedMonth < minMonth) selectedMonth = minMonth
        if (selectedMonth > maxMonth) selectedMonth = maxMonth
    }

    val daysInMonth = remember(selectedYear, selectedMonth) {
        try {
            val startOfNextMonth = if (selectedMonth == 12) {
                LocalDate(selectedYear + 1, 1, 1)
            } else {
                LocalDate(selectedYear, selectedMonth + 1, 1)
            }
            val lastDay = startOfNextMonth - DatePeriod(days = 1)
            lastDay.dayOfMonth
        } catch (e: Exception) {
            31
        }
    }

    val minDay = if (selectedYear == minDate?.year && selectedMonth == minDate.monthNumber) minDate.dayOfMonth else 1
    val maxDay = if (selectedYear == maxDate?.year && selectedMonth == maxDate.monthNumber) minOf(maxDate.dayOfMonth, daysInMonth) else daysInMonth
    val days = (minDay..maxDay).toList()

    LaunchedEffect(selectedYear, selectedMonth, minDay, maxDay) {
        if (selectedDay < minDay) selectedDay = minDay
        if (selectedDay > maxDay) selectedDay = maxDay
    }

    LaunchedEffect(selectedYear, selectedMonth, selectedDay) {
        try {
            val newDate = LocalDate(selectedYear, selectedMonth, selectedDay)
            val newInstant = newDate.atStartOfDayIn(TimeZone.currentSystemDefault())
            onDateSelected(newInstant.toEpochMilliseconds())
        } catch (e: Exception) {}
    }

    val monthNames = listOf(
        stringResource(Res.string.month_jan),
        stringResource(Res.string.month_feb),
        stringResource(Res.string.month_mar),
        stringResource(Res.string.month_apr),
        stringResource(Res.string.month_may),
        stringResource(Res.string.month_jun),
        stringResource(Res.string.month_jul),
        stringResource(Res.string.month_aug),
        stringResource(Res.string.month_sep),
        stringResource(Res.string.month_oct),
        stringResource(Res.string.month_nov),
        stringResource(Res.string.month_dec)
    )

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        // Highlight background for the selected row
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WheelPicker(
                items = years,
                selectedItem = selectedYear,
                onItemSelected = { selectedYear = it },
                itemToString = { it.toString() },
                modifier = Modifier.weight(1f)
            )
            WheelPicker(
                items = months,
                selectedItem = selectedMonth,
                onItemSelected = { selectedMonth = it },
                itemToString = { monthNames[it - 1] },
                modifier = Modifier.weight(1f)
            )
            WheelPicker(
                items = days,
                selectedItem = selectedDay,
                onItemSelected = { selectedDay = it },
                itemToString = { it.toString() },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun WheelTimePicker(
    modifier: Modifier = Modifier,
    initialHour: Int,
    initialMinute: Int,
    onTimeSelected: (hour: Int, minute: Int) -> Unit
) {
    var selectedHour by remember { mutableStateOf(initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }

    LaunchedEffect(selectedHour, selectedMinute) {
        onTimeSelected(selectedHour, selectedMinute)
    }

    val hours = (0..23).toList()
    val minutes = (0..59).toList()

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        // Highlight background for the selected row
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
        )
        Row(
            modifier = Modifier.fillMaxWidth(0.7f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WheelPicker(
                items = hours,
                selectedItem = selectedHour,
                onItemSelected = { selectedHour = it },
                itemToString = { it.toString().padStart(2, '0') },
                modifier = Modifier.weight(1f)
            )
            Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
            WheelPicker(
                items = minutes,
                selectedItem = selectedMinute,
                onItemSelected = { selectedMinute = it },
                itemToString = { it.toString().padStart(2, '0') },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
