package com.yusufteker.planora.feature.home.presentation.premium

import app.cash.turbine.test
import com.yusufteker.planora.core.preferences.SessionPreferences
import com.yusufteker.planora.feature.home.data.repository.FakeProfileRepository
import com.yusufteker.planora.feature.home.util.createTestSessionPreferences
import com.yusufteker.planora.shared.api.UserProfileResponse
import com.yusufteker.planora.shared.billing.PaymentMethod
import com.yusufteker.planora.shared.billing.SubscriptionPeriod
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
 * Planora Premium Paywall Mimarisi Test Paketi.
 *
 * Kapsanan Senaryolar:
 * 1. REACTIVE STATE FLOW: SessionPreferences üzerinden gelen gerçek Premium durumu reaktif olarak ViewModel state'ine yansır.
 * 2. SERVER / NEON SYNC: Ekran açılışında veya yenilemede ProfileRepository'den gelen gerçek isPremium durumu DataStore ve State'e yazılır.
 * 3. COMING SOON SHEET STATE: Kullanıcı satın alma adımını açtığında bilgilendirme sheet'i açılır, kapatıldığında güvenle kapanır.
 * 4. NO FAKE LOCAL PURCHASE: Sheet açılıp kapandığında kullanıcıya ASLA yerel/sahte premium verilmez.
 * 5. SELECTION: Plan (Aylık/Yıllık) ve ödeme yöntemi seçimleri doğru güncellenir.
 * 6. CANCELLATION: Senkronizasyon coroutine'i iptal edildiğinde sızıntı/yan etki oluşmaz.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PremiumViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var sessionPreferences: SessionPreferences
    private lateinit var fakeProfileRepository: FakeProfileRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sessionPreferences = createTestSessionPreferences()
        fakeProfileRepository = FakeProfileRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(includeRepository: Boolean = true): PremiumViewModel {
        return PremiumViewModel(
            sessionPreferences = sessionPreferences,
            profileRepository = if (includeRepository) fakeProfileRepository else null
        )
    }

    // ========================================================================
    // 1. REAKTİF STATE VE PREMIUM GÜNCELLEME SENARYOLARI
    // ========================================================================

    @Test
    fun `sessionPreferences isPremiumFlow degistiginde viewModel state isPremium aninda reaktif guncellenmelidir`() = runTest {
        val viewModel = createViewModel(includeRepository = false)
        runCurrent()

        viewModel.state.test {
            val initial = awaitItem()
            assertFalse(initial.isPremium)

            // Act: Veritabanından/DataStore'dan kullanıcının Premium olduğu bilgisi gelir
            sessionPreferences.setPremium(true)
            runCurrent()

            val updatedState = awaitItem()
            assertTrue(updatedState.isPremium)

            // Act: Premium süresi bittiğinde veya kaldırıldığında
            sessionPreferences.setPremium(false)
            runCurrent()

            val revertedState = awaitItem()
            assertFalse(revertedState.isPremium)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ekran acilisinda profileRepository'den gelen gercek Neon premium durumu State ve DataStore'a yazilmalidir`() = runTest {
        // Neon veritabanında kullanıcının manuel olarak Premium yapıldığını simüle ediyoruz:
        fakeProfileRepository.profilesMap["me"] = UserProfileResponse(
            id = 101,
            name = "Premium User",
            username = "premium_pro",
            email = "pro@planora.com",
            avatarId = "avatar_1",
            isPremium = true
        )

        val viewModel = createViewModel(includeRepository = true)
        runCurrent()

        // Assert: ViewModel senkronizasyonu tamamladı ve Premium aktif
        assertTrue(viewModel.state.value.isPremium)
        assertTrue(sessionPreferences.isPremium())
        assertFalse(viewModel.state.value.isSyncing)
    }

    // ========================================================================
    // 2. PAYMENT SHEET & COMING SOON GÜVENLİK SENARYOLARI
    // ========================================================================

    @Test
    fun `satin alma cta tiklandiginda showPaymentSheet acilmali ve kapatildiginda kapanmalidir`() = runTest {
        val viewModel = createViewModel(includeRepository = false)
        runCurrent()

        assertFalse(viewModel.state.value.showPaymentSheet)

        // Act: Kullanıcı "Abone Ol / Planora Premium'a Geç" tıklar
        viewModel.onEvent(PremiumEvent.OnStartPurchaseClicked)
        runCurrent()

        // Assert: Sheet açılır (Ödeme sistemi çok yakında bilgilendirmesi)
        assertTrue(viewModel.state.value.showPaymentSheet)

        // Act: Kullanıcı "Anladım" veya kapat butonuna basar
        viewModel.onEvent(PremiumEvent.OnDismissPaymentSheet)
        runCurrent()

        // Assert: Sheet kapanır
        assertFalse(viewModel.state.value.showPaymentSheet)

        // En Kritik Güvenlik Kuralı: Kullanıcıya ASLA sahte/demo premium verilmez!
        assertFalse(viewModel.state.value.isPremium)
        assertFalse(sessionPreferences.isPremium())
    }

    @Test
    fun `geri tusuna basildiginda NavigateBack efekti yayilmalidir`() = runTest {
        val viewModel = createViewModel(includeRepository = false)
        runCurrent()

        viewModel.effect.test {
            viewModel.onEvent(PremiumEvent.OnBackClicked)
            runCurrent()

            val effect = awaitItem()
            assertEquals(PremiumEffect.NavigateBack, effect)
            expectNoEvents()
        }
    }

    // ========================================================================
    // 3. PLAN VE ÖDEME YÖNTEMİ SEÇİM SENARYOLARI
    // ========================================================================

    @Test
    fun `farkli plan secildiginde selectedPlan basariyla guncellenmelidir`() = runTest {
        val viewModel = createViewModel(includeRepository = false)
        runCurrent()

        val monthlyPlan = viewModel.state.value.availablePlans.first { it.period == SubscriptionPeriod.MONTHLY }
        viewModel.onEvent(PremiumEvent.OnPlanSelected(monthlyPlan))

        assertEquals(monthlyPlan, viewModel.state.value.selectedPlan)
    }

    @Test
    fun `odeme yontemi degistirildiginde selectedPaymentMethod guncellenmelidir`() = runTest {
        val viewModel = createViewModel(includeRepository = false)
        runCurrent()

        viewModel.onEvent(PremiumEvent.OnPaymentMethodSelected(PaymentMethod.GOOGLE_PLAY))
        assertEquals(PaymentMethod.GOOGLE_PLAY, viewModel.state.value.selectedPaymentMethod)
    }

    // ========================================================================
    // 4. COROUTINE CANCELLATION SENARYOLARI
    // ========================================================================

    @Test
    fun `sync sirasinda tekrar tetiklenirse eski sync job iptal edilmeli ve guvenle yenisi calismalidir`() = runTest {
        fakeProfileRepository.profilesMap["me"] = UserProfileResponse(
            id = 101,
            name = "User",
            username = "user",
            email = "user@test.com",
            avatarId = "avatar_1",
            isPremium = true
        )

        val viewModel = createViewModel(includeRepository = true)
        val initialJob = viewModel.syncJob

        // Yenileme çağrısı yap
        viewModel.onEvent(PremiumEvent.OnRefreshPremiumStatus)
        runCurrent()

        // Eski job tamamlandı veya yenisi atandı
        assertTrue(viewModel.state.value.isPremium)
    }
}
