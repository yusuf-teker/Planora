package com.yusufteker.planora.feature.home.presentation.home.components.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.planora.core.utils.getCurrentTimeMs
import com.yusufteker.planora.feature.home.presentation.home.AccessibleUser
import com.yusufteker.planora.feature.home.presentation.home.components.DayScheduleGrid
import com.yusufteker.planora.feature.home.presentation.home.components.NewYearSnow
import com.yusufteker.planora.shared.api.TaskDto
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.events_count_format
import planora.core.generated.resources.holiday_label

/**
 * Planora İnteraktif Takvim Bileşeni (PlanoraCalendarView).
 *
 * Yıl (12 Ay), Ay, Hafta (Daraltılmış 1 satır) ve Gün modları arasında geçiş sağlar.
 * İki parmakla sıkıştırma (Pinch In) hareketi ekranın HER YERİNDE Yıl görünümünü açar.
 * Seçilen özel günleri (resmi tatiller, yılbaşı vb.) kart bilgisi ve animasyonlarla gösterir.
 *
 * @param tasksByDate Tarihlere göre gruplanmış görevler
 * @param accessibleUsers Ortak kullanıcı listesi
 * @param selectedDate Seçili tarih
 * @param visibleMonth Gösterilen ay
 * @param holidays Özel günler haritası
 * @param upcomingTasks Seçili güne ait görevler
 * @param onDateSelected Tarih seçildiğinde tetiklenen olay
 * @param onMonthChanged Ay değiştiğinde tetiklenen olay
 * @param onTaskClick Göreve tıklandığında tetiklenen olay
 * @param modifier Dış düzenleyici
 */
@Composable
fun PlanoraCalendarView(
    tasksByDate: Map<LocalDate, List<TaskDto>>,
    sharedTasksByDate: Map<Int, Map<LocalDate, List<TaskDto>>> = emptyMap(),
    sharedUserColors: Map<Int, String> = emptyMap(),
    selectedSharedUserIds: Set<Int> = emptySet(),
    isMyTasksSelected: Boolean = true,
    accessibleUsers: List<AccessibleUser>,
    selectedDate: LocalDate?,
    visibleMonth: LocalDate?,
    holidays: Map<LocalDate, String> = emptyMap(),
    upcomingTasks: List<TaskDto>,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChanged: (LocalDate) -> Unit,
    onTaskClick: (TaskDto) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeZone = remember { TimeZone.currentSystemDefault() }
    val today = remember {
        Instant.fromEpochMilliseconds(getCurrentTimeMs()).toLocalDateTime(timeZone).date
    }

    val activeSelectedDate = selectedDate ?: today
    val activeVisibleMonth = visibleMonth ?: LocalDate(today.year, today.monthNumber, 1)

    var calendarMode by remember { mutableStateOf(CalendarMode.MONTH) }
    var isCollapsedToWeek by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            // 1. İKİ PARMAKLA KÜÇÜLTME (PointerEventPass.Initial ile Tarih Hücrelerinin Üzerinde de Çalışır!)
            .pointerInput(calendarMode) {
                awaitPointerEventScope {
                    var startZoomDistance = -1f

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val changes = event.changes

                        if (changes.size >= 2) {
                            val p1 = changes[0].position
                            val p2 = changes[1].position
                            val distance = Offset(p1.x - p2.x, p1.y - p2.y).getDistance()

                            if (startZoomDistance < 0f) {
                                startZoomDistance = distance
                            } else {
                                val scaleRatio = distance / startZoomDistance
                                if (scaleRatio < 0.85f && calendarMode != CalendarMode.YEAR) {
                                    calendarMode = CalendarMode.YEAR
                                    startZoomDistance = -1f
                                } else if (scaleRatio > 1.15f && calendarMode == CalendarMode.YEAR) {
                                    calendarMode = CalendarMode.MONTH
                                    startZoomDistance = -1f
                                }
                            }
                        }

                        if (changes.all { !it.pressed }) {
                            startZoomDistance = -1f
                        }
                    }
                }
            }
            // 2. HERHANGİ BİR YERDEN YUKARI / AŞAĞI SÜRÜKLEME (Collapse / Expand)
            .pointerInput(calendarMode, isCollapsedToWeek) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    if (dragAmount.y < -12f && calendarMode == CalendarMode.MONTH) {
                        isCollapsedToWeek = true
                        calendarMode = CalendarMode.WEEK
                    } else if (dragAmount.y > 12f && calendarMode == CalendarMode.WEEK) {
                        isCollapsedToWeek = false
                        calendarMode = CalendarMode.MONTH
                    }
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Ana Takvim Mod İçeriği
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = calendarMode,
                    transitionSpec = {
                        if (targetState == CalendarMode.MONTH && initialState == CalendarMode.YEAR) {
                            // Zoom In: Yıldan Aya geçiş (Seçilen ay büyüyerek açılır)
                            (scaleIn(initialScale = 0.55f, animationSpec = tween(380, easing = FastOutSlowInEasing)) + fadeIn(tween(300)))
                                .togetherWith(scaleOut(targetScale = 1.25f, animationSpec = tween(380, easing = FastOutSlowInEasing)) + fadeOut(tween(300)))
                        } else if (targetState == CalendarMode.YEAR && initialState != CalendarMode.YEAR) {
                            // Zoom Out: Aydan Yıla geçiş (Ay küçülerek 12 ay ızgarasına döner)
                            (scaleIn(initialScale = 1.25f, animationSpec = tween(380, easing = FastOutSlowInEasing)) + fadeIn(tween(300)))
                                .togetherWith(scaleOut(targetScale = 0.55f, animationSpec = tween(380, easing = FastOutSlowInEasing)) + fadeOut(tween(300)))
                        } else {
                            fadeIn(tween(300)).togetherWith(fadeOut(tween(300)))
                        }
                    },
                    label = "CalendarModeTransition"
                ) { mode ->
                    when (mode) {
                        CalendarMode.YEAR -> {
                            // Yıl Görünümü (12 Ay Izgarası)
                            YearGridView(
                                selectedDate = activeSelectedDate,
                                today = today,
                                tasksByDate = tasksByDate,
                                onMonthClick = { clickedMonth ->
                                    onMonthChanged(clickedMonth)
                                    calendarMode = CalendarMode.MONTH
                                    isCollapsedToWeek = false
                                },
                                onDateClick = { date ->
                                    onDateSelected(date)
                                    onMonthChanged(LocalDate(date.year, date.monthNumber, 1))
                                    calendarMode = CalendarMode.MONTH
                                }
                            )
                        }

                        CalendarMode.MONTH, CalendarMode.WEEK -> {
                            // Ay / Daraltılabilir Hafta Görünümü
                            val isWeekCollapsed = calendarMode == CalendarMode.WEEK || isCollapsedToWeek

                            Column(modifier = Modifier.fillMaxSize()) {
                                CollapsibleMonthWeekView(
                                    visibleMonth = activeVisibleMonth,
                                    selectedDate = activeSelectedDate,
                                    today = today,
                                    tasksByDate = tasksByDate,
                                    sharedTasksByDate = sharedTasksByDate,
                                    sharedUserColors = sharedUserColors,
                                    selectedSharedUserIds = selectedSharedUserIds,
                                    isMyTasksSelected = isMyTasksSelected,
                                    holidays = holidays,
                                    isCollapsed = isWeekCollapsed,
                                    onToggleCollapse = { collapsed ->
                                        isCollapsedToWeek = collapsed
                                        calendarMode = if (collapsed) CalendarMode.WEEK else CalendarMode.MONTH
                                    },
                                    onDateSelected = onDateSelected,
                                    onMonthChanged = onMonthChanged,
                                    onYearHeaderClick = {
                                        calendarMode = CalendarMode.YEAR
                                    }
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Seçili Gün Ajanda Başlık Şeridi (Ör. 17 Temmuz • 3 Etkinlik)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${activeSelectedDate.dayOfMonth} ${activeSelectedDate.month.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.events_count_format, upcomingTasks.size),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            ),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                // Özel Gün (Holiday) Kartı
                                val holidayName = holidays[activeSelectedDate]
                                if (!holidayName.isNullOrBlank()) {
                                    val isSelectedNewYear = activeSelectedDate.monthNumber == 1 && activeSelectedDate.dayOfMonth == 1
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 4.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isSelectedNewYear) {
                                                Box(modifier = Modifier.size(36.dp)) {
                                                    NewYearSnow(modifier = Modifier.fillMaxSize())
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                            } else {
                                                Text(
                                                    text = "🎉 ",
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = stringResource(Res.string.holiday_label),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                                )
                                                Text(
                                                    text = holidayName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                    }
                                }

                                // Alt Kısımda Seçili Günün Saatlik Etkinlik Çizelgesi
                                DayScheduleGrid(
                                    selectedDate = activeSelectedDate,
                                    tasks = upcomingTasks,
                                    accessibleUsers = accessibleUsers,
                                    onTaskClick = onTaskClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 12.dp)
                                )
                            }
                        }

                        CalendarMode.DAY -> {
                            // Günlük Tam Boyutlu Ajanda Görünümü
                            Column(modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = "${activeSelectedDate.dayOfMonth} ${activeSelectedDate.month} ${activeSelectedDate.year}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )

                                // Özel Gün (Holiday) Kartı
                                val holidayName = holidays[activeSelectedDate]
                                if (!holidayName.isNullOrBlank()) {
                                    val isSelectedNewYear = activeSelectedDate.monthNumber == 1 && activeSelectedDate.dayOfMonth == 1
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 4.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isSelectedNewYear) {
                                                Box(modifier = Modifier.size(36.dp)) {
                                                    NewYearSnow(modifier = Modifier.fillMaxSize())
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                            } else {
                                                Text(
                                                    text = "🎉 ",
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = stringResource(Res.string.holiday_label),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                                )
                                                Text(
                                                    text = holidayName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                    }
                                }

                                DayScheduleGrid(
                                    selectedDate = activeSelectedDate,
                                    tasks = upcomingTasks,
                                    accessibleUsers = accessibleUsers,
                                    onTaskClick = onTaskClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
