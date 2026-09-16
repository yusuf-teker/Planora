package com.yusufteker.planora.feature.auth.presentation.login

import com.yusufteker.planora.core.analytics.AnalyticsManager
import com.yusufteker.planora.feature.auth.domain.repository.AuthRepository
import com.yusufteker.planora.feature.auth.domain.usecase.LoginUseCase
import com.yusufteker.planora.shared.api.AuthRequest
import com.yusufteker.planora.shared.api.AuthResponse
import com.yusufteker.planora.shared.api.RegisterRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

import com.yusufteker.planora.feature.auth.fakes.FakeAuthRepository

/**
 * Pulse uygulamasının gerçek LoginViewModel sınıfını test eden birim testi.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(fakeRepo: FakeAuthRepository = FakeAuthRepository()): LoginViewModel {
        val loginUseCase = LoginUseCase(fakeRepo)
        val analyticsManager = AnalyticsManager()
        return LoginViewModel(loginUseCase, analyticsManager)
    }

    @Test
    fun `baslangicta giris ekrani state varsayilan bos degerlerde olmalidir`() {
        // Arrange (Hazırlık)
        val viewModel = createViewModel()

        // Assert (Doğrulama)
        assertEquals("", viewModel.state.value.identifier)
        assertEquals("", viewModel.state.value.password)
        assertFalse(viewModel.state.value.isPasswordVisible)
        assertFalse(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.identifierError)
        assertNull(viewModel.state.value.passwordError)
    }

    @Test
    fun `IdentifierChanged eventi calistiginda identifier guncellenmeli ve hata sifirlanmalidir`() {
        // Arrange
        val viewModel = createViewModel()

        // Act (Eylem)
        viewModel.onEvent(LoginEvent.IdentifierChanged("yusuf@planora.com"))

        // Assert
        assertEquals("yusuf@planora.com", viewModel.state.value.identifier)
        assertNull(viewModel.state.value.identifierError)
    }

    @Test
    fun `PasswordChanged eventi calistiginda sifre guncellenmelidir`() {
        // Arrange
        val viewModel = createViewModel()

        // Act
        viewModel.onEvent(LoginEvent.PasswordChanged("gucluSifre123"))

        // Assert
        assertEquals("gucluSifre123", viewModel.state.value.password)
        assertNull(viewModel.state.value.passwordError)
    }

    @Test
    fun `TogglePasswordVisibility eventi sifre gorunurlugunu acip kapatmalidir`() {
        // Arrange
        val viewModel = createViewModel()
        assertFalse(viewModel.state.value.isPasswordVisible)

        // Act 1: Aç
        viewModel.onEvent(LoginEvent.TogglePasswordVisibility)
        // Assert 1
        assertTrue(viewModel.state.value.isPasswordVisible)

        // Act 2: Kapat
        viewModel.onEvent(LoginEvent.TogglePasswordVisibility)
        // Assert 2
        assertFalse(viewModel.state.value.isPasswordVisible)
    }

    @Test
    fun `bos form ile giris yapilmaya calisildiginda identifier ve password hatalari set edilmelidir`() {
        // Arrange: E-posta ve şifre boş bırakıldı
        val viewModel = createViewModel()

        // Act: Giriş butonuna tıklandı
        viewModel.onEvent(LoginEvent.LoginClicked)

        // Assert: Form validasyonundan geçmemeli ve error alanları dolmalı
        assertNotNull(viewModel.state.value.identifierError, "E-posta alanı boşken hata mesajı atanmalıdır")
        assertNotNull(viewModel.state.value.passwordError, "Şifre alanı boşken hata mesajı atanmalıdır")
        assertFalse(viewModel.state.value.isLoading, "Hatalı formda loading başlamamalıdır")
    }

    @Test
    fun `ClearForm eventi cagrildiginda tum state ilk haline donmelidir`() {
        // Arrange: Kullanıcı alanları doldurdu
        val viewModel = createViewModel()
        viewModel.onEvent(LoginEvent.IdentifierChanged("test@planora.com"))
        viewModel.onEvent(LoginEvent.PasswordChanged("123456"))
        viewModel.onEvent(LoginEvent.TogglePasswordVisibility)

        // Act: Formu temizle
        viewModel.onEvent(LoginEvent.ClearForm)

        // Assert: Tertemiz ilk haline dönmeli
        assertEquals("", viewModel.state.value.identifier)
        assertEquals("", viewModel.state.value.password)
        assertFalse(viewModel.state.value.isPasswordVisible)
    }

    // ========================================================================
    // 2. KONU TESTİ: Coroutine Testi (runTest & advanceUntilIdle)
    // ========================================================================

    @Test
    fun `gecerli bilgilerle giris butonuna basildiginda once loading baslamali ve islem bitince loading durmalidir`() = runTest {
        // 1. Arrange: Geçerli e-posta ve şifre girildi
        val viewModel = createViewModel()
        viewModel.onEvent(LoginEvent.IdentifierChanged("yusuf@planora.com"))
        viewModel.onEvent(LoginEvent.PasswordChanged("123456"))

        // 2. Act: Giriş Yap'a tıklandı (launch ile coroutine başlar)
        viewModel.onEvent(LoginEvent.LoginClicked)

        // Butona tıklandığı an loading = true olmalıdır:
        assertTrue(viewModel.state.value.isLoading, "İstek başladığı anda isLoading true olmalıdır")

        // 3. advanceUntilIdle(): Arka plandaki coroutine tamamlanana kadar sanal zamanı ilerlet:
        advanceUntilIdle()

        // 4. Assert: İşlem tamamlandığında loading tekrar false olmalıdır
        assertFalse(viewModel.state.value.isLoading, "İstek bittiğinde isLoading tekrar false olmalıdır")
    }
}
