package com.yusufteker.planora.feature.home.presentation.task_detail

import app.cash.turbine.test
import com.yusufteker.planora.feature.home.data.repository.FakePlanRepository
import com.yusufteker.planora.feature.home.data.repository.FakeProfileRepository
import com.yusufteker.planora.feature.home.util.FakeSessionPreferences
import com.yusufteker.planora.shared.api.CreateTaskRequest
import com.yusufteker.planora.shared.api.TaskType
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
import kotlin.test.assertTrue

/**
 * Unit tests for [TaskDetailViewModel].
 *
 * Verifies:
 * 1. Adding a quick note saves note under parent task and emits a snackbar effect.
 * 2. Empty quick note is ignored.
 * 3. Deleting an attached note removes it and emits a snackbar effect.
 * 4. Focus click triggers NavigateToFocus effect.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TaskDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakePlanRepository: FakePlanRepository
    private lateinit var fakeProfileRepository: FakeProfileRepository
    private lateinit var fakeSessionPreferences: FakeSessionPreferences
    private lateinit var viewModel: TaskDetailViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakePlanRepository = FakePlanRepository()
        fakeProfileRepository = FakeProfileRepository()
        fakeSessionPreferences = FakeSessionPreferences()
        viewModel = TaskDetailViewModel(
            planRepository = fakePlanRepository,
            profileRepository = fakeProfileRepository,
            sessionPreferences = fakeSessionPreferences
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun addQuickNote_createsNoteWithParentIdAndEmitsSnackbar() = runTest {
        // Given a loaded task
        val taskResult = fakePlanRepository.createTask(
            CreateTaskRequest(
                title = "Ana Görev",
                startTime = 1000L,
                type = TaskType.TASK
            )
        )
        val taskId = taskResult.getOrThrow().id

        viewModel.onEvent(TaskDetailEvent.OnLoadTask(taskId = taskId))
        advanceUntilIdle()

        viewModel.effect.test {
            // When quick note is added
            viewModel.onEvent(TaskDetailEvent.OnAddQuickNote("Hızlı Not İçeriği"))
            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is TaskDetailEffect.ShowSnackbar)
        }

        // Verify note is created in repository with parentId
        advanceUntilIdle()
        fakePlanRepository.observeAllTasks().test {
            val allTasks = awaitItem()
            val createdNote = allTasks.find { it.parentId == taskId && it.type == TaskType.NOTE }
            assertTrue(createdNote != null)
            assertEquals("Hızlı Not İçeriği", createdNote.title)
        }
    }

    @Test
    fun addQuickNote_blankContent_doesNotCreateNote() = runTest {
        val taskResult = fakePlanRepository.createTask(
            CreateTaskRequest(
                title = "Görev",
                startTime = 1000L,
                type = TaskType.TASK
            )
        )
        val taskId = taskResult.getOrThrow().id
        viewModel.onEvent(TaskDetailEvent.OnLoadTask(taskId = taskId))
        advanceUntilIdle()

        viewModel.onEvent(TaskDetailEvent.OnAddQuickNote("   "))
        advanceUntilIdle()

        fakePlanRepository.observeAllTasks().test {
            val allTasks = awaitItem()
            val notes = allTasks.filter { it.type == TaskType.NOTE }
            assertTrue(notes.isEmpty())
        }
    }

    @Test
    fun deleteNote_removesNoteAndEmitsSnackbar() = runTest {
        val noteResult = fakePlanRepository.createTask(
            CreateTaskRequest(
                title = "Silinecek Not",
                startTime = 1000L,
                type = TaskType.NOTE
            )
        )
        val noteId = noteResult.getOrThrow().id

        viewModel.effect.test {
            viewModel.onEvent(TaskDetailEvent.OnDeleteNote(noteId))
            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is TaskDetailEffect.ShowSnackbar)
        }

        fakePlanRepository.observeAllTasks().test {
            val allTasks = awaitItem()
            assertTrue(allTasks.none { it.id == noteId })
        }
    }

    @Test
    fun focusClick_emitsNavigateToFocusEffect() = runTest {
        val taskResult = fakePlanRepository.createTask(
            CreateTaskRequest(
                title = "Odaklanılacak Görev",
                startTime = 1000L,
                type = TaskType.TASK
            )
        )
        val taskId = taskResult.getOrThrow().id
        viewModel.onEvent(TaskDetailEvent.OnLoadTask(taskId = taskId))
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onEvent(TaskDetailEvent.OnFocusClick)
            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is TaskDetailEffect.NavigateToFocus)
            assertEquals(taskId, effect.taskId)
        }
    }
}
