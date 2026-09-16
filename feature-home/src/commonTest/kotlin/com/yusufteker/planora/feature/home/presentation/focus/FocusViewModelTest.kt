package com.yusufteker.planora.feature.home.presentation.focus

import com.yusufteker.planora.feature.home.domain.repository.PlanRepository
import com.yusufteker.planora.shared.api.CreatePlanRoomRequest
import com.yusufteker.planora.shared.api.CreateTaskRequest
import com.yusufteker.planora.shared.api.InviteUserRequest
import com.yusufteker.planora.shared.api.PlanRoomDto
import com.yusufteker.planora.shared.api.TaskDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

/**
 * FocusViewModel için sahte (Fake) PlanRepository.
 */
private class FakePlanRepository : PlanRepository {
    override suspend fun createTask(request: CreateTaskRequest, triggerSync: Boolean): Result<TaskDto> = Result.failure(Exception())
    override suspend fun updateTask(taskId: String, request: CreateTaskRequest, triggerSync: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun toggleTaskPinLocal(taskId: String, isPinned: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun deleteTask(taskId: String): Result<Unit> = Result.success(Unit)
    override suspend fun completeTaskInstance(taskId: String, dateMs: Long, isCompleted: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun joinTask(taskId: String, roomId: String): Result<Unit> = Result.success(Unit)
    override suspend fun fetchMyTasks(fromTime: Long?, toTime: Long?): Result<Unit> = Result.success(Unit)
    override suspend fun fetchRoomTasks(roomId: String, fromTime: Long?, toTime: Long?): Result<Unit> = Result.success(Unit)
    override suspend fun syncPendingChanges(): Result<Unit> = Result.success(Unit)
    override suspend fun autoScheduleTasks(taskIds: List<String>): Result<Unit> = Result.success(Unit)
    override fun observeAllTasks(): Flow<List<TaskDto>> = emptyFlow()
    override fun observeTasksForRange(fromTimeMs: Long, toTimeMs: Long): Flow<List<TaskDto>> = emptyFlow()
    override suspend fun fetchMyRooms(): Result<Unit> = Result.success(Unit)
    override suspend fun createPlanRoom(request: CreatePlanRoomRequest): Result<PlanRoomDto> = Result.failure(Exception())
    override suspend fun inviteUserToRoom(roomId: String, request: InviteUserRequest): Result<Unit> = Result.success(Unit)
    override suspend fun getMyPendingInvitations(): Result<List<PlanRoomDto>> = Result.success(emptyList())
    override suspend fun respondToInvite(roomId: String, accept: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun renameRoom(roomId: String, name: String): Result<Unit> = Result.success(Unit)
    override suspend fun deleteRoom(roomId: String): Result<Unit> = Result.success(Unit)
    override suspend fun leaveRoom(roomId: String): Result<Unit> = Result.success(Unit)
    override suspend fun removeMemberFromRoom(roomId: String, targetUserId: Int): Result<Unit> = Result.success(Unit)
    override suspend fun uploadRoomImage(roomId: String, imageBytes: ByteArray): Result<String> = Result.success("")
    override fun observeAllPlanRooms(): Flow<List<PlanRoomDto>> = emptyFlow()
    override suspend fun fetchAccessibleUsers(): Result<Unit> = Result.success(Unit)
    override fun observeAccessibleUsers(): Flow<List<com.yusufteker.planora.core.database.CalendarAccessEntity>> = emptyFlow()
    override suspend fun fetchSharedTasks(userId: Int, from: Long?, to: Long?): Result<List<TaskDto>> = Result.success(emptyList())
    override suspend fun deleteAccount(): Result<Unit> = Result.success(Unit)
}

/**
 * Pulse uygulamasının Odaklanma / Pomodoro sayacı (FocusViewModel) sınıfını
 * test eden birim ve coroutine testi.
 *
 * 2. KONU: Coroutine Testleri (runTest, advanceTimeBy, advanceUntilIdle)
 * - delay(1000) içeren sayacı gerçekte 1 saniye beklemek yerine sanal zamanla anında test ederiz.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FocusViewModelTest {

    // ViewModel'lerin viewModelScope'u Dispatchers.Main kullandığı için
    // test ortamında onu sanal testDispatcher ile değiştiriyoruz.
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() { //
        Dispatchers.resetMain()
    }

    private fun createViewModel(): FocusViewModel {
        return FocusViewModel(FakePlanRepository())
    }

    @Test
    fun `baslangicta odaklanma sayaci 25 dakika ve durdurulmus olmalidir`() {
        // 1. Arrange: ViewModel'i oluştur
        val viewModel = createViewModel()

        // 2. Assert: Başlangıç state'ini kontrol et
        assertEquals(25, viewModel.state.value.selectedDurationMinutes)
        assertEquals(1500, viewModel.state.value.timeRemainingSeconds) // 25 dakika * 60 saniye
        assertFalse(viewModel.state.value.isRunning)
        assertFalse(viewModel.state.value.isFinished)
    }

    @Test
    fun `odak suresi degistirildiginde kalan saniye otomatik hesaplanmalidir`() {
        // Arrange
        val viewModel = createViewModel()

        // Act: Süreyi 45 dakika yap
        viewModel.setFocusDuration(45)

        // Assert: 45 * 60 = 2700 saniye olmalı
        assertEquals(45, viewModel.state.value.selectedDurationMinutes)
        assertEquals(2700, viewModel.state.value.timeRemainingSeconds)
        assertFalse(viewModel.state.value.isFinished)
    }

    @Test
    fun `resetTimer cagrildiginda kalan sure secilen dakikaya geri donmeli ve sayac durmalidir`() {
        // Arrange: Önce süreyi 30 dakika yap
        val viewModel = createViewModel()
        viewModel.setFocusDuration(30)
        assertEquals(1800, viewModel.state.value.timeRemainingSeconds)

        // Act: Sayacı sıfırla
        viewModel.resetTimer()

        // Assert: Tekrar 30 dakika (1800 saniye) ve durmuş olmalı
        assertEquals(1800, viewModel.state.value.timeRemainingSeconds)
        assertFalse(viewModel.state.value.isRunning)
        assertFalse(viewModel.state.value.isFinished)
    }

    // ========================================================================
    // 2. KONU TESTLERİ: Coroutine Testleri (runTest, advanceTimeBy, advanceUntilIdle)
    // ========================================================================

    @Test
    fun `sayac baslatildiginda advanceTimeBy ile 1 saniye sonra kalan sure 1 azalmalidir`() = runTest {
        // 1. Arrange: ViewModel oluştur
        val viewModel = createViewModel()
        assertEquals(1500, viewModel.state.value.timeRemainingSeconds)

        // 2. Act: Sayacı başlat
        viewModel.toggleTimer()
        assertTrue(viewModel.state.value.isRunning)

        // Sanal zamanı 1 saniye ileri sar ve o andaki görevleri çalıştır:
        advanceTimeBy(1000.milliseconds)
        runCurrent()

        // 3. Assert: 1 saniye aktı ve süre 1500'den 1499'a düştü!
        assertEquals(1499, viewModel.state.value.timeRemainingSeconds)
    }

    @Test
    fun `advanceTimeBy ile 5 saniye ileri sarildiginda zaman beklemeden 5 saniye eksilmelidir`() = runTest {
        val viewModel = createViewModel()
        viewModel.toggleTimer()

        // Sanal zamanı 5000 ms (5 saniye) ileri sarıyoruz:
        advanceTimeBy(5000)
        runCurrent()

        // 1500 - 5 = 1495 olmalıdır:
        assertEquals(1495, viewModel.state.value.timeRemainingSeconds)
    }

    @Test
    fun `sayac duraklatildiginda zaman ileri sarilsa bile sure eksilmemelidir`() = runTest {
        val viewModel = createViewModel()
        viewModel.toggleTimer() // Başlat
        advanceTimeBy(3000)     // 3 saniye aktı
        runCurrent()
        assertEquals(1497, viewModel.state.value.timeRemainingSeconds)

        // Sayacı duraklat (pauseTimer)
        viewModel.toggleTimer()
        assertFalse(viewModel.state.value.isRunning)

        // Sayaç duraklatılmışken zamanı 10 saniye (10.000 ms) ileri sarıyoruz!
        advanceTimeBy(10000)
        runCurrent()

        // Sayaç durduğu için süre 1497'de sabit kalmalı, eksilmemeli!
        assertEquals(1497, viewModel.state.value.timeRemainingSeconds)
    }

    @Test
    fun `advanceUntilIdle ile tum sayac gercekte 1 milisaniyede bitirilmeli ve isFinished true olmalidir`() = runTest {
        val viewModel = createViewModel()
        viewModel.setFocusDuration(1) // 1 dakika = 60 saniye
        assertEquals(60, viewModel.state.value.timeRemainingSeconds)

        viewModel.toggleTimer() // Sayacı başlat

        // Normalde sayacın bitmesi için 60 saniye (1 dakika) beklememiz gerekirdi.
        // advanceUntilIdle() coroutine kuyruğundaki tüm delay ve işleri bitene kadar ileri sarar!
        advanceUntilIdle()

        // Sonuç: Sayaç bitti, 0 saniye kaldı, isFinished = true oldu!
        assertEquals(0, viewModel.state.value.timeRemainingSeconds)
        assertFalse(viewModel.state.value.isRunning)
        assertTrue(viewModel.state.value.isFinished)
    }
}
