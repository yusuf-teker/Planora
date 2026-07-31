package com.yusufteker.planora.feature.home.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.yusufteker.planora.core.ui.components.AvatarImage
import com.yusufteker.planora.core.utils.formatTime
import com.yusufteker.planora.feature.home.presentation.home.AccessibleUser
import com.yusufteker.planora.shared.api.TaskDto
import com.yusufteker.planora.shared.api.TaskType
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.all_day_tasks
import planora.core.generated.resources.empty_events_today

/**
 * Seçili günün etkinliklerini kompakt, saat bilgisi üstte olacak şekilde ve
 * boş saatleri gizleyerek gösteren ajanda tablosu (Day Schedule Grid).
 *
 * @param selectedDate Seçili tarih
 * @param tasks O güne ait tüm görev ve etkinlikler
 * @param accessibleUsers Ortak takvim erişimine sahip kullanıcı listesi
 * @param onTaskClick Etkinliğe tıklama dinleyicisi
 * @param modifier Dış düzenleyici
 */
@Composable
fun DayScheduleGrid(
    selectedDate: LocalDate,
    tasks: List<TaskDto>,
    accessibleUsers: List<AccessibleUser>,
    onTaskClick: (TaskDto) -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. All-Day ve Saatli (Timed) görev ayrımı
    val (allDayTasks, timedTasks) = remember(tasks) {
        tasks.partition { it.isAllDay }
    }

    // 2. Saatli görevleri başlangıç zamanına göre sıralama
    val sortedTimedTasks = remember(timedTasks) {
        timedTasks.sortedBy { task ->
            (task.specificDetails as? com.yusufteker.planora.shared.api.ItemDetails.Task)?.deadline ?: task.startTime
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Tüm Gün (All-Day) Etkinlikler Bölümü
        if (allDayTasks.isNotEmpty()) {
            AllDayTasksSection(
                allDayTasks = allDayTasks,
                accessibleUsers = accessibleUsers,
                onTaskClick = onTaskClick
            )
        }

        // Eğer hiç etkinlik yoksa
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(Res.string.empty_events_today),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Sadece dolu olan saatlerin kompakt kartlar halinde listelenmesi (Boş saatler gizlendi)
            sortedTimedTasks.forEach { task ->
                val creatorUser = accessibleUsers.find { it.userId == task.creatorId }
                CompactHourlyEventCard(
                    task = task,
                    creatorUser = creatorUser,
                    onClick = { onTaskClick(task) }
                )
            }
        }
    }
}

/**
 * Tüm gün etkinliklerini üstte kompakt yatay liste halinde gösterir.
 */
@Composable
private fun AllDayTasksSection(
    allDayTasks: List<TaskDto>,
    accessibleUsers: List<AccessibleUser>,
    onTaskClick: (TaskDto) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = stringResource(Res.string.all_day_tasks),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            allDayTasks.forEach { task ->
                val typeColor = getTaskTypeColor(task.type)
                val creatorUser = accessibleUsers.find { it.userId == task.creatorId }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(typeColor.copy(alpha = 0.15f))
                        .border(1.dp, typeColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .clickable { onTaskClick(task) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Katılımcı avatarları
                    ParticipantAvatarsRow(
                        task = task,
                        creatorUser = creatorUser
                    )
                }
            }
        }
    }
}

/**
 * Ultra Kompakt Tek Satırlı Etkinlik Kartı.
 *
 * Saat rozeti, tür renk indikatörü, başlık ve katılımcı avatarlarını TEK BİR SATIRDA birleştirir.
 *
 * @param task Etkinlik verisi
 * @param creatorUser Oluşturan kullanıcı bilgisi
 * @param onClick Kart tıklama dinleyicisi
 */
@Composable
private fun CompactHourlyEventCard(
    task: TaskDto,
    creatorUser: AccessibleUser?,
    onClick: () -> Unit
) {
    val typeColor = getTaskTypeColor(task.type)
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val containerBg = if (isDark) {
        typeColor.copy(alpha = 0.15f)
    } else {
        typeColor.copy(alpha = 0.08f)
    }

    val primaryTime = (task.specificDetails as? com.yusufteker.planora.shared.api.ItemDetails.Task)?.deadline ?: task.startTime
    val startStr = formatTime(primaryTime)
    val endStr = task.endTime?.let { formatTime(it) }
    val timeSpanText = if (endStr != null) "$startStr - $endStr" else startStr

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, typeColor.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Sol: Saat Rozeti
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(typeColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = typeColor,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = timeSpanText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = typeColor
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 2. Orta: Tür Çubuğu + Başlık
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(typeColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // 3. Sağ: Katılımcı Görselleri
            ParticipantAvatarsRow(
                task = task,
                creatorUser = creatorUser
            )
        }
    }
}

/**
 * Görev/Etkinlik katılımcılarının avatar ikonlarını üste binmiş küçük halkalar olarak çizer.
 */
@Composable
private fun ParticipantAvatarsRow(
    task: TaskDto,
    creatorUser: AccessibleUser?
) {
    val participantsList = task.participants
    val hasParticipants = participantsList.size > 1

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((-6).dp)
    ) {
        if (hasParticipants) {
            participantsList.take(3).forEachIndexed { index, participant ->
                AvatarImage(
                    avatarId = participant.avatarId,
                    profileImageUrl = participant.profileImageUrl,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.background, CircleShape)
                        .zIndex(3f - index)
                )
            }
            if (participantsList.size > 3) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.background, CircleShape)
                        .zIndex(0f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+${participantsList.size - 3}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (creatorUser != null) {
            val creatorColor = try {
                Color(creatorUser.color.removePrefix("#").toLong(16) or 0xFF000000)
            } catch (e: Exception) {
                MaterialTheme.colorScheme.primary
            }

            AvatarImage(
                avatarId = creatorUser.avatarId,
                profileImageUrl = creatorUser.profileImageUrl,
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .border(1.dp, creatorColor, CircleShape)
            )
        }
    }
}

/**
 * Etkinlik türüne uygun renk döndürür.
 */
private fun getTaskTypeColor(type: TaskType): Color {
    return when (type) {
        TaskType.EVENT -> Color(0xFF6366F1)  // Indigo
        TaskType.TASK -> Color(0xFF10B981)   // Emerald
        TaskType.NOTE -> Color(0xFFF59E0B)   // Amber
        TaskType.FOLDER -> Color(0xFF22C55E) // Lime
    }
}
