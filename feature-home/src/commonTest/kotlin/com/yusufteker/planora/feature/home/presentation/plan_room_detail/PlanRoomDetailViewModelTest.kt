package com.yusufteker.planora.feature.home.presentation.plan_room_detail

import com.yusufteker.planora.feature.home.data.repository.FakePlanRepository
import com.yusufteker.planora.feature.home.data.repository.FakeProfileRepository
import com.yusufteker.planora.feature.home.util.FakeSessionPreferences
import com.yusufteker.planora.shared.api.ItemDetails
import com.yusufteker.planora.shared.api.PlanRoomDto
import com.yusufteker.planora.shared.api.TaskDto
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for PlanRoomDetailViewModel covering room to-dos and tab navigation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlanRoomDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var planRepository: FakePlanRepository
    private lateinit var profileRepository: FakeProfileRepository
    private lateinit var sessionPreferences: FakeSessionPreferences

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        planRepository = FakePlanRepository()
        profileRepository = FakeProfileRepository()
        sessionPreferences = FakeSessionPreferences()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): PlanRoomDetailViewModel {
        return PlanRoomDetailViewModel(
            planRepository = planRepository,
            profileRepository = profileRepository,
            sessionPreferences = sessionPreferences
        )
    }

    @Test
    fun `OnTabSelected updates selectedTab to TODOS`() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(PlanRoomDetailEvent.OnTabSelected(RoomDetailTab.TODOS))
        advanceUntilIdle()

        assertEquals(RoomDetailTab.TODOS, viewModel.state.value.selectedTab)
    }

    @Test
    fun `OnQuickCreateTask creates an unscheduled todo for the room`() = runTest {
        val roomId = "room_123"
        planRepository.seedRooms(
            listOf(
                PlanRoomDto(
                    id = roomId,
                    name = "Test Room",
                    creatorId = 1,
                    createdAt = 1000L,
                    members = emptyList()
                )
            )
        )
        
        val viewModel = createViewModel()
        viewModel.onEvent(PlanRoomDetailEvent.LoadRoom(roomId))
        advanceUntilIdle()

        viewModel.onEvent(PlanRoomDetailEvent.OnQuickCreateTask("Buy groceries for event"))
        advanceUntilIdle()

        val allTasks = planRepository.getStoredTasks()
        val createdTask = allTasks.find { it.title == "Buy groceries for event" }

        assertTrue(createdTask != null, "Task should be created in repository")
        assertEquals(TaskType.TASK, createdTask.type)
        assertEquals(TaskVisibility.ROOM_SHARED, createdTask.visibility)
        assertTrue(createdTask.sharedRoomIds.contains(roomId))

        val taskDetails = createdTask.specificDetails as? ItemDetails.Task
        assertTrue(taskDetails != null)
        assertNull(taskDetails.deadline, "Quick room todo must be unscheduled (no deadline)")
    }

    @Test
    fun `OnToggleTaskStatus toggles completion status via completeTaskInstance`() = runTest {
        val task = TaskDto(
            id = "todo-101",
            title = "Room Todo Item",
            description = null,
            startTime = 1000L,
            endTime = null,
            type = TaskType.TASK,
            status = TaskStatus.PENDING,
            visibility = TaskVisibility.ROOM_SHARED,
            sharedRoomIds = listOf("room_123"),
            isRecurring = false,
            recurrenceRule = null,
            isFlexible = true,
            isOptional = false,
            isPostponable = true,
            isAllDay = false,
            reminders = emptyList(),
            participants = emptyList(),
            specificDetails = ItemDetails.Task(deadline = null),
            parentId = null,
            creatorId = 1,
            isSynced = true
        )
        planRepository.seedTasks(listOf(task))

        val viewModel = createViewModel()
        viewModel.onEvent(PlanRoomDetailEvent.LoadRoom("room_123"))
        advanceUntilIdle()

        viewModel.onEvent(PlanRoomDetailEvent.OnToggleTaskStatus(task))
        advanceUntilIdle()

        val updatedTask = planRepository.getStoredTasks().find { it.id == "todo-101" }
        assertEquals(TaskStatus.COMPLETED, updatedTask?.status)
    }
}
