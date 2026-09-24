package com.yusufteker.planora.feature.home.presentation.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.planora.core.ui.components.GlassCard
import com.yusufteker.planora.core.ui.components.bounceClick
import com.yusufteker.planora.core.utils.formatShortDate
import com.yusufteker.planora.core.utils.formatTime
import com.yusufteker.planora.feature.home.presentation.components.SwipeableTaskItem
import com.yusufteker.planora.feature.home.presentation.home.HomeState
import com.yusufteker.planora.shared.api.ItemDetails
import com.yusufteker.planora.shared.api.TaskDto
import com.yusufteker.planora.shared.api.TaskPriority
import com.yusufteker.planora.shared.api.TaskStatus
import com.yusufteker.planora.shared.api.TaskType
import com.yusufteker.planora.shared.api.isUnscheduled
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.action_change_priority
import planora.core.generated.resources.action_move_down
import planora.core.generated.resources.action_move_up
import planora.core.generated.resources.action_pin
import planora.core.generated.resources.action_unpin
import planora.core.generated.resources.priority_high
import planora.core.generated.resources.priority_low
import planora.core.generated.resources.priority_medium
import planora.core.generated.resources.priority_urgent
import planora.core.generated.resources.tasks_hub_action_add
import planora.core.generated.resources.tasks_hub_empty_scheduled
import planora.core.generated.resources.tasks_hub_empty_unscheduled
import planora.core.generated.resources.tasks_hub_quick_add_placeholder
import planora.core.generated.resources.tasks_hub_section_completed
import planora.core.generated.resources.tasks_tab_backlog
import planora.core.generated.resources.tasks_tab_scheduled

/**
 * Görevler merkezinin 2 ana sekmesi:
 * - [BACKLOG]: Tarihi/deadline'ı olmayan zamansız yapılacaklar havuzu.
 * - [SCHEDULED]: Belirli bir tarih ve saate planlanmış görevler.
 */
enum class TasksHubTab {
    BACKLOG,
    SCHEDULED
}

/**
 * Görevlerin toplandığı, 2 sekmeye (Yapılacaklar Havuzu vs Planlanmış Görevler) ayrılmış,
 * havuzda öneme göre yukarı/aşağı taşıma ve öncelik değiştirme imkanı sunan merkez bileşen.
 *
 * @param state Ana ekran UI durumu ([HomeState]).
 * @param onTaskClick Görev detayına gitmek için tıklama lambda'sı.
 * @param onTaskToggleStatus Görevin tamamlandı/bekliyor durumunu değiştiren lambda.
 * @param onTaskDelete Görevi silen lambda.
 * @param onQuickCreateTask Hızlı to-do ekleme lambda'sı.
 * @param onTaskTogglePin Görevin başa sabitleme durumunu değiştiren lambda.
 * @param onTaskChangePriority Görevin öncelik seviyesini güncelleyen lambda.
 * @param onMoveTaskOrder Havuzdaki görevin sırasını yukarı/aşağı taşıyan lambda.
 * @param modifier Dış düzenleyici.
 */
@Composable
fun TasksHubSection(
    state: HomeState,
    onTaskClick: (TaskDto) -> Unit,
    onTaskToggleStatus: (TaskDto) -> Unit,
    onTaskDelete: (String) -> Unit,
    onQuickCreateTask: (String) -> Unit,
    onTaskTogglePin: (taskId: String, isPinned: Boolean) -> Unit,
    onTaskChangePriority: (TaskDto, TaskPriority) -> Unit,
    onMoveTaskOrder: (TaskDto, isUp: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(TasksHubTab.BACKLOG) }
    var quickAddText by remember { mutableStateOf("") }
    var isCompletedExpanded by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Sadece görev tipindeki öğeleri filtrele
    val allTasks = remember(state.upcomingTasks) {
        state.upcomingTasks.filter { it.type == TaskType.TASK }
    }

    // Zamansız / Havuz (Unscheduled Backlog) görevler: Pinned öğeler üstte, ardından en son eklenenler/düzenlenenler
    val unscheduledPendingTasks = remember(allTasks) {
        allTasks
            .filter { it.isUnscheduled() && it.status != TaskStatus.COMPLETED }
            .sortedWith(compareByDescending<TaskDto> { it.isPinned }.thenByDescending { it.startTime })
    }

    // Planlanmış / Tarihli bekleyen görevler: Pinned öğeler üstte, ardından deadline/startTime kronolojik sırayla
    val scheduledPendingTasks = remember(allTasks) {
        allTasks
            .filter { !it.isUnscheduled() && it.status != TaskStatus.COMPLETED }
            .sortedWith(
                compareByDescending<TaskDto> { it.isPinned }
                    .thenBy { (it.specificDetails as? ItemDetails.Task)?.deadline ?: it.startTime }
            )
    }

    // Aktif sekmedeki tamamlanan görevler
    val completedTasks = remember(allTasks, selectedTab) {
        allTasks.filter { task ->
            task.status == TaskStatus.COMPLETED &&
                if (selectedTab == TasksHubTab.BACKLOG) task.isUnscheduled() else !task.isUnscheduled()
        }
    }

    val submitQuickTask: () -> Unit = {
        val trimmed = quickAddText.trim()
        if (trimmed.isNotEmpty()) {
            onQuickCreateTask(trimmed)
            quickAddText = ""
            focusManager.clearFocus()
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // 1. Üst Sekme Seçici (Yapılacaklar Havuzu | Planlanmış Görevler)
        TasksHubTabSelector(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            backlogCount = unscheduledPendingTasks.size,
            scheduledCount = scheduledPendingTasks.size,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                TasksHubTab.BACKLOG -> {
                    // Hızlı to-do ekleme çubuğu
                    item(key = "quick_add_bar") {
                        QuickAddTaskBar(
                            text = quickAddText,
                            onTextChanged = { quickAddText = it },
                            onAddClicked = submitQuickTask
                        )
                    }

                    if (unscheduledPendingTasks.isEmpty()) {
                        item(key = "empty_unscheduled") {
                            TasksHubEmptyHint(message = stringResource(Res.string.tasks_hub_empty_unscheduled))
                        }
                    } else {
                        itemsIndexed(
                            items = unscheduledPendingTasks,
                            key = { _, task -> "unscheduled_${task.id}" }
                        ) { index, task ->
                            SwipeableTaskItem(
                                onSwipeRightToComplete = { onTaskToggleStatus(task) },
                                onSwipeLeftToDelete = { onTaskDelete(task.id) }
                            ) {
                                TasksHubBacklogCard(
                                    task = task,
                                    isFirst = index == 0,
                                    isLast = index == unscheduledPendingTasks.size - 1,
                                    onTaskClick = { onTaskClick(task) },
                                    onToggleStatus = { onTaskToggleStatus(task) },
                                    onTogglePin = { onTaskTogglePin(task.id, !task.isPinned) },
                                    onChangePriority = { newPriority -> onTaskChangePriority(task, newPriority) },
                                    onMoveUp = { onMoveTaskOrder(task, true) },
                                    onMoveDown = { onMoveTaskOrder(task, false) }
                                )
                            }
                        }
                    }
                }

                TasksHubTab.SCHEDULED -> {
                    if (scheduledPendingTasks.isEmpty()) {
                        item(key = "empty_scheduled") {
                            TasksHubEmptyHint(message = stringResource(Res.string.tasks_hub_empty_scheduled))
                        }
                    } else {
                        items(
                            items = scheduledPendingTasks,
                            key = { "scheduled_${it.id}" }
                        ) { task ->
                            SwipeableTaskItem(
                                onSwipeRightToComplete = { onTaskToggleStatus(task) },
                                onSwipeLeftToDelete = { onTaskDelete(task.id) }
                            ) {
                                TasksHubScheduledCard(
                                    task = task,
                                    onTaskClick = { onTaskClick(task) },
                                    onToggleStatus = { onTaskToggleStatus(task) },
                                    onTogglePin = { onTaskTogglePin(task.id, !task.isPinned) },
                                    onChangePriority = { newPriority -> onTaskChangePriority(task, newPriority) }
                                )
                            }
                        }
                    }
                }
            }

            // Katlanabilir Tamamlananlar Bölümü (Her sekmenin kendi tamamlananları)
            if (completedTasks.isNotEmpty()) {
                item(key = "section_completed_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { isCompletedExpanded = !isCompletedExpanded }
                            )
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(Res.string.tasks_hub_section_completed),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = completedTasks.size.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }

                        Icon(
                            imageVector = if (isCompletedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isCompletedExpanded) {
                    items(completedTasks, key = { "completed_${it.id}" }) { task ->
                        SwipeableTaskItem(
                            onSwipeRightToComplete = { onTaskToggleStatus(task) },
                            onSwipeLeftToDelete = { onTaskDelete(task.id) }
                        ) {
                            TasksHubCompletedCard(
                                task = task,
                                onTaskClick = { onTaskClick(task) },
                                onToggleStatus = { onTaskToggleStatus(task) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 2 Sekmeli Şık Segmented Kontrol.
 */
@Composable
private fun TasksHubTabSelector(
    selectedTab: TasksHubTab,
    onTabSelected: (TasksHubTab) -> Unit,
    backlogCount: Int,
    scheduledCount: Int,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Tab 0: Yapılacaklar Havuzu
            TasksHubTabPill(
                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                title = stringResource(Res.string.tasks_tab_backlog),
                count = backlogCount,
                isSelected = selectedTab == TasksHubTab.BACKLOG,
                onClick = { onTabSelected(TasksHubTab.BACKLOG) },
                modifier = Modifier.weight(1f)
            )

            // Tab 1: Planlanmış Görevler
            TasksHubTabPill(
                icon = Icons.Default.DateRange,
                title = stringResource(Res.string.tasks_tab_scheduled),
                count = scheduledCount,
                isSelected = selectedTab == TasksHubTab.SCHEDULED,
                onClick = { onTabSelected(TasksHubTab.SCHEDULED) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Tekil sekme kapsülü.
 */
@Composable
private fun TasksHubTabPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) primaryColor else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "tabBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 200),
        label = "tabContent"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (count > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        )
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else primaryColor
                    )
                }
            }
        }
    }
}

/**
 * Hızlı to-do ekleme çubuğu.
 */
@Composable
private fun QuickAddTaskBar(
    text: String,
    onTextChanged: (String) -> Unit,
    onAddClicked: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    GlassCard(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChanged,
                placeholder = {
                    Text(
                        text = stringResource(Res.string.tasks_hub_quick_add_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAddClicked() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            AnimatedVisibility(
                visible = text.isNotBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                IconButton(
                    onClick = onAddClicked,
                    modifier = Modifier
                        .bounceClick()
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(primaryColor)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(Res.string.tasks_hub_action_add),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Havuzdaki (Backlog) zamansız to-do kartı.
 * Öncelik değiştirme çipi, başa sabitleme yıldızı, ve yukarı/aşağı taşıma butonları içerir.
 */
@Composable
private fun TasksHubBacklogCard(
    task: TaskDto,
    isFirst: Boolean,
    isLast: Boolean,
    onTaskClick: () -> Unit,
    onToggleStatus: () -> Unit,
    onTogglePin: () -> Unit,
    onChangePriority: (TaskPriority) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val taskDetails = task.specificDetails as? ItemDetails.Task
    val priority = taskDetails?.priority ?: TaskPriority.MEDIUM

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        onClick = onTaskClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Yuvarlak Checkbox
            RoundCheckbox(
                isChecked = false,
                onCheckedChange = onToggleStatus
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Başlık, açıklama ve öncelik çipi
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (!task.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.description ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Tıklanabilir Öncelik Rozeti (Tıklandığında bir sonraki önceliğe döngü yapar)
                PriorityBadgeChip(
                    priority = priority,
                    onClick = {
                        val nextPriority = when (priority) {
                            TaskPriority.LOW -> TaskPriority.MEDIUM
                            TaskPriority.MEDIUM -> TaskPriority.HIGH
                            TaskPriority.HIGH -> TaskPriority.URGENT
                            TaskPriority.URGENT -> TaskPriority.LOW
                        }
                        onChangePriority(nextPriority)
                    }
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Eylem İkonları: Başa Sabitleme + Yukarı/Aşağı Taşıma
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Pin / Sabitleme Butonu
                IconButton(
                    onClick = onTogglePin,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (task.isPinned) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = stringResource(if (task.isPinned) Res.string.action_unpin else Res.string.action_pin),
                        tint = if (task.isPinned) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Sıralama Kontrolleri (Yukarı ve Aşağı)
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Yukarı Taşı
                    IconButton(
                        onClick = onMoveUp,
                        enabled = !isFirst,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = stringResource(Res.string.action_move_up),
                            tint = if (!isFirst) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Aşağı Taşı
                    IconButton(
                        onClick = onMoveDown,
                        enabled = !isLast,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(Res.string.action_move_down),
                            tint = if (!isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Planlanmış (Tarih ve Saatli) görev kartı.
 * Tarih rozeti, öncelik çipi ve sabitleme butonu içerir.
 */
@Composable
private fun TasksHubScheduledCard(
    task: TaskDto,
    onTaskClick: () -> Unit,
    onToggleStatus: () -> Unit,
    onTogglePin: () -> Unit,
    onChangePriority: (TaskPriority) -> Unit
) {
    val taskDetails = task.specificDetails as? ItemDetails.Task
    val priority = taskDetails?.priority ?: TaskPriority.MEDIUM
    val deadline = taskDetails?.deadline ?: task.startTime

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        onClick = onTaskClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Yuvarlak Checkbox
            RoundCheckbox(
                isChecked = false,
                onCheckedChange = onToggleStatus
            )

            Spacer(modifier = Modifier.width(12.dp))

            // İçerik
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (!task.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.description ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Tarih ve Saat Rozeti
                    Text(
                        text = "${formatShortDate(deadline)} ${formatTime(deadline)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    )

                    // Tıklanabilir Öncelik Rozeti
                    PriorityBadgeChip(
                        priority = priority,
                        onClick = {
                            val nextPriority = when (priority) {
                                TaskPriority.LOW -> TaskPriority.MEDIUM
                                TaskPriority.MEDIUM -> TaskPriority.HIGH
                                TaskPriority.HIGH -> TaskPriority.URGENT
                                TaskPriority.URGENT -> TaskPriority.LOW
                            }
                            onChangePriority(nextPriority)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Pin / Sabitleme Butonu
            IconButton(
                onClick = onTogglePin,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = if (task.isPinned) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = stringResource(if (task.isPinned) Res.string.action_unpin else Res.string.action_pin),
                    tint = if (task.isPinned) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Tamamlanmış görev kartı.
 */
@Composable
private fun TasksHubCompletedCard(
    task: TaskDto,
    onTaskClick: () -> Unit,
    onToggleStatus: () -> Unit
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = 0.65f,
        animationSpec = tween(durationMillis = 200),
        label = "completedAlpha"
    )

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = animatedAlpha },
        shape = RoundedCornerShape(16.dp),
        onClick = onTaskClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundCheckbox(
                isChecked = true,
                onCheckedChange = onToggleStatus
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.LineThrough),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Tıklanabilir Öncelik Rozeti.
 * Tıklandığında kullanıcı hızlıca öncelik döngüsü yapabilir.
 */
@Composable
private fun PriorityBadgeChip(
    priority: TaskPriority,
    onClick: () -> Unit
) {
    val (priorityColor, priorityTextRes) = when (priority) {
        TaskPriority.LOW -> Color(0xFF64748B) to Res.string.priority_low
        TaskPriority.MEDIUM -> Color(0xFF3B82F6) to Res.string.priority_medium
        TaskPriority.HIGH -> Color(0xFFF59E0B) to Res.string.priority_high
        TaskPriority.URGENT -> Color(0xFFEF4444) to Res.string.priority_urgent
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(priorityColor.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(priorityTextRes),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = priorityColor
        )
    }
}

/**
 * Yuvarlak, animasyonlu onay kutusu (Checkbox) bileşeni.
 */
@Composable
private fun RoundCheckbox(
    isChecked: Boolean,
    onCheckedChange: () -> Unit
) {
    val checkBgColor by animateColorAsState(
        targetValue = if (isChecked) Color(0xFF10B981) else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "checkboxBg"
    )

    val checkBorderColor by animateColorAsState(
        targetValue = if (isChecked) Color(0xFF10B981) else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
        animationSpec = tween(durationMillis = 200),
        label = "checkboxBorder"
    )

    Box(
        modifier = Modifier
            .size(24.dp)
            .bounceClick()
            .clip(CircleShape)
            .background(checkBgColor)
            .border(2.dp, checkBorderColor, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCheckedChange
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isChecked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

/**
 * Boş durum ipucu metni.
 */
@Composable
private fun TasksHubEmptyHint(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}
