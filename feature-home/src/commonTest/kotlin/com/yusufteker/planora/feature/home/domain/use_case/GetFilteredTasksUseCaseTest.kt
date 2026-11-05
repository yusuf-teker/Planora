package com.yusufteker.planora.feature.home.domain.use_case

import com.yusufteker.planora.core.utils.TimelineTypeFilter
import com.yusufteker.planora.core.utils.TimelineViewOption
import com.yusufteker.planora.core.utils.getCurrentTimeMs
import com.yusufteker.planora.feature.home.presentation.home.TimelineFilterOptions
import com.yusufteker.planora.shared.api.ItemDetails
import com.yusufteker.planora.shared.api.TaskDto
import com.yusufteker.planora.shared.api.TaskPriority
import com.yusufteker.planora.shared.api.TaskStatus
import com.yusufteker.planora.shared.api.TaskType
import com.yusufteker.planora.shared.api.TaskVisibility
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [GetFilteredTasksUseCase] sınıfının tekrarlayan görevler, "Sadece Yaklaşan Tekrarlar"
 * (showOnlyNextRecurring) filtresi, takvim görünümü ve tamamlanma durumlarına göre filtreleme
 * davranışlarını doğrulayan birim test sınıfı.
 */
class GetFilteredTasksUseCaseTest {

    private lateinit var useCase: GetFilteredTasksUseCase
    private val timeZone = TimeZone.currentSystemDefault()

    @BeforeTest
    fun setUp() {
        useCase = GetFilteredTasksUseCase()
    }

    private fun createTask(
        id: String,
        startTime: Long,
        isRecurring: Boolean = false,
        status: TaskStatus = TaskStatus.PENDING,
        deadline: Long? = startTime
    ): TaskDto {
        return TaskDto(
            id = id,
            creatorId = 1,
            title = "Task $id",
            description = null,
            startTime = startTime,
            endTime = null,
            type = TaskType.TASK,
            status = status,
            visibility = TaskVisibility.PRIVATE,
            isRecurring = isRecurring,
            recurrenceRule = if (isRecurring) """{"type":"daily","interval":1}""" else null,
            isFlexible = false,
            isOptional = false,
            isPostponable = false,
            isAllDay = false,
            aiMetadata = null,
            reminders = emptyList(),
            specificDetails = ItemDetails.Task(
                subtasks = emptyList(),
                priority = TaskPriority.MEDIUM,
                deadline = deadline
            ),
            tags = emptyList(),
            color = null,
            parentId = null,
            participants = emptyList(),
            sharedRoomIds = emptyList(),
            isPinned = false,
            isSynced = true
        )
    }

    @Test
    fun testRecurringDailyTaskCreatedInPast_showsTodayInstance_notOldInstance() {
        val nowMs = getCurrentTimeMs()
        val oneDayMs = 86_400_000L

        val twoDaysAgoMs = nowMs - (oneDayMs * 2)
        val yesterdayMs = nowMs - oneDayMs
        val todayMs = nowMs
        val tomorrowMs = nowMs + oneDayMs

        val baseId = "weight_measurement"
        val taskPast2 = createTask(id = "${baseId}_$twoDaysAgoMs", startTime = twoDaysAgoMs, isRecurring = true)
        val taskPast1 = createTask(id = "${baseId}_$yesterdayMs", startTime = yesterdayMs, isRecurring = true)
        val taskToday = createTask(id = "${baseId}_$todayMs", startTime = todayMs, isRecurring = true)
        val taskTomorrow = createTask(id = "${baseId}_$tomorrowMs", startTime = tomorrowMs, isRecurring = true)

        val allOccurrences = listOf(taskPast2, taskPast1, taskToday, taskTomorrow)

        // Sadece Yaklaşan Tekrarlar (showOnlyNextRecurring = true) ve Tamamlananları Gösterme (showCompleted = false)
        val options = TimelineFilterOptions(
            showOnlyNextRecurring = true,
            showCompleted = false,
            showRoomTasks = true
        )

        val result = useCase(
            myTasks = allOccurrences,
            sharedTasksMap = emptyMap(),
            selectedUsers = emptySet(),
            options = options,
            viewOption = TimelineViewOption.DATE,
            selectedCalendarDate = null,
            visibleCalendarMonth = null
        )

        // Eski (2 gün önceki veya dünkü) kopya yerine BUGÜNKÜ aktif kopya gelmeli
        assertEquals(1, result.size)
        assertEquals(taskToday.id, result.first().id)
    }

    @Test
    fun testRecurringDailyTask_whenTodayCompleted_andShowCompletedFalse_showsTomorrowInstance() {
        val nowMs = getCurrentTimeMs()
        val oneDayMs = 86_400_000L

        val yesterdayMs = nowMs - oneDayMs
        val todayMs = nowMs
        val tomorrowMs = nowMs + oneDayMs

        val baseId = "daily_habit"
        val taskYesterday = createTask(id = "${baseId}_$yesterdayMs", startTime = yesterdayMs, isRecurring = true, status = TaskStatus.COMPLETED)
        val taskToday = createTask(id = "${baseId}_$todayMs", startTime = todayMs, isRecurring = true, status = TaskStatus.COMPLETED)
        val taskTomorrow = createTask(id = "${baseId}_$tomorrowMs", startTime = tomorrowMs, isRecurring = true, status = TaskStatus.PENDING)

        val allOccurrences = listOf(taskYesterday, taskToday, taskTomorrow)

        val options = TimelineFilterOptions(
            showOnlyNextRecurring = true,
            showCompleted = false,
            showRoomTasks = true
        )

        val result = useCase(
            myTasks = allOccurrences,
            sharedTasksMap = emptyMap(),
            selectedUsers = emptySet(),
            options = options,
            viewOption = TimelineViewOption.DATE,
            selectedCalendarDate = null,
            visibleCalendarMonth = null
        )

        // Bugün tamamlandığı için sıradaki yaklaşan kopya YARIN olmalı
        assertEquals(1, result.size)
        assertEquals(taskTomorrow.id, result.first().id)
    }

    @Test
    fun testRecurringDailyTask_whenTodayCompleted_andShowCompletedTrue_showsTodayAndTomorrow() {
        val nowMs = getCurrentTimeMs()
        val oneDayMs = 86_400_000L

        val todayMs = nowMs
        val tomorrowMs = nowMs + oneDayMs

        val baseId = "daily_habit"
        val taskToday = createTask(id = "${baseId}_$todayMs", startTime = todayMs, isRecurring = true, status = TaskStatus.COMPLETED)
        val taskTomorrow = createTask(id = "${baseId}_$tomorrowMs", startTime = tomorrowMs, isRecurring = true, status = TaskStatus.PENDING)

        val allOccurrences = listOf(taskToday, taskTomorrow)

        val options = TimelineFilterOptions(
            showOnlyNextRecurring = true,
            showCompleted = true,
            showRoomTasks = true
        )

        val result = useCase(
            myTasks = allOccurrences,
            sharedTasksMap = emptyMap(),
            selectedUsers = emptySet(),
            options = options,
            viewOption = TimelineViewOption.DATE,
            selectedCalendarDate = null,
            visibleCalendarMonth = null
        )

        // Tamamlananlar açık olduğunda hem bugünkü tamamlanan hem de yarınki sıradaki kopya listelenmeli
        assertEquals(2, result.size)
        assertTrue(result.any { it.id == taskToday.id })
        assertTrue(result.any { it.id == taskTomorrow.id })
    }

    @Test
    fun testCalendarViewOption_doesNotFilterOutFutureDates() {
        val nowMs = getCurrentTimeMs()
        val oneDayMs = 86_400_000L

        val tomorrowMs = nowMs + oneDayMs
        val tomorrowDate = Instant.fromEpochMilliseconds(tomorrowMs).toLocalDateTime(timeZone).date

        val baseId = "meeting"
        val taskToday = createTask(id = "${baseId}_$nowMs", startTime = nowMs, isRecurring = true)
        val taskTomorrow = createTask(id = "${baseId}_$tomorrowMs", startTime = tomorrowMs, isRecurring = true)

        val allOccurrences = listOf(taskToday, taskTomorrow)

        val options = TimelineFilterOptions(
            showOnlyNextRecurring = true,
            showCompleted = true,
            showRoomTasks = true
        )

        // Takvimde YARIN seçildiğinde, yarınki tekrar gösterilmeli (showOnlyNextRecurring yüzünden gizlenmemeli)
        val result = useCase(
            myTasks = allOccurrences,
            sharedTasksMap = emptyMap(),
            selectedUsers = emptySet(),
            options = options,
            viewOption = TimelineViewOption.CALENDAR,
            selectedCalendarDate = tomorrowDate,
            visibleCalendarMonth = null
        )

        assertEquals(1, result.size)
        assertEquals(taskTomorrow.id, result.first().id)
    }
}

