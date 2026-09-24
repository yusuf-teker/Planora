package com.yusufteker.planora.feature.home.presentation.trash

import app.cash.turbine.test
import com.yusufteker.planora.feature.home.data.repository.FakePlanRepository
import com.yusufteker.planora.feature.home.domain.model.DeletedTaskItem
import com.yusufteker.planora.feature.home.util.FakeSessionPreferences
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
 * Planora Recycle Bin (Trash) Unit Tests.
 *
 * Validates the 7-day retention visibility limit for Free users,
 * 30-day full recovery for Premium users, and navigation effects.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TrashViewModelTest {

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

    private fun createViewModel(): TrashViewModel {
        return TrashViewModel(
            planRepository = planRepository,
            sessionPreferences = sessionPreferences,
            timeProvider = { fixedNowMs }
        )
    }

    @Test
    fun `ucretsiz kullanici icin 7 gunden eski silinmis gorevler filtrelenir ve gizli sayisina eklenir`() = runTest {
        // Arrange: 2 gün önce, 5 gün önce (görünür) ve 9 gün önce, 15 gün önce (gizli) silinmiş görevler
        val recent1 = DeletedTaskItem(id = "1", title = "Görev 1 (2 gün)", deletedAt = fixedNowMs - (2 * oneDayMs), daysRemaining = 28)
        val recent2 = DeletedTaskItem(id = "2", title = "Görev 2 (5 gün)", deletedAt = fixedNowMs - (5 * oneDayMs), daysRemaining = 25)
        val old1 = DeletedTaskItem(id = "3", title = "Görev 3 (9 gün)", deletedAt = fixedNowMs - (9 * oneDayMs), daysRemaining = 21)
        val old2 = DeletedTaskItem(id = "4", title = "Görev 4 (15 gün)", deletedAt = fixedNowMs - (15 * oneDayMs), daysRemaining = 15)

        planRepository.seedDeletedTasks(listOf(recent1, recent2, old1, old2))
        sessionPreferences.setPremium(false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Assert: Yalnızca son 7 gündeki 2 görev görünmeli, 2 görev gizlenmeli
        val state = viewModel.state.value
        assertFalse(state.isPremium)
        assertEquals(2, state.items.size)
        assertEquals(listOf("1", "2"), state.items.map { it.id })
        assertEquals(2, state.hiddenItemCount)
    }

    @Test
    fun `kullanici premium oldugunda 30 gune kadar tum silinen gorevler eksiksiz gorunur`() = runTest {
        val recent = DeletedTaskItem(id = "1", title = "Görev 1 (3 gün)", deletedAt = fixedNowMs - (3 * oneDayMs), daysRemaining = 27)
        val old = DeletedTaskItem(id = "2", title = "Görev 2 (20 gün)", deletedAt = fixedNowMs - (20 * oneDayMs), daysRemaining = 10)

        planRepository.seedDeletedTasks(listOf(recent, old))
        sessionPreferences.setPremium(false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Başta ücretsiz: 1 görünür, 1 gizli
        assertEquals(1, viewModel.state.value.items.size)
        assertEquals(1, viewModel.state.value.hiddenItemCount)

        // Act: Kullanıcı Premium'a yükseltilir
        sessionPreferences.setPremium(true)
        advanceUntilIdle()

        // Assert: Artık tüm öğeler görünür, hiddenItemCount 0 olur
        val updatedState = viewModel.state.value
        assertTrue(updatedState.isPremium)
        assertEquals(2, updatedState.items.size)
        assertEquals(0, updatedState.hiddenItemCount)
    }

    @Test
    fun `UpgradeClicked olayi NavigateToPremium efektini tetikler`() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.onEvent(TrashEvent.UpgradeClicked)
            val effect = awaitItem()
            assertEquals(TrashEffect.NavigateToPremium, effect)
        }
    }
}
