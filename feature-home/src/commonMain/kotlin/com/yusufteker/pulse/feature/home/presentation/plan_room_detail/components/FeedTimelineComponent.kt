package com.yusufteker.pulse.feature.home.presentation.plan_room_detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yusufteker.pulse.shared.api.TaskDto
import com.yusufteker.pulse.shared.api.UserProfileResponse
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import pulsy.core.generated.resources.Res
import pulsy.core.generated.resources.*
import com.yusufteker.pulse.feature.home.presentation.home.components.TimelineTaskCard

@Composable
fun FeedTimelineComponent(
    tasks: List<TaskDto>,
    memberProfiles: Map<Int, UserProfileResponse>,
    onTaskClick: (TaskDto) -> Unit = {}
) {
    var showPastTasks by remember { mutableStateOf(false) }
    
    val today = kotlinx.datetime.Instant.fromEpochMilliseconds(com.yusufteker.pulse.core.utils.getCurrentTimeMs()).toLocalDateTime(TimeZone.currentSystemDefault()).date
    
    val sortedTasks = remember(tasks, showPastTasks) {
        val filtered = if (showPastTasks) {
            tasks
        } else {
            tasks.filter { 
                val taskDate = Instant.fromEpochMilliseconds(it.startTime).toLocalDateTime(TimeZone.currentSystemDefault()).date
                taskDate >= today
            }
        }
        filtered.sortedBy { it.startTime }
    }
    
    val groupedTasks = remember(sortedTasks) {
        sortedTasks.groupBy { 
            Instant.fromEpochMilliseconds(it.startTime).toLocalDateTime(TimeZone.currentSystemDefault()).date
        }
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            if (!showPastTasks) {
                TextButton(
                    onClick = { showPastTasks = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(Res.string.show_past_events))
                }
            } else {
                TextButton(
                    onClick = { showPastTasks = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(Res.string.hide_past_events))
                }
            }
        }
        
        if (groupedTasks.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(Res.string.empty_events_prompt), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        
        groupedTasks.forEach { (date, tasksForDate) ->
            item {
                DateHeader(date = date, isToday = date == today)
            }
            items(tasksForDate, key = { it.id }) { task ->
                val profile = memberProfiles[task.creatorId]
                val hasParticipants = task.participants.isNotEmpty()
                
                TimelineTaskCard(
                    task = task, 
                    showDate = false,
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
    val dateText = if (isToday) "Bugün" else "${date.dayOfMonth} ${monthName(date.monthNumber)} ${date.year}"
    
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

private fun monthName(month: Int): String {
    return when(month) {
        1 -> "Ocak" 2 -> "Şubat" 3 -> "Mart" 4 -> "Nisan" 5 -> "Mayıs" 6 -> "Haziran"
        7 -> "Temmuz" 8 -> "Ağustos" 9 -> "Eylül" 10 -> "Ekim" 11 -> "Kasım" 12 -> "Aralık"
        else -> ""
    }
}
