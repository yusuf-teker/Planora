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
import com.yusufteker.planora.core.theme.getTaskTypeColor
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
        verticalArrangement = Arrangement.spacedBy(10.dp)
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(Res.string.empty_events_today),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
 * Tüm gün etkinliklerini üstte şık ve ferah bir liste halinde gösterir.
 */
@Composable
private fun AllDayTasksSection(
    allDayTasks: List<TaskDto>,
    accessibleUsers: List<AccessibleUser>,
    onTaskClick: (TaskDto) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(Res.string.all_day_tasks),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                allDayTasks.forEach { task ->
                    val typeColor = getTaskTypeColor(task.type)
                    val creatorUser = accessibleUsers.find { it.userId == task.creatorId }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(typeColor.copy(alpha = 0.12f))
                            .border(1.dp, typeColor.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                            .clickable { onTaskClick(task) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(typeColor)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Katılımcı avatarları
                        ParticipantAvatarsRow(
                            task = task,
                            creatorUser = creatorUser,
                            avatarSize = 22.dp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Ferah ve Okunaklı Saatlik Etkinlik / Görev Kartı.
 *
 * Saat rozeti, tür renk indikatörü, başlık ve katılımcı avatarlarını dengeli ve okunaklı bir boyutta gösterir.
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
        typeColor.copy(alpha = 0.16f)
    } else {
        typeColor.copy(alpha = 0.09f)
    }

    val primaryTime = (task.specificDetails as? com.yusufteker.planora.shared.api.ItemDetails.Task)?.deadline ?: task.startTime
    val startStr = formatTime(primaryTime)
    val endStr = task.endTime?.let { formatTime(it) }
    val timeSpanText = if (endStr != null) "$startStr - $endStr" else startStr

    val location = (task.specificDetails as? com.yusufteker.planora.shared.api.ItemDetails.Event)?.location

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, typeColor.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Sol: Saat Rozeti
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(typeColor.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = typeColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = timeSpanText,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = typeColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 2. Orta: Tür Çubuğu + Başlık (+ Varsa Konum)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(if (!location.isNullOrBlank()) 32.dp else 22.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(typeColor)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!location.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = location,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 3. Sağ: Katılımcı Görselleri
            ParticipantAvatarsRow(
                task = task,
                creatorUser = creatorUser,
                avatarSize = 24.dp
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
    creatorUser: AccessibleUser?,
    avatarSize: androidx.compose.ui.unit.Dp = 24.dp
) {
    val participantsList = task.participants
    val hasParticipants = participantsList.size > 1

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((-8).dp)
    ) {
        if (hasParticipants) {
            participantsList.take(3).forEachIndexed { index, participant ->
                AvatarImage(
                    avatarId = participant.avatarId,
                    profileImageUrl = participant.profileImageUrl,
                    modifier = Modifier
                        .size(avatarSize)
                        .clip(CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape)
                        .zIndex(3f - index)
                )
            }
            if (participantsList.size > 3) {
                Box(
                    modifier = Modifier
                        .size(avatarSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape)
                        .zIndex(0f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+${participantsList.size - 3}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
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
                    .size(avatarSize)
                    .clip(CircleShape)
                    .border(1.5.dp, creatorColor, CircleShape)
            )
        }
    }
}
