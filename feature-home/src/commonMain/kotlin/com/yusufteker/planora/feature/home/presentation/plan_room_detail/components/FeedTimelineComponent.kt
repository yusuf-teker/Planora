package com.yusufteker.planora.feature.home.presentation.plan_room_detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yusufteker.planora.shared.api.TaskDto
import com.yusufteker.planora.shared.api.UserProfileResponse
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.*
import com.yusufteker.planora.shared.api.ItemDetails
import com.yusufteker.planora.feature.home.presentation.home.components.TimelineTaskCard

/**
 * Plan odası akış zaman çizelgesi bileşeni (FeedTimelineComponent).
 *
 * Akışta odadaki tüm görev ve etkinlikleri yeniden eskiye (en son eklenen / en güncel tarih en üstte)
 * olacak şekilde listeler.
 */
@Composable
fun FeedTimelineComponent(
    tasks: List<TaskDto>,
    memberProfiles: Map<Int, UserProfileResponse>,
    roomImageUrl: String? = null,
    roomName: String? = null,
    onTaskClick: (TaskDto) -> Unit = {}
) {
    val listState = rememberLazyListState()

    val timeZone = remember { TimeZone.currentSystemDefault() }
    val today = remember {
        val nowMs = com.yusufteker.planora.core.utils.getCurrentTimeMs()
        Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(timeZone).date
    }

    // Yeniden eskiye (En son eklenen / en güncel tarih en üstte olacak şekilde azalan sıralama)
    val sortedTasks = remember(tasks) {
        tasks.sortedByDescending { (it.specificDetails as? ItemDetails.Task)?.deadline ?: it.startTime }
    }

    val groupedTasks = remember(sortedTasks) {
        sortedTasks.groupBy { task ->
            val effectiveTime = (task.specificDetails as? ItemDetails.Task)?.deadline ?: task.startTime
            Instant.fromEpochMilliseconds(effectiveTime).toLocalDateTime(timeZone).date
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (groupedTasks.isEmpty()) {
            item(key = "empty_state") {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(Res.string.empty_events_prompt), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        groupedTasks.forEach { (date, tasksForDate) ->
            item(key = "header_${date}") {
                DateHeader(date = date, isToday = date == today)
            }
            items(tasksForDate, key = { it.id }) { task ->
                val profile = memberProfiles[task.creatorId]
                val hasParticipants = task.participants.isNotEmpty()

                TimelineTaskCard(
                    modifier = Modifier.padding(vertical = 6.dp),
                    task = task,
                    showDate = false,
                    roomImageUrl = roomImageUrl,
                    roomName = roomName,
                    sharedUserAvatar = if (hasParticipants) null else profile?.avatarId,
                    sharedUserColor = null,
                    sharedUserProfileImageUrl = if (hasParticipants) null else profile?.profileImageUrl,
                    onClick = { onTaskClick(task) }
                )
            }
        }
    }
}

@Composable
private fun DateHeader(date: LocalDate, isToday: Boolean) {
    val dateText = if (isToday) stringResource(Res.string.today) else "${date.dayOfMonth} ${monthName(date.monthNumber)} ${date.year}"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        Text(
            text = dateText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun monthName(month: Int): String {
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
