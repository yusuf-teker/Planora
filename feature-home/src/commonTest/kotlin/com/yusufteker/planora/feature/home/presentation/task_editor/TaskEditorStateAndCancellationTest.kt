package com.yusufteker.planora.feature.home.presentation.task_editor

import app.cash.turbine.test
import com.yusufteker.planora.feature.home.data.repository.FakePlanRepository
import com.yusufteker.planora.feature.home.data.repository.FakeProfileRepository
import com.yusufteker.planora.feature.home.util.FakeSessionPreferences
import com.yusufteker.planora.shared.api.CreateTaskRequest
import com.yusufteker.planora.shared.api.TaskDto
import com.yusufteker.planora.shared.api.TaskPriority
import com.yusufteker.planora.shared.api.TaskStatus
import com.yusufteker.planora.shared.api.TaskType
import com.yusufteker.planora.shared.api.TaskVisibility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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

/**
 * 7. KONU: Görev Formu Validasyon, Loading, Error ve Debounce/Cancellation Testleri
 *
 * TaskEditorViewModel Test Paketi.
 *
 * Kapsanan Senaryolar:
 * 1. FORM & VALIDATION (Hata Durumu): Boş başlıkla kaydetme denendiğinde hata snackbar'ı verilmesi ve işlemin iptali.
 * 2. LOADING STATE: Görev kaydedilirken `isLoading` durumunun true başlayıp işlem bitince false'a dönmesi.
 * 3. ERROR RECOVERY: Veritabanı veya ağ hatası oluştuğunda `isLoading` durumunun güvenle sıfırlanması.
 * 4. COROUTINE CANCELLATION (DEBOUNCE): Hızlı ardışık metin girişlerinde eski auto-save işinin iptal edilip
 *    yalnızca son güncel verinin işlenmesi.
 * 5. CONCURRENT LOAD CANCELLATION: Peş peşe farklı görevler yüklendiğinde eski `loadJob` coroutine'inin iptal edilmesi.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TaskEditorStateAndCancellationTest {

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

    private fun createViewModel(): TaskEditorViewModel {
        return TaskEditorViewModel(
            planRepository = fakePlanRepository,
            profileRepository = fakeProfileRepository,
            sessionPreferences = fakeSessionPreferences
        )
    }

    private fun createDummyTask(
        id: String,
        title: String,
        type: TaskType = TaskType.TASK,
        status: TaskStatus = TaskStatus.PENDING,
        description: String? = null,
        startTime: Long = 1000L,
        endTime: Long? = 2000L,
        visibility: TaskVisibility = TaskVisibility.PRIVATE
    ): TaskDto {
        return TaskDto(
            id = id,
            creatorId = 1,
            title = title,
            description = description,
            startTime = startTime,
            endTime = endTime,
            type = type,
            status = status,
            visibility = visibility,
            sharedRoomIds = emptyList()
        )
    }

    // ========================================================================
    // 1. FORM VALIDASYON (ERROR STATE) SENARYOLARI
    // ========================================================================

    @Test
    fun `baslik bos iken kaydet tiklandiginda kayit engellenmeli ve hata snackbari yayilmalidir`() = runTest {
        val viewModel = createViewModel()
        runCurrent()

        // Başlık boş bırakılarak kaydet butonuna basılır:
        viewModel.effect.test {
            viewModel.onEvent(TaskEditorEvent.SaveClicked)
            runCurrent()

            // Assert: Boş başlık uyarısı verildi
            val effect = awaitItem()
            assertTrue(effect is TaskEditorEffect.ShowSnackbar)
            assertTrue(effect.message.contains("başlık"))

            // Assert: Repository'de hiçbir görev oluşturulmadı
            assertEquals(0, fakePlanRepository.getStoredTasks().size)

            expectNoEvents()
        }
    }

    // ========================================================================
    // 2. LOADING STATE SENARYOLARI
    // ========================================================================

    @Test
    fun `gorev basariyla kaydedildiginde loading durumu acilip kapanmali ve NavigateBack yayilmalidir`() = runTest {
        val viewModel = createViewModel()
        runCurrent()

        // Geçerli bir başlık gir
        viewModel.onEvent(TaskEditorEvent.TitleChanged("Mimarî Toplantısı"))
        runCurrent()

        viewModel.effect.test {
            viewModel.onEvent(TaskEditorEvent.SaveClicked)
            runCurrent()

            // Assert: NavigateBack effect'i yayınlandı
            val effect = awaitItem()
            assertEquals(TaskEditorEffect.NavigateBack, effect)

            // Assert: Görev başarıyla kaydedildi
            assertFalse(viewModel.state.value.isLoading)
            val allTasks = fakePlanRepository.getStoredTasks()
            assertEquals(1, allTasks.size)
            assertEquals("Mimarî Toplantısı", allTasks.first().title)

            expectNoEvents()
        }
    }

    // ========================================================================
    // 3. ERROR RECOVERY SENARYOLARI
    // ========================================================================

    @Test
    fun `kayit sirasinda ag hatasi olustugunda isLoading guvenle false olmali ve hata snackbari yayilmalidir`() = runTest {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onEvent(TaskEditorEvent.TitleChanged("Hatalı İstek Testi"))
        runCurrent()

        // Repository'nin hata vermesini sağla:
        fakePlanRepository.shouldFailNetwork = true
        fakePlanRepository.createTaskResultOverride = Result.failure(Exception("Sunucu bağlantısı koptu"))

        viewModel.effect.test {
            viewModel.onEvent(TaskEditorEvent.SaveClicked)
            runCurrent()

            // Assert: Hata snackbar'ı fırlatıldı
            val effect = awaitItem()
            assertTrue(effect is TaskEditorEffect.ShowSnackbar)
            assertTrue(effect.message.contains("güncellenemedi") || effect.message.contains("hata"))

            // Assert: Loading askıda kalmadı, güvenle false oldu
            assertFalse(viewModel.state.value.isLoading)

            expectNoEvents()
        }
    }

    // ========================================================================
    // 4. COROUTINE CANCELLATION (DEBOUNCE) SENARYOLARI
    // ========================================================================

    @Test
    fun `hizli yazi yazilirken auto-save debounce coroutine ile eski istekleri iptal etmelidir`() = runTest {
        // Var olan bir görevi seed et:
        val existingTask = createDummyTask(
            id = "task-auto-save",
            title = "Eski Başlık"
        )
        fakePlanRepository.seedTasks(listOf(existingTask))

        val viewModel = createViewModel()
        viewModel.onEvent(TaskEditorEvent.OnLoadTask(taskId = "task-auto-save"))
        runCurrent()

        fakePlanRepository.observeAllTasks().test {
            // Başlangıç durumu
            val currentList = awaitItem()
            assertEquals("Eski Başlık", currentList.first().title)

            // 1. İlk harf yazıldı:
            viewModel.onEvent(TaskEditorEvent.TitleChanged("A"))
            advanceTimeBy(300) // 300 ms geçti (1000ms dolmadı, autoSave HENÜZ tetiklenmedi)
            runCurrent()
            expectNoEvents()

            // 2. İkinci harf yazıldı (Eski debounce süresi sıfırlanır):
            viewModel.onEvent(TaskEditorEvent.TitleChanged("Ab"))
            advanceTimeBy(400) // 400 ms geçti (toplam 700ms, hala 1000ms dolmadı)
            runCurrent()
            expectNoEvents()

            // 3. Son kelime yazıldı ve kullanıcı beklemeye geçti:
            viewModel.onEvent(TaskEditorEvent.TitleChanged("Abone Listesi"))
            // 1000ms debounce süresi dolana kadar zamanı ilerlet:
            advanceTimeBy(1100)
            runCurrent()

            // Assert: Debounce süresi dolunca tek seferde son durum "Abone Listesi" kaydedildi!
            val updatedList = awaitItem()
            assertEquals("Abone Listesi", updatedList.first().title)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========================================================================
    // 5. CONCURRENT LOAD CANCELLATION SENARYOLARI
    // ========================================================================

    @Test
    fun `pes pese farkli gorevler yuklendiginde eski yukleme isi iptal edilip en sonuncusu yuklenmelidir`() = runTest {
        val task1 = createDummyTask(id = "task-1", title = "1. Görev")
        val task2 = createDummyTask(id = "task-2", title = "2. Görev")
        fakePlanRepository.seedTasks(listOf(task1, task2))

        val viewModel = createViewModel()

        // 1. Görevi yüklemeyi başlat:
        viewModel.onEvent(TaskEditorEvent.OnLoadTask(taskId = "task-1"))
        // Anında 2. görevi yükle (Eski Job iptal edilir):
        viewModel.onEvent(TaskEditorEvent.OnLoadTask(taskId = "task-2"))
        runCurrent()

        // Assert: Sonuçta 2. görev yüklenmiş olmalıdır
        assertEquals("task-2", viewModel.state.value.id)
        assertEquals("2. Görev", viewModel.state.value.title)
    }
}
