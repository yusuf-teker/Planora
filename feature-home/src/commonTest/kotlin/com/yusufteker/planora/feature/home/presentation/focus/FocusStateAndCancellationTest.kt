package com.yusufteker.planora.feature.home.presentation.focus

import app.cash.turbine.test
import com.yusufteker.planora.feature.home.data.repository.FakePlanRepository
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
 * 5. KONU: State Testleri (Loading, Success, Error ve Coroutine Cancellation)
 *
 * Bir uygulamanın mimari kalitesi ve dayanıklılığı, tüm yaşam döngüsü durumlarını
 * (Loading, Success, Error, Cancellation) doğru yönetebilmesine bağlıdır.
 *
 * Bu test sınıfında:
 * 1. LOADING STATE: İşlem başlatıldığında arayüzün loading moduna geçişi ve çifte tıklama (re-entrant) koruması.
 * 2. SUCCESS STATE: İşlemin başarıyla tamamlanması, form/sayaç sıfırlaması ve yönlendirme effect'i.
 * 3. ERROR STATE: Ağ veya sunucu hatasında loading'in güvenle kapanması, sayacın bozulmaması ve hata snackbar'ı.
 * 4. COROUTINE CANCELLATION: Sayaç durdurulduğunda veya sıfırlandığında coroutine Job'ının
 *    gerçekten iptal edilmesi ve zaman ileri sarılsa bile bellek/arkaplan sızıntısı yapmaması.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FocusStateAndCancellationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakePlanRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakePlanRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): FocusViewModel {
        return FocusViewModel(fakeRepository)
    }

    // ========================================================================
    // 1. LOADING STATE SENARYOLARI
    // ========================================================================

    @Test
    fun `completeTask cagrildiginda Loading durumu isCompleting dogru sekilde baslamali ve bitmelidir`() = runTest {
        val viewModel = createViewModel()
        viewModel.loadTask("task_load_1")
        runCurrent()

        // Turbine ile state akışını dinliyoruz:
        viewModel.state.test {
            // Başlangıç: isCompleting = false
            val initial = awaitItem()
            assertFalse(initial.isCompleting)

            // Act: Tamamlama işlemini tetikle
            viewModel.completeTask()
            runCurrent()

            // 1. Durum: İşlem başladı -> isCompleting = true (Loading açık)
            val loadingState = awaitItem()
            assertTrue(loadingState.isCompleting)

            // 2. Durum: İşlem bitti -> isCompleting = false (Loading kapandı)
            val finishedState = awaitItem()
            assertFalse(finishedState.isCompleting)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isCompleting true iken cift tiklama yapilirsa mukerrer istek engellenmelidir`() = runTest {
        // Arrange: Repository'ye 1000 ms sanal gecikme veriyoruz (ağ çağrısı simülasyonu)
        fakeRepository.completeTaskDelayMs = 1000L

        val viewModel = createViewModel()
        viewModel.loadTask("task_double_click")
        runCurrent()

        viewModel.state.test {
            awaitItem() // Başlangıç state'i

            // 1. İlk tıklama: İşlem başlar
            viewModel.completeTask()
            runCurrent() // Coroutine başlar, isCompleting = true olur ve repo'daki delay(1000)'e girer

            val loadingState = awaitItem()
            assertTrue(loadingState.isCompleting)

            // 2. İşlem henüz sürerken (isCompleting = true iken) kullanıcı 2. kez tıklar:
            viewModel.completeTask()
            runCurrent()

            // 3. Zamanı 1000 ms ileri sararak ilk işlemin tamamlanmasını sağlıyoruz:
            advanceTimeBy(1000)
            runCurrent()

            val finishedState = awaitItem()
            assertFalse(finishedState.isCompleting)

            // Assert: 2. tıklama if (isCompleting) return ile engellendiği için fazladan hiçbir state yayılmaz!
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========================================================================
    // 2. SUCCESS STATE SENARYOLARI
    // ========================================================================

    @Test
    fun `islem basarili oldugunda sayac sifirlanmali ve NavigateBack effect yayilmalidir`() = runTest {
        val viewModel = createViewModel()
        viewModel.loadTask("task_success_test")
        viewModel.setFocusDuration(30)
        viewModel.toggleTimer() // Sayacı başlat
        advanceTimeBy(3000)     // 3 saniye aktı
        runCurrent()
        assertTrue(viewModel.state.value.isRunning)

        // Hem state hem de effect'i Turbine ile takip edelim:
        viewModel.effect.test {
            // Act: Görevi tamamla
            viewModel.completeTask()
            runCurrent()

            // Assert 1: Tek seferlik NavigateBack effect'i yayıldı
            val effect = awaitItem()
            assertEquals(FocusEffect.NavigateBack, effect)

            // Assert 2: Sayaç durduruldu ve başlangıç süresine (30 dk * 60 = 1800 sn) resetlendi
            assertFalse(viewModel.state.value.isRunning)
            assertEquals(1800, viewModel.state.value.timeRemainingSeconds)
            assertFalse(viewModel.state.value.isFinished)

            expectNoEvents()
        }
    }

    // ========================================================================
    // 3. ERROR STATE SENARYOLARI
    // ========================================================================

    @Test
    fun `sunucu hatasinda isCompleting guvenle false olmali ve NavigateBack tetiklenmeyip Snackbar gosterilmelidir`() = runTest {
        // Arrange: Repository'de hata simülasyonunu aç
        fakeRepository.shouldFailNetwork = true
        fakeRepository.networkErrorMessage = "Sunucu 500: İç Sunucu Hatası"

        val viewModel = createViewModel()
        viewModel.loadTask("task_error_state")
        runCurrent()

        viewModel.effect.test {
            // Act: Başarısız olacak tamamlama çağrısı
            viewModel.completeTask()
            runCurrent()

            // Assert 1: Error Snackbar effect'i yakalandı
            val effect = awaitItem()
            assertTrue(effect is FocusEffect.ShowSnackbar)
            assertEquals("Görev tamamlanamadı. Lütfen tekrar deneyin.", effect.message)

            // Assert 2: Loading durumu askıda kalmadı, güvenle false oldu
            assertFalse(viewModel.state.value.isCompleting)

            // Assert 3: Hata durumunda ekrandan GERİ ÇIKILMADI (NavigateBack tetiklenmedi)
            expectNoEvents()
        }
    }

    // ========================================================================
    // 4. COROUTINE CANCELLATION (İPTAL) SENARYOLARI
    // ========================================================================

    @Test
    fun `sayac duraklatildiginda timer coroutine'i iptal edilmeli ve zaman ilerlese bile sure degismemelidir`() = runTest {
        val viewModel = createViewModel()
        viewModel.toggleTimer() // Sayacı başlat (Job başladı)

        // 5 saniye zaman akıt
        advanceTimeBy(5000)
        runCurrent()
        assertEquals(1495, viewModel.state.value.timeRemainingSeconds)

        // Act: Sayacı duraklat (pauseTimer -> timerJob?.cancel())
        viewModel.toggleTimer()
        assertFalse(viewModel.state.value.isRunning)

        // Sanal zamanı devasa bir süre (1 saat = 3.600.000 ms) ileri sarıyoruz!
        // Eğer coroutine iptal edilmemiş (leak olmuş) olsaydı, süre sıfırlanıp biterdi!
        advanceTimeBy(3_600_000)
        runCurrent()

        // Assert: Coroutine iptal edildiği için süre 1495'te donup kaldı, hiçbir sızıntı olmadı!
        assertEquals(1495, viewModel.state.value.timeRemainingSeconds)
        assertFalse(viewModel.state.value.isFinished)
    }

    @Test
    fun `sayac calisirken resetTimer yapildiginda coroutine iptal edilip sure secilen dakikaya donmelidir`() = runTest {
        val viewModel = createViewModel()
        viewModel.setFocusDuration(10) // 10 dakika = 600 saniye
        viewModel.toggleTimer()

        // 10 saniye aktı
        advanceTimeBy(10000)
        runCurrent()
        assertEquals(590, viewModel.state.value.timeRemainingSeconds)

        // Act: Sayacı sıfırla (timerJob?.cancel() ve süre resetlenir)
        viewModel.resetTimer()

        // Sanal zamanı tekrar 30 saniye ileri saralım:
        advanceTimeBy(30000)
        runCurrent()

        // Assert:
        // 1. Sayaç durdu ve tam 600 saniyeye resetlendi
        assertEquals(600, viewModel.state.value.timeRemainingSeconds)
        assertFalse(viewModel.state.value.isRunning)
        assertFalse(viewModel.state.value.isFinished)
    }

    @Test
    fun `sayac calisirken pes pese toggle yapildiginda eski Job iptal edilip yeni dongu saglikli calismalidir`() = runTest {
        val viewModel = createViewModel()

        // 1. Başlat
        viewModel.toggleTimer()
        advanceTimeBy(2000)
        runCurrent()
        assertEquals(1498, viewModel.state.value.timeRemainingSeconds)

        // 2. Durdur (Eski Job iptal edildi)
        viewModel.toggleTimer()
        advanceTimeBy(5000)
        runCurrent()
        assertEquals(1498, viewModel.state.value.timeRemainingSeconds)

        // 3. Tekrar Başlat (Yeni Job açıldı)
        viewModel.toggleTimer()
        advanceTimeBy(3000)
        runCurrent()

        // 1498 - 3 = 1495 olmalıdır:
        assertEquals(1495, viewModel.state.value.timeRemainingSeconds)
        assertTrue(viewModel.state.value.isRunning)
    }
}
