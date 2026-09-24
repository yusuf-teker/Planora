package com.yusufteker.planora.feature.home.presentation.analytics

import app.cash.turbine.test
import com.yusufteker.planora.feature.home.data.repository.FakePlanRepository
import com.yusufteker.planora.feature.home.util.FakeSessionPreferences
import com.yusufteker.planora.shared.api.ItemDetails
import com.yusufteker.planora.shared.api.TaskDto
import com.yusufteker.planora.shared.api.TaskPriority
import com.yusufteker.planora.shared.api.TaskStatus
import com.yusufteker.planora.shared.api.TaskType
import com.yusufteker.planora.shared.api.TaskVisibility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * AnalyticsViewModel Unit Test Paketi.
 *
 * Görev tamamlama oranları, haftalık verimlilik dağılımı,
 * streak hesaplaması ve Premium durumunun yansımasını doğrular.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var planRepository: FakePlanRepository
    private lateinit var sessionPreferences: FakeSessionPreferences

    private val fixedNowMs = 1_700_000_000_000L
    private val oneDayMs = 24L * 60L * 60L * 1000L

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        planRepository = FakePlanRepository()
        sessionPreferences = FakeSessionPreferences()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): AnalyticsViewModel {
        return AnalyticsViewModel(
            planRepository = planRepository,
            sessionPreferences = sessionPreferences,
            timeProvider = { fixedNowMs }
        )
    }

    private fun createDummyTask(
        id: String,
        status: TaskStatus,
        dateMs: Long = fixedNowMs,
        priority: TaskPriority = TaskPriority.MEDIUM
    ): TaskDto {
        return TaskDto(
            id = id,
            creatorId = 1,
            title = "Task $id",
            description = null,
            startTime = dateMs,
            endTime = dateMs + 3600_000L,
            type = TaskType.TASK,
            status = status,
            visibility = TaskVisibility.PRIVATE,
            sharedRoomIds = emptyList(),
            specificDetails = ItemDetails.Task(priority = priority),
            isSynced = true
        )
    }

    @Test
    fun `varsayilan zaman saglayici ile AnalyticsViewModel basariyla olusturulur`() {
        val vm = AnalyticsViewModel(
            planRepository = planRepository,
            sessionPreferences = sessionPreferences
        )
        // Default timeProvider ile başarıyla oluşturulduğunu doğrular
        assertTrue(vm.state.value.isLoading)
    }

    @Test
    fun `bos gorev listesinde sifir istatistik hesaplanir`() = runTest {
        planRepository.seedTasks(emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(0, state.totalTasks)
        assertEquals(0, state.completedTasks)
        assertEquals(0f, state.completionRate)
        assertEquals(0, state.currentStreakDays)
    }

    @Test
    fun `tamamlanan gorevler ve basari orani dogru hesaplanir`() = runTest {
        // Arrange: 4 görevden 3'ü COMPLETED, 1'i IN_PROGRESS
        val tasks = listOf(
            createDummyTask("1", TaskStatus.COMPLETED, fixedNowMs, TaskPriority.HIGH),
            createDummyTask("2", TaskStatus.COMPLETED, fixedNowMs, TaskPriority.HIGH),
            createDummyTask("3", TaskStatus.COMPLETED, fixedNowMs, TaskPriority.MEDIUM),
            createDummyTask("4", TaskStatus.IN_PROGRESS, fixedNowMs, TaskPriority.LOW)
        )
        planRepository.seedTasks(tasks)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(4, state.totalTasks)
        assertEquals(3, state.completedTasks)
        assertEquals(0.75f, state.completionRate)
        assertEquals(2, state.priorityBreakdown[TaskPriority.HIGH])
        assertEquals(1, state.priorityBreakdown[TaskPriority.MEDIUM])
        assertEquals(1, state.priorityBreakdown[TaskPriority.LOW])
    }

    @Test
    fun `ardisik gunlerde tamamlanan gorevler streak sayisini artirir`() = runTest {
        // Bugün ve dün tamamlanan 1'er görev
        val taskToday = createDummyTask("1", TaskStatus.COMPLETED, dateMs = fixedNowMs)
        val taskYesterday = createDummyTask("2", TaskStatus.COMPLETED, dateMs = fixedNowMs - oneDayMs)

        planRepository.seedTasks(listOf(taskToday, taskYesterday))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.currentStreakDays)
    }

    @Test
    fun `UpgradeClicked olayi NavigateToPremium efektini tetikler`() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.onEvent(AnalyticsEvent.UpgradeClicked)
            val effect = awaitItem()
            assertEquals(AnalyticsEffect.NavigateToPremium, effect)
        }
    }
}
