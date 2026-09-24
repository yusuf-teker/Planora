package com.yusufteker.planora.feature.home.presentation.plan_rooms

import app.cash.turbine.test
import com.yusufteker.planora.core.snackbar.SnackbarManager
import com.yusufteker.planora.feature.home.data.repository.FakePlanRepository
import com.yusufteker.planora.feature.home.data.repository.FakeProfileRepository
import com.yusufteker.planora.feature.home.util.FakeSessionPreferences
import com.yusufteker.planora.shared.api.PlanRoomDto
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
 * PlanRoomsViewModel Unit Test Paketi.
 *
 * Ücretsiz kullanıcılar için maksimum 3 plan odası kısıtlaması,
 * limit aşımında paywall yönlendirmesi ve Premium kullanıcıların
 * sınırsız oda hakkını doğrular.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlanRoomsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var planRepository: FakePlanRepository
    private lateinit var profileRepository: FakeProfileRepository
    private lateinit var snackbarManager: SnackbarManager
    private lateinit var sessionPreferences: FakeSessionPreferences

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        planRepository = FakePlanRepository()
        profileRepository = FakeProfileRepository()
        snackbarManager = com.yusufteker.planora.core.snackbar.DefaultSnackbarManager()
        sessionPreferences = FakeSessionPreferences()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): PlanRoomsViewModel {
        return PlanRoomsViewModel(
            planRepository = planRepository,
            profileRepository = profileRepository,
            snackbarManager = snackbarManager,
            sessionPreferences = sessionPreferences
        )
    }

    private fun createDummyRoom(id: String, name: String): PlanRoomDto {
        return PlanRoomDto(
            id = id,
            name = name,
            creatorId = 1,
            createdAt = 1000L,
            members = emptyList()
        )
    }

    @Test
    fun `ucretsiz kullanici 3 odaya sahipken yeni oda acmak istediginde limit dialogu acilir`() = runTest {
        // Arrange: Kullanıcı 3 odaya sahip ve Premium değil
        val rooms = listOf(
            createDummyRoom("1", "Oda 1"),
            createDummyRoom("2", "Oda 2"),
            createDummyRoom("3", "Oda 3")
        )
        planRepository.seedRooms(rooms)
        sessionPreferences.setPremium(false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Act: Kullanıcı yeni oda oluştur'a basar
        viewModel.onEvent(PlanRoomsEvent.OnCreateRoomClick(isVisible = true))
        advanceUntilIdle()

        // Assert: Oda oluşturma modalı açılmaz, limit uyarısı dialogu açılır
        val state = viewModel.state.value
        assertFalse(state.isCreateRoomDialogVisible)
        assertTrue(state.showRoomLimitDialog)
    }

    @Test
    fun `ucretsiz kullanici 2 odaya sahipken yeni oda dialogunu basariyla acabilir`() = runTest {
        // Arrange: Kullanıcının 2 odası var (< 3)
        val rooms = listOf(
            createDummyRoom("1", "Oda 1"),
            createDummyRoom("2", "Oda 2")
        )
        planRepository.seedRooms(rooms)
        sessionPreferences.setPremium(false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Act
        viewModel.onEvent(PlanRoomsEvent.OnCreateRoomClick(isVisible = true))
        advanceUntilIdle()

        // Assert
        val state = viewModel.state.value
        assertTrue(state.isCreateRoomDialogVisible)
        assertFalse(state.showRoomLimitDialog)
    }

    @Test
    fun `premium kullanici 3 veya daha fazla odaya sahip olsa bile sinirsiz oda olusturabilir`() = runTest {
        // Arrange: Kullanıcı Premium ve zaten 5 odası var
        val rooms = (1..5).map { createDummyRoom(it.toString(), "Oda $it") }
        planRepository.seedRooms(rooms)
        sessionPreferences.setPremium(true)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Act
        viewModel.onEvent(PlanRoomsEvent.OnCreateRoomClick(isVisible = true))
        advanceUntilIdle()

        // Assert
        val state = viewModel.state.value
        assertTrue(state.isPremium)
        assertTrue(state.isCreateRoomDialogVisible)
        assertFalse(state.showRoomLimitDialog)
    }

    @Test
    fun `ucretsiz kullanici 3 odaya sahipken davet kabul etmeye calisirsa islem engellenir`() = runTest {
        val rooms = listOf(
            createDummyRoom("1", "Oda 1"),
            createDummyRoom("2", "Oda 2"),
            createDummyRoom("3", "Oda 3")
        )
        planRepository.seedRooms(rooms)
        sessionPreferences.setPremium(false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Act: Gelen daveti kabul etmeye çalışır
        viewModel.onEvent(PlanRoomsEvent.RespondToInvite(roomId = "room_inv_4", accept = true))
        advanceUntilIdle()

        // Assert: 4. odaya katılamaz, limit dialogu gösterilir
        val state = viewModel.state.value
        assertTrue(state.showRoomLimitDialog)
    }

    @Test
    fun `OnDismissRoomLimitDialog navigateToPremium=true ile NavigateToPremium efektini tetikler`() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.onEvent(PlanRoomsEvent.OnDismissRoomLimitDialog(navigateToPremium = true))
            val effect = awaitItem()
            assertEquals(PlanRoomsEffect.NavigateToPremium, effect)
        }
        assertFalse(viewModel.state.value.showRoomLimitDialog)
    }
}
