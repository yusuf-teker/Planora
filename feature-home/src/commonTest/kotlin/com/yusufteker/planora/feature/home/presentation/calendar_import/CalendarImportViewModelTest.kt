package com.yusufteker.planora.feature.home.presentation.calendar_import

import app.cash.turbine.test
import com.yusufteker.planora.core.calendar.CalendarImportDateRange
import com.yusufteker.planora.core.calendar.CalendarImportItem
import com.yusufteker.planora.core.calendar.CalendarService
import com.yusufteker.planora.feature.home.data.repository.FakePlanRepository
import com.yusufteker.planora.shared.api.ItemDetails
import com.yusufteker.planora.shared.api.TaskPriority
import com.yusufteker.planora.shared.api.TaskType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Fake implementation of [CalendarService] for unit testing native calendar sync & import operations.
 */
class FakeCalendarService : CalendarService {
    var hasPermission: Boolean = true
    var eventsToReturn: List<CalendarImportItem> = emptyList()
    var lastQueriedRange: Pair<Long, Long>? = null

    override fun hasCalendarReadPermission(): Boolean = hasPermission

    override suspend fun fetchCalendarEvents(startEpochMs: Long, endEpochMs: Long): List<CalendarImportItem> {
        lastQueriedRange = Pair(startEpochMs, endEpochMs)
        return eventsToReturn
    }

    override fun addToSystemCalendar(
        title: String,
        description: String?,
        location: String?,
        startTimeEpochMillis: Long,
        endTimeEpochMillis: Long?
    ): Boolean {
        return true
    }
}

/**
 * Unit test suite for [CalendarImportViewModel].
 *
 * Verifies:
 * 1. Permission checks and automatic/manual loading of native calendar events.
 * 2. Event selection and bulk select-all behaviors (including calendar filter constraints).
 * 3. Switching between [TaskType.EVENT] and [TaskType.TASK].
 * 4. Editing item details (title, description, location, priority) prior to import.
 * 5. Batch import ensuring Offline-First persistence and background synchronization.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CalendarImportViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeCalendarService: FakeCalendarService
    private lateinit var fakePlanRepository: FakePlanRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeCalendarService = FakeCalendarService()
        fakePlanRepository = FakePlanRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): CalendarImportViewModel {
        return CalendarImportViewModel(
            calendarService = fakeCalendarService,
            planRepository = fakePlanRepository
        )
    }

    private fun createSampleItem(
        id: String = "1",
        title: String = "Team Meeting",
        calendarName: String = "Work Calendar",
        targetType: TaskType = TaskType.EVENT,
        startTime: Long = 1774435200000L,
        endTime: Long = 1774438800000L,
        location: String? = "Meeting Room A",
        isSelected: Boolean = true
    ): CalendarImportItem {
        return CalendarImportItem(
            id = id,
            calendarName = calendarName,
            title = title,
            description = "Quarterly review",
            startTimeEpochMillis = startTime,
            endTimeEpochMillis = endTime,
            isAllDay = false,
            location = location,
            targetType = targetType,
            priority = TaskPriority.HIGH,
            isSelected = isSelected
        )
    }

    @Test
    fun `izin mevcut oldugunda baslangicta etkinlikler otomatik yuklenir`() = runTest {
        fakeCalendarService.hasPermission = true
        fakeCalendarService.eventsToReturn = listOf(
            createSampleItem(id = "1", calendarName = "Google"),
            createSampleItem(id = "2", calendarName = "Personal")
        )

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.hasPermission)
        assertFalse(state.isLoading)
        assertEquals(2, state.events.size)
        assertEquals(listOf("Google", "Personal"), state.availableCalendars)
    }

    @Test
    fun `izin olmadiginda etkinlikler yuklenmez ve izin bekleme durumu set edilir`() = runTest {
        fakeCalendarService.hasPermission = false

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.hasPermission)
        assertTrue(state.events.isEmpty())
    }

    @Test
    fun `kullanici izin verdiginde etkinlikler basariyla cekilmelidir`() = runTest {
        fakeCalendarService.hasPermission = false
        fakeCalendarService.eventsToReturn = listOf(createSampleItem(id = "10"))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.state.value.hasPermission)

        viewModel.onEvent(CalendarImportUiEvent.PermissionResult(isGranted = true))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.hasPermission)
        assertEquals(1, viewModel.state.value.events.size)
    }

    @Test
    fun `kullanici izin reddettiginde isPermissionDenied true olmalidir`() = runTest {
        fakeCalendarService.hasPermission = false
        val viewModel = createViewModel()

        viewModel.onEvent(CalendarImportUiEvent.PermissionResult(isGranted = false))
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.hasPermission)
        assertTrue(viewModel.state.value.isPermissionDenied)
    }

    @Test
    fun `tarih araligi degistirildiginde etkinlikler yeni aralik icin tekrar yuklenmelidir`() = runTest {
        fakeCalendarService.hasPermission = true
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(CalendarImportUiEvent.SelectDateRange(CalendarImportDateRange.NEXT_3_MONTHS))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(CalendarImportDateRange.NEXT_3_MONTHS, viewModel.state.value.selectedDateRange)
        assertNotNull(fakeCalendarService.lastQueriedRange)
    }

    @Test
    fun `etkinlik secim durumu toggle edilebilmelidir`() = runTest {
        fakeCalendarService.eventsToReturn = listOf(createSampleItem(id = "1", isSelected = true))
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.events.first().isSelected)

        viewModel.onEvent(CalendarImportUiEvent.ToggleEventSelection("1"))
        assertFalse(viewModel.state.value.events.first().isSelected)

        viewModel.onEvent(CalendarImportUiEvent.ToggleEventSelection("1"))
        assertTrue(viewModel.state.value.events.first().isSelected)
    }

    @Test
    fun `tumunu sec ve kaldir aksiyonu secili filtreye gore calismalidir`() = runTest {
        fakeCalendarService.eventsToReturn = listOf(
            createSampleItem(id = "1", calendarName = "Google", isSelected = true),
            createSampleItem(id = "2", calendarName = "Apple", isSelected = true)
        )
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Remove all
        viewModel.onEvent(CalendarImportUiEvent.ToggleSelectAll(selectAll = false))
        assertTrue(viewModel.state.value.events.none { it.isSelected })

        // Select all
        viewModel.onEvent(CalendarImportUiEvent.ToggleSelectAll(selectAll = true))
        assertTrue(viewModel.state.value.events.all { it.isSelected })

        // Filter to Google calendar and unselect all Google only
        viewModel.onEvent(CalendarImportUiEvent.SelectCalendarFilter("Google"))
        viewModel.onEvent(CalendarImportUiEvent.ToggleSelectAll(selectAll = false))

        val googleItem = viewModel.state.value.events.first { it.id == "1" }
        val appleItem = viewModel.state.value.events.first { it.id == "2" }
        assertFalse(googleItem.isSelected)
        assertTrue(appleItem.isSelected)
    }

    @Test
    fun `etkinlik ile gorev arasinda hedef tipi donusturulebilmelidir`() = runTest {
        fakeCalendarService.eventsToReturn = listOf(createSampleItem(id = "1", targetType = TaskType.EVENT))
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(TaskType.EVENT, viewModel.state.value.events.first().targetType)

        viewModel.onEvent(CalendarImportUiEvent.ToggleTargetType("1"))
        assertEquals(TaskType.TASK, viewModel.state.value.events.first().targetType)

        viewModel.onEvent(CalendarImportUiEvent.ToggleTargetType("1"))
        assertEquals(TaskType.EVENT, viewModel.state.value.events.first().targetType)
    }

    @Test
    fun `etkinlik duzenleme modalinda baslik ve detaylar guncellenebilmelidir`() = runTest {
        val originalItem = createSampleItem(id = "1", title = "Original Title")
        fakeCalendarService.eventsToReturn = listOf(originalItem)
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(CalendarImportUiEvent.StartEditItem(originalItem))
        assertEquals(originalItem, viewModel.state.value.editingItem)

        val updatedItem = originalItem.copy(
            title = "Updated Title",
            location = "New Conference Room",
            priority = TaskPriority.URGENT
        )
        viewModel.onEvent(CalendarImportUiEvent.SaveEditedItem(updatedItem))

        assertNull(viewModel.state.value.editingItem)
        val storedItem = viewModel.state.value.events.first { it.id == "1" }
        assertEquals("Updated Title", storedItem.title)
        assertEquals("New Conference Room", storedItem.location)
        assertEquals(TaskPriority.URGENT, storedItem.priority)
    }

    @Test
    fun `secili etkinlikler basariyla yerel veritabanina kaydedilip senkronize edilmelidir`() = runTest {
        fakeCalendarService.eventsToReturn = listOf(
            createSampleItem(id = "1", title = "Event 1", targetType = TaskType.EVENT, isSelected = true),
            createSampleItem(id = "2", title = "Task 2", targetType = TaskType.TASK, isSelected = true),
            createSampleItem(id = "3", title = "Event 3", isSelected = false)
        )
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onEvent(CalendarImportUiEvent.ImportSelectedEvents)
            testDispatcher.scheduler.advanceUntilIdle()

            // Verify stored items in fake repository (Offline-First)
            val stored = fakePlanRepository.getStoredTasks()
            assertEquals(2, stored.size)

            val eventTask = stored.first { it.title == "Event 1" }
            assertEquals(TaskType.EVENT, eventTask.type)
            assertTrue(eventTask.specificDetails is ItemDetails.Event)

            val normalTask = stored.first { it.title == "Task 2" }
            assertEquals(TaskType.TASK, normalTask.type)
            assertTrue(normalTask.specificDetails is ItemDetails.Task)

            // Verify effects emitted: Snackbar & NavigateBack
            val snackbarEffect = awaitItem() as CalendarImportEffect.ShowSnackbar
            assertEquals(2, snackbarEffect.count)

            val navEffect = awaitItem() as CalendarImportEffect.NavigateBack
            assertNotNull(navEffect)
        }
    }

    @Test
    fun `hicbir etkinlik secilmemisken ice aktarma tiklandiginda islem yapilmamalidir`() = runTest {
        fakeCalendarService.eventsToReturn = listOf(
            createSampleItem(id = "1", isSelected = false)
        )
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(CalendarImportUiEvent.ImportSelectedEvents)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakePlanRepository.getStoredTasks().isEmpty())
    }
}
