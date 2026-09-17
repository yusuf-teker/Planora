package com.yusufteker.planora.feature.home.presentation.focus

import com.yusufteker.planora.feature.home.domain.repository.PlanRepository
import com.yusufteker.planora.shared.api.CreatePlanRoomRequest
import com.yusufteker.planora.shared.api.CreateTaskRequest
import com.yusufteker.planora.shared.api.InviteUserRequest
import com.yusufteker.planora.shared.api.PlanRoomDto
import com.yusufteker.planora.shared.api.TaskDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import app.cash.turbine.test
import com.yusufteker.planora.shared.api.TaskStatus
import com.yusufteker.planora.shared.api.TaskType
import com.yusufteker.planora.shared.api.TaskVisibility
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

/**
 * FocusViewModel için sahte (Fake) PlanRepository.
 */
private class FakePlanRepository : PlanRepository {
    var completeTaskResult: Result<Unit> = Result.success(Unit)
    var tasksFlow: Flow<List<TaskDto>> = emptyFlow()

    override suspend fun createTask(request: CreateTaskRequest, triggerSync: Boolean): Result<TaskDto> = Result.failure(Exception())
    override suspend fun updateTask(taskId: String, request: CreateTaskRequest, triggerSync: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun toggleTaskPinLocal(taskId: String, isPinned: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun deleteTask(taskId: String): Result<Unit> = Result.success(Unit)
    override suspend fun completeTaskInstance(taskId: String, dateMs: Long, isCompleted: Boolean): Result<Unit> = completeTaskResult
    override suspend fun joinTask(taskId: String, roomId: String): Result<Unit> = Result.success(Unit)
    override suspend fun fetchMyTasks(fromTime: Long?, toTime: Long?): Result<Unit> = Result.success(Unit)
    override suspend fun fetchRoomTasks(roomId: String, fromTime: Long?, toTime: Long?): Result<Unit> = Result.success(Unit)
    override suspend fun syncPendingChanges(): Result<Unit> = Result.success(Unit)
    override suspend fun autoScheduleTasks(taskIds: List<String>): Result<Unit> = Result.success(Unit)
    override fun observeAllTasks(): Flow<List<TaskDto>> = tasksFlow
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

    private fun createViewModel(repo: FakePlanRepository = FakePlanRepository()): FocusViewModel {
        return FocusViewModel(repo)
    }

    private fun createDummyTask(id: String, title: String): TaskDto {
        return TaskDto(
            id = id,
            creatorId = 1,
            title = title,
            description = null,
            startTime = 1000L,
            endTime = 2000L,
            type = TaskType.TASK,
            status = TaskStatus.PENDING,
            visibility = TaskVisibility.PRIVATE,
            sharedRoomIds = emptyList()
        )
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

    // ========================================================================
    // 3. KONU: Flow & Turbine (app.cash.turbine) Testleri
    // ========================================================================
    // Turbine, Kotlin Flow'larını (StateFlow, SharedFlow, cold Flow) test etmek
    // için geliştirilmiş resmi Cash App kütüphanesidir.
    //
    // Neden Turbine kullanmalıyız?
    // 1. flow.collect { } ile elle coroutine açıp listeye doldurmak karmaşıktır ve
    //    özellikle SharedFlow gibi tek seferlik hot akışlarda yarış durumlarına (race condition)
    //    veya kaçırılan event'lere yol açar.
    // 2. Turbine `flow.test { }` bloğu ile emisyonları bir kuyrukta toplar.
    //    - awaitItem(): Sıradaki yayılan elemanı anında bekleyip yakalar.
    //    - expectNoEvents(): Beklenmedik başka bir event olmadığını doğrular.
    //    - cancelAndIgnoreRemainingEvents(): İlgili adımlar bittiğinde akışı güvenle kapatır.
    // ========================================================================

    @Test
    fun `Turbine ile StateFlow durum degisimleri adim adim yakalanip dogrulanmalidir`() = runTest {
        val viewModel = createViewModel()

        // StateFlow her zaman son bir değere (initial state) sahiptir.
        // viewModel.state.test { ... } bloğuna girdiğimiz anda ilk item kuyruğa girer.
        viewModel.state.test {
            // 1. Adım: Başlangıç state'ini yakala
            val initialState = awaitItem()
            assertEquals(25, initialState.selectedDurationMinutes)
            assertEquals(1500, initialState.timeRemainingSeconds)
            assertFalse(initialState.isRunning)

            // 2. Adım: Kullanıcı süreyi 40 dakika yapıyor
            viewModel.setFocusDuration(40)

            // StateFlow güncellendi, sıradaki yeni state'i yakala:
            val durationUpdatedState = awaitItem()
            assertEquals(40, durationUpdatedState.selectedDurationMinutes)
            assertEquals(2400, durationUpdatedState.timeRemainingSeconds)

            // 3. Adım: Kullanıcı sayacı başlatıyor
            viewModel.toggleTimer()

            // Sayacın çalışmaya başladığı (isRunning = true) state'ini yakala:
            val runningState = awaitItem()
            assertTrue(runningState.isRunning)

            // O anda başka beklenmedik bir event olmamalı:
            expectNoEvents()

            // Dinlemeyi temiz bir şekilde sonlandırıyoruz:
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Turbine ile SharedFlow tek seferlik basarili tamamlama eventi NavigateBack yakalanmalidir`() = runTest {
        // Arrange: Başarılı sonuç dönecek fake repository ve viewModel
        val fakeRepo = FakePlanRepository().apply {
            completeTaskResult = Result.success(Unit)
        }
        val viewModel = createViewModel(fakeRepo)

        // Bir görev seçili olsun:
        viewModel.loadTask("task_123")
        runCurrent()

        // SharedFlow'lar tek seferlik (one-time event) ve genelde replay=0 akışlardır.
        // Bu yüzden ÖNCE effect.test { } bloğu ile dinlemeye başlamalıyız,
        // ARDINDAN aksiyonu tetiklemeliyiz!
        viewModel.effect.test {
            // Act: Görevi tamamla
            viewModel.completeTask()
            runCurrent()

            // Assert: awaitItem() ile yayılmış olan NavigateBack effect'ini yakalıyoruz
            val effect = awaitItem()
            assertEquals(FocusEffect.NavigateBack, effect)

            // Başka bir effect yayılmamış olmalıdır:
            expectNoEvents()
        }
    }

    @Test
    fun `Turbine ile SharedFlow hata durumunda ShowSnackbar eventi yakalanmalidir`() = runTest {
        // Arrange: Hata fırlatan fake repository
        val fakeRepo = FakePlanRepository().apply {
            completeTaskResult = Result.failure(RuntimeException("Ağ bağlantısı koptu"))
        }
        val viewModel = createViewModel(fakeRepo)

        viewModel.loadTask("task_error_case")
        runCurrent()

        viewModel.effect.test {
            // Act: Görevi tamamlama başarısız olacak
            viewModel.completeTask()
            runCurrent()

            // Assert: Hata snackbar effect'ini yakala ve mesajı doğrula
            val effect = awaitItem()
            assertTrue(effect is FocusEffect.ShowSnackbar)
            assertEquals("Görev tamamlanamadı. Lütfen tekrar deneyin.", effect.message)

            expectNoEvents()
        }
    }

    @Test
    fun `completeTask sirasinda StateFlow isCompleting gecisi Turbine ile adim adim yakalanmalidir`() = runTest {
        val fakeRepo = FakePlanRepository().apply {
            completeTaskResult = Result.success(Unit)
        }
        val viewModel = createViewModel(fakeRepo)
        viewModel.loadTask("task_loading_test")
        runCurrent()

        // completeTask çağrıldığında: isCompleting false -> true -> false geçişi yapar.
        // Turbine ile bu ara durumları kaçırmadan sırayla doğrularız:
        viewModel.state.test {
            // Mevcut state (isCompleting = false):
            val initial = awaitItem()
            assertFalse(initial.isCompleting)

            // Act: Görevi tamamla
            viewModel.completeTask()
            runCurrent()

            // 1. Ara durum: isCompleting = true (yükleme başladı)
            val loadingState = awaitItem()
            assertTrue(loadingState.isCompleting)

            // 2. Son durum: isCompleting = false (yükleme bitti)
            val finishedState = awaitItem()
            assertFalse(finishedState.isCompleting)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Repository Flow akisindan gelen gorev listesi ViewModel StateFlow'una adim adim yansimalidir`() = runTest {
        val tasksSharedFlow = MutableSharedFlow<List<TaskDto>>()
        val fakeRepo = FakePlanRepository().apply {
            tasksFlow = tasksSharedFlow
        }
        val viewModel = createViewModel(fakeRepo)

        viewModel.state.test {
            // 1. Başlangıç state'i (taskId null, taskTitle varsayılan olarak "Odak Zamanı")
            val initial = awaitItem()
            assertNull(initial.taskId)
            assertEquals("Odak Zamanı", initial.taskTitle)

            // 2. loadTask çağrılıyor:
            viewModel.loadTask("task_pulse_101")
            runCurrent()

            // State güncellendi ve taskId set edildi:
            val stateWithTaskId = awaitItem()
            assertEquals("task_pulse_101", stateWithTaskId.taskId)

            // 3. Repository Flow'u yeni bir task listesi yayıyor (emit):
            val sampleTasks = listOf(
                createDummyTask(id = "task_pulse_101", title = "Turbine ile Flow Testi Öğren")
            )
            tasksSharedFlow.emit(sampleTasks)
            runCurrent()

            // 4. ViewModel bu Flow'u collect edip taskTitle'ı güncelledi!
            val updatedState = awaitItem()
            assertEquals("Turbine ile Flow Testi Öğren", updatedState.taskTitle)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
