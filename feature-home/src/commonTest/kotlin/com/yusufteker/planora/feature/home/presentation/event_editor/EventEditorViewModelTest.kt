package com.yusufteker.planora.feature.home.presentation.event_editor

import app.cash.turbine.test
import com.yusufteker.planora.feature.home.data.repository.FakePlanRepository
import com.yusufteker.planora.feature.home.data.repository.FakeProfileRepository
import com.yusufteker.planora.feature.home.util.FakeSessionPreferences
import com.yusufteker.planora.shared.api.CreatePlanRoomRequest
import com.yusufteker.planora.shared.api.TaskType
import com.yusufteker.planora.shared.api.TaskVisibility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [EventEditorViewModel] verifying Plan Room loading, selection,
 * and persistence of room-shared vs. personal events.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EventEditorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakePlanRepository: FakePlanRepository
    private lateinit var fakeProfileRepository: FakeProfileRepository
    private lateinit var fakeSessionPreferences: FakeSessionPreferences

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakePlanRepository = FakePlanRepository()
        fakeProfileRepository = FakeProfileRepository()
        fakeSessionPreferences = FakeSessionPreferences()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): EventEditorViewModel {
        return EventEditorViewModel(
            planRepository = fakePlanRepository,
            profileRepository = fakeProfileRepository,
            sessionPreferences = fakeSessionPreferences
        )
    }

    @Test
    fun `yeni etkinlik yuklendiginde availableRooms korunur ve plan odasi secilip kaydedilebilir`() = runTest {
        val room = fakePlanRepository.createPlanRoom(CreatePlanRoomRequest("Etkinlik Odası")).getOrThrow()

        val viewModel = createViewModel()
        runCurrent()

        viewModel.onEvent(EventEditorEvent.OnLoadEvent(eventId = null))
        runCurrent()

        assertEquals(1, viewModel.state.value.availableRooms.size)
        assertEquals(room.id, viewModel.state.value.availableRooms.first().id)

        viewModel.onEvent(EventEditorEvent.OnRoomSelected(room.id))
        runCurrent()

        assertEquals(room.id, viewModel.state.value.planRoomId)
        assertEquals("Etkinlik Odası", viewModel.state.value.planRoomName)

        viewModel.onEvent(EventEditorEvent.OnTitleChange("Oda Toplantısı"))
        viewModel.effect.test {
            viewModel.onEvent(EventEditorEvent.OnSaveClick)
            advanceUntilIdle()
            assertEquals(EventEditorEffect.NavigateBack, awaitItem())
        }

        val savedEvent = fakePlanRepository.getStoredTasks().find { it.title == "Oda Toplantısı" }
        assertTrue(savedEvent != null)
        assertEquals(TaskType.EVENT, savedEvent.type)
        assertEquals(TaskVisibility.ROOM_SHARED, savedEvent.visibility)
        assertEquals(listOf(room.id), savedEvent.sharedRoomIds)
    }

    @Test
    fun `plan odasi secimi kaldirildiginda kisisel etkinlik olarak kaydedilir`() = runTest {
        val room = fakePlanRepository.createPlanRoom(CreatePlanRoomRequest("Takım Odası")).getOrThrow()

        val viewModel = createViewModel()
        runCurrent()

        viewModel.onEvent(EventEditorEvent.OnLoadEvent(eventId = null))
        runCurrent()

        viewModel.onEvent(EventEditorEvent.OnRoomSelected(room.id))
        runCurrent()
        assertEquals(room.id, viewModel.state.value.planRoomId)

        viewModel.onEvent(EventEditorEvent.OnRoomSelected(null))
        runCurrent()

        assertNull(viewModel.state.value.planRoomId)
        assertNull(viewModel.state.value.planRoomName)
        assertTrue(viewModel.state.value.participants.isEmpty())

        viewModel.onEvent(EventEditorEvent.OnTitleChange("Kişisel Etkinlik"))
        viewModel.effect.test {
            viewModel.onEvent(EventEditorEvent.OnSaveClick)
            advanceUntilIdle()
            assertEquals(EventEditorEffect.NavigateBack, awaitItem())
        }

        val savedEvent = fakePlanRepository.getStoredTasks().find { it.title == "Kişisel Etkinlik" }
        assertTrue(savedEvent != null)
        assertEquals(TaskType.EVENT, savedEvent.type)
        assertEquals(TaskVisibility.PRIVATE, savedEvent.visibility)
        assertTrue(savedEvent.sharedRoomIds.isEmpty())
    }
}
