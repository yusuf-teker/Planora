package com.yusufteker.planora.feature.home.presentation.aichat

import app.cash.turbine.test
import com.yusufteker.planora.core.ai.CloudAiManager
import com.yusufteker.planora.core.ui.text.UiText
import com.yusufteker.planora.feature.home.data.repository.FakePlanRepository
import com.yusufteker.planora.feature.home.data.repository.FakeProfileRepository
import com.yusufteker.planora.feature.home.util.FakeAiApi
import com.yusufteker.planora.feature.home.util.FakeOfflineAiManager
import com.yusufteker.planora.feature.home.util.FakeSessionPreferences
import com.yusufteker.planora.shared.ai.AiChatResult
import com.yusufteker.planora.shared.ai.AiChatServerResponse
import com.yusufteker.planora.shared.ai.AiIntent
import com.yusufteker.planora.shared.ai.AiQuotaDto
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
 * 8. KONU: Yapay Zeka (AI) Asistanı Loading, Error, Quota ve Cancellation Testleri
 *
 * Planora AiChatViewModel Test Paketi.
 *
 * Kapsanan Senaryolar:
 * 1. LOADING STATE (QUOTA & CHAT): Kota ve AI yanıtı yüklenirken loading durumlarının takibi ve input temizliği.
 * 2. SUCCESS RESPONSE: Gemini AI yanıt verdiğinde mesaj listesine eklenmesi ve kotanın güncellenmesi.
 * 3. QUOTA EXCEEDED (FALLBACK RECOVERY): Sunucu kotası aşıldığında yerel kural tabanlı motora (OfflineAiManager)
 *    düşülmesi ve kullanıcının yarı yolda bırakılmaması.
 * 4. NETWORK ERROR FALLBACK: Sunucuya ulaşılamadığında güvenle yerel motora geçiş yapılması.
 * 5. COROUTINE CANCELLATION (CANCEL GENERATION): Yanıt üretilirken kullanıcının vazgeçmesi/durdurması durumunda
 *    `generationJob`ın iptal edilmesi, `isLoading`ın false olması ve sonradan mesaj sızıntısı olmaması.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AiChatStateAndCancellationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakePlanRepository: FakePlanRepository
    private lateinit var fakeProfileRepository: FakeProfileRepository
    private lateinit var fakeSessionPreferences: FakeSessionPreferences
    private lateinit var fakeAiApi: FakeAiApi
    private lateinit var fakeOfflineAiManager: FakeOfflineAiManager
    private lateinit var cloudAiManager: CloudAiManager

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakePlanRepository = FakePlanRepository()
        fakeProfileRepository = FakeProfileRepository()
        fakeSessionPreferences = FakeSessionPreferences()
        fakeAiApi = FakeAiApi()
        fakeOfflineAiManager = FakeOfflineAiManager()
        cloudAiManager = CloudAiManager()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): AiChatViewModel {
        return AiChatViewModel(
            planRepository = fakePlanRepository,
            profileRepository = fakeProfileRepository,
            sessionPreferences = fakeSessionPreferences,
            offlineAiManager = fakeOfflineAiManager,
            cloudAiManager = cloudAiManager,
            aiApi = fakeAiApi
        )
    }

    // ========================================================================
    // 1. LOADING VE KOTA DURUMU SENARYOLARI
    // ========================================================================

    @Test
    fun `acilis aninda kota yuklenirken isQuotaLoading yonetilmeli ve basariyla statee yazilmalidir`() = runTest {
        // Kota için simüle edilmiş ağ gecikmesi:
        fakeAiApi.simulateDelayMs = 500L
        val viewModel = createViewModel()

        viewModel.state.test {
            // 1. StateFlow ilk yayını: varsayılan state (henüz coroutine çalışmadı)
            val initial = awaitItem()
            assertFalse(initial.isQuotaLoading)

            // loadQuota coroutine'ini başlat
            runCurrent()

            // 2. Kota çekimi başladı: isQuotaLoading = true
            val loadingState = awaitItem()
            assertTrue(loadingState.isQuotaLoading)

            // Simüle edilen gecikmeyi (500ms) ileri sar
            advanceTimeBy(600)
            runCurrent()

            // 3. Kota başarıyla çekildi: isQuotaLoading = false
            val loadedState = awaitItem()
            assertFalse(loadedState.isQuotaLoading)
            assertEquals(1, loadedState.quota.dailyRemaining)
            assertEquals(3, loadedState.quota.weeklyRemaining)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `mesaj gonderildiginde input temizlenmeli isLoading acilip yanitla birlikte kapanmalidir`() = runTest {
        val viewModel = createViewModel()
        runCurrent() // loadQuota tamamlandı

        // Sohbet simülasyonu için 1000ms gecikme ayarla
        fakeAiApi.simulateDelayMs = 1000L

        // Kullanıcı metin girdi:
        viewModel.onEvent(AiChatEvent.InputTextChanged("Bugün saat 15:00'te toplantı ekle"))
        assertEquals("Bugün saat 15:00'te toplantı ekle", viewModel.state.value.inputText)

        viewModel.state.test {
            awaitItem() // Mevcut durum (1 karşılama mesajı)

            // Mesajı gönder
            viewModel.onEvent(AiChatEvent.SendMessage)
            runCurrent()

            // 1. Ara Durum: Kullanıcı mesajı listeye eklendi (toplam 2 mesaj: welcome + user), input boşaldı, isLoading = true
            val sendingState = awaitItem()
            assertTrue(sendingState.isLoading)
            assertEquals("", sendingState.inputText)
            assertEquals(2, sendingState.messages.size)
            assertTrue(sendingState.messages.last().isUser)

            // 1000ms gecikmeyi sar
            advanceTimeBy(1100)
            runCurrent()

            // 2. Ara Durum: Sunucu yanıtı geldiğinde anlık kota bilgisi güncellenir (isLoading halen true)
            val quotaState = awaitItem()
            assertTrue(quotaState.isLoading)

            // 3. Nihai Sonuç Durumu: Sunucu yanıtı mesajlara eklendi, isLoading = false, toplam 3 mesaj var (welcome + user + ai)
            val finishedState = awaitItem()
            assertFalse(finishedState.isLoading)
            assertEquals(3, finishedState.messages.size)
            assertFalse(finishedState.messages.last().isUser)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========================================================================
    // 2. KOTA AŞIMI VE FALLBACK (YEREL MOTOR) SENARYOLARI
    // ========================================================================

    @Test
    fun `sunucu kotasi asildiginda yerel offline motor devreye girmeli ve fallbackUsed true olmalidir`() = runTest {
        // Sunucu kotası aşıldı simülasyonu:
        fakeAiApi.chatResponseResult = Result.success(
            AiChatServerResponse(
                result = null,
                quota = AiQuotaDto(isPremium = false, dailyRemaining = 0, dailyLimit = 1),
                quotaExceeded = true
            )
        )
        fakeOfflineAiManager.offlineResult = AiChatResult(
            intent = AiIntent.CHAT,
            replyText = "Cihaz içi asistan devrede: Görevinizi oluşturdum."
        )

        val viewModel = createViewModel()
        runCurrent()

        viewModel.onEvent(AiChatEvent.InputTextChanged("Kotam bittiğinde ne olur?"))
        viewModel.onEvent(AiChatEvent.SendMessage)
        runCurrent()

        // Assert:
        val finalState = viewModel.state.value
        assertFalse(finalState.isLoading)
        assertTrue(finalState.fallbackUsed)
        assertEquals(1, fakeOfflineAiManager.processMessageCallCount)
        assertEquals(3, finalState.messages.size)
        // Yerel yanıt kullanıcıya iletildi:
        val aiReply = (finalState.messages.last().text as UiText.DynamicString).value
        assertTrue(aiReply.contains("Cihaz içi asistan"))
    }

    // ========================================================================
    // 3. AĞ HATASINDA OTOMATİK YEREL MOTOR KURTARMASI (ERROR RECOVERY)
    // ========================================================================

    @Test
    fun `sunucuya baglanti koptugunda yerel motor otomatik kurtarma yapmali ve isLoading false olmalidir`() = runTest {
        // Ağ hatası simülasyonu:
        fakeAiApi.chatResponseResult = Result.failure(Exception("503 Service Unavailable"))
        fakeOfflineAiManager.offlineResult = AiChatResult(
            intent = AiIntent.CHAT,
            replyText = "Çevrimdışı mod: İnternet olmasa da buradayım."
        )

        val viewModel = createViewModel()
        runCurrent()

        viewModel.onEvent(AiChatEvent.InputTextChanged("İnternetsiz deneme"))
        viewModel.onEvent(AiChatEvent.SendMessage)
        runCurrent()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.fallbackUsed)
        assertEquals(1, fakeOfflineAiManager.processMessageCallCount)
        assertEquals(3, state.messages.size)
        val reply = (state.messages.last().text as UiText.DynamicString).value
        assertTrue(reply.contains("Çevrimdışı mod"))
    }

    // ========================================================================
    // 4. COROUTINE CANCELLATION (CANCEL GENERATION) SENARYOLARI
    // ========================================================================

    @Test
    fun `yanit uretilirken CancelGeneration tetiklendiginde coroutine aninda iptal edilmeli ve mesaj gelmemelidir`() = runTest {
        // Uzun süren (3 saniye) yapay zeka işlemi:
        fakeAiApi.simulateDelayMs = 3000L
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onEvent(AiChatEvent.InputTextChanged("Çok uzun bir analiz yap"))
        viewModel.onEvent(AiChatEvent.SendMessage)
        runCurrent()

        // 1. İşlem başladı ve arka planda çalışıyor:
        assertTrue(viewModel.state.value.isLoading)
        assertEquals(2, viewModel.state.value.messages.size) // Welcome + User

        // 1000 ms geçti (işlem henüz bitmedi)
        advanceTimeBy(1000)
        runCurrent()

        // 2. Act: Kullanıcı işlemi durdurdu (CancelGeneration):
        viewModel.onEvent(AiChatEvent.CancelGeneration)
        runCurrent()

        // Assert 1: Loading anında sıfırlandı
        assertFalse(viewModel.state.value.isLoading)

        // 3. Zamanı fazlasıyla (5000 ms) ileri sarıyoruz:
        advanceTimeBy(5000)
        runCurrent()

        // Assert 2: Job iptal edildiği için sunucu yanıt verse bile ASLA mesaja eklenmedi!
        assertEquals(2, viewModel.state.value.messages.size)
        assertTrue(viewModel.state.value.messages.last().isUser)
    }

    @Test
    fun `ClearChat tetiklendiginde tum mesajlar ve input guvenle sifirlanmalidir`() = runTest {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onEvent(AiChatEvent.InputTextChanged("Kalan metin"))
        viewModel.onEvent(AiChatEvent.SendMessage)
        runCurrent()

        assertEquals(3, viewModel.state.value.messages.size)

        // Temizle
        viewModel.onEvent(AiChatEvent.ClearChat)

        assertTrue(viewModel.state.value.messages.isEmpty())
        assertEquals("", viewModel.state.value.inputText)
    }

    // ========================================================================
    // 5. KOTA EKSİLMESİ VE KOTA TÜKENDİĞİNDE RULE-BASED ASİSTANA DÖNÜŞ TESTLERİ
    // ========================================================================

    @Test
    fun `her mesaj gonderildiginde kota 1 adet dusmeli ve hem state hem sessionPreferences guncellenmelidir`() = runTest {
        // 50/50 Premium kota simülasyonu
        val initialQuota = AiQuotaDto(
            isPremium = true,
            dailyRemaining = 50,
            dailyLimit = 50,
            weeklyRemaining = 300,
            weeklyLimit = 300
        )
        fakeAiApi.quotaResult = Result.success(initialQuota)
        fakeSessionPreferences.saveAiQuota(initialQuota)

        // Sunucu chat isteğine başarılı yanıt döner, ancak sunucu kotayı güncellememiş olsa bile client 1 adet düşer
        fakeAiApi.chatResponseResult = Result.success(
            AiChatServerResponse(
                result = AiChatResult(intent = AiIntent.CHAT, replyText = "Görev oluşturuldu."),
                quota = initialQuota
            )
        )

        val viewModel = createViewModel()
        runCurrent()

        assertEquals(50, viewModel.state.value.quota.dailyRemaining)

        // 1. Mesaj gönder
        viewModel.onEvent(AiChatEvent.InputTextChanged("1. mesajım"))
        viewModel.onEvent(AiChatEvent.SendMessage)
        runCurrent()

        assertEquals(49, viewModel.state.value.quota.dailyRemaining)
        assertEquals(49, fakeSessionPreferences.getAiQuota(true).dailyRemaining)

        // 2. Mesaj gönder
        viewModel.onEvent(AiChatEvent.InputTextChanged("2. mesajım"))
        viewModel.onEvent(AiChatEvent.SendMessage)
        runCurrent()

        assertEquals(48, viewModel.state.value.quota.dailyRemaining)
        assertEquals(48, fakeSessionPreferences.getAiQuota(true).dailyRemaining)
    }

    @Test
    fun `kota tukendiginde yapay zeka adimlari atlanmali kural tabanli motora gecilmeli ve uyari notu iletilmelidir`() = runTest {
        // Kotası tamamen bitmiş (0/50) durum simülasyonu
        val exhaustedQuota = AiQuotaDto(
            isPremium = true,
            dailyRemaining = 0,
            dailyLimit = 50,
            weeklyRemaining = 250,
            weeklyLimit = 300
        )
        fakeAiApi.quotaResult = Result.success(exhaustedQuota)
        fakeSessionPreferences.saveAiQuota(exhaustedQuota)

        fakeOfflineAiManager.offlineResult = AiChatResult(
            intent = AiIntent.CREATE_TASK,
            replyText = "Yarın için 'Fatura öde' görevi hazırlandı."
        )

        val viewModel = createViewModel()
        runCurrent()

        assertEquals(0, viewModel.state.value.quota.dailyRemaining)

        // Kota 0 iken mesaj gönder
        viewModel.onEvent(AiChatEvent.InputTextChanged("Yarın fatura öde"))
        viewModel.onEvent(AiChatEvent.SendMessage)
        runCurrent()

        val finalState = viewModel.state.value
        assertFalse(finalState.isLoading)
        assertTrue(finalState.fallbackUsed)

        // Sunucuya API isteği hiç atılmamalı, doğrudan yerel rule-based çağrılmalı
        assertEquals(0, fakeAiApi.sendChatMessageCallCount)
        assertEquals(1, fakeOfflineAiManager.processRuleBasedCallCount)

        // Mesaj içeriğinde uyarı notu bulunmalı
        val lastMessage = finalState.messages.last()
        val messageText = (lastMessage.text as UiText.DynamicString).value
        assertTrue(messageText.contains("Fatura öde"))
        assertTrue(messageText.contains("kural tabanlı asistan") || messageText.contains("kotanız"))

        // Kota negatif olmamalı, 0 kalmalı
        assertEquals(0, finalState.quota.dailyRemaining)
    }
}
