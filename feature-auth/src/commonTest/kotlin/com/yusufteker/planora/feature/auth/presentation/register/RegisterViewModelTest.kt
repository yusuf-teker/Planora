package com.yusufteker.planora.feature.auth.presentation.register

import com.yusufteker.planora.feature.auth.domain.usecase.RegisterUseCase
import com.yusufteker.planora.feature.auth.domain.usecase.SendRegisterCodeUseCase
import com.yusufteker.planora.feature.auth.fakes.FakeAuthRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pulse uygulamasının RegisterViewModel (Kayıt Ekranı) sınıfını
 * test eden birim testi.
 *
 * Amaç: Ekran açılış durumu, form validasyonları (isim, kullanıcı adı, e-posta,
 * şifre uzunluğu, şifre eşleşmesi) ve state sıfırlama mekanizmasını
 * cihaz/emülatör olmadan doğrulamak.
 */
class RegisterViewModelTest {

    private fun createViewModel(): RegisterViewModel {
        val fakeRepo = FakeAuthRepository()
        val registerUseCase = RegisterUseCase(fakeRepo)
        val sendCodeUseCase = SendRegisterCodeUseCase(fakeRepo)
        return RegisterViewModel(registerUseCase, sendCodeUseCase)
    }

    @Test
    fun `baslangicta tum alanlar bos ve hata mesajlari null olmalidir`() {
        // 1. Arrange: ViewModel'i oluştur
        val viewModel = createViewModel()

        // 2. Assert: Başlangıç state'ini doğrula
        assertEquals("", viewModel.state.value.name)
        assertEquals("", viewModel.state.value.username)
        assertEquals("", viewModel.state.value.email)
        assertEquals("", viewModel.state.value.password)
        assertEquals("", viewModel.state.value.confirmPassword)
        assertEquals("", viewModel.state.value.code)
        assertFalse(viewModel.state.value.isCodeSent)
        assertFalse(viewModel.state.value.isLoading)
        assertFalse(viewModel.state.value.isPasswordVisible)
        assertNull(viewModel.state.value.nameError)
        assertNull(viewModel.state.value.usernameError)
        assertNull(viewModel.state.value.emailError)
        assertNull(viewModel.state.value.passwordError)
        assertNull(viewModel.state.value.confirmPasswordError)
    }

    @Test
    fun `ad ve kullanici adi girildiginde state guncellenmeli ve hatalar temizlenmelidir`() {
        // Arrange
        val viewModel = createViewModel()

        // Act: Kullanıcı adını ve kullanıcı adını yazdı
        viewModel.onEvent(RegisterEvent.NameChanged("Yusuf Teker"))
        viewModel.onEvent(RegisterEvent.UsernameChanged("yusufteker"))

        // Assert
        assertEquals("Yusuf Teker", viewModel.state.value.name)
        assertEquals("yusufteker", viewModel.state.value.username)
        assertNull(viewModel.state.value.nameError)
        assertNull(viewModel.state.value.usernameError)
    }

    @Test
    fun `sifre ve sifre tekrari girildiginde state guncellenmelidir`() {
        // Arrange
        val viewModel = createViewModel()

        // Act
        viewModel.onEvent(RegisterEvent.PasswordChanged("GizliSifre123"))
        viewModel.onEvent(RegisterEvent.ConfirmPasswordChanged("GizliSifre123"))

        // Assert
        assertEquals("GizliSifre123", viewModel.state.value.password)
        assertEquals("GizliSifre123", viewModel.state.value.confirmPassword)
        assertNull(viewModel.state.value.passwordError)
        assertNull(viewModel.state.value.confirmPasswordError)
    }

    @Test
    fun `ad alani bosken kod gonderilmeye calisildiginda nameError atanmali ve loading baslamamalidir`() {
        // Arrange: İsim boş, diğer alanlar dolu
        val viewModel = createViewModel()
        viewModel.onEvent(RegisterEvent.UsernameChanged("yusuf"))
        viewModel.onEvent(RegisterEvent.EmailChanged("yusuf@planora.com"))
        viewModel.onEvent(RegisterEvent.PasswordChanged("123456"))
        viewModel.onEvent(RegisterEvent.ConfirmPasswordChanged("123456"))

        // Act: Kod Gönder butonuna basıldı
        viewModel.onEvent(RegisterEvent.SendCodeClicked)

        // Assert
        assertNotNull(viewModel.state.value.nameError, "Ad alanı boşken hata atanmalıdır")
        assertFalse(viewModel.state.value.isLoading, "Validasyon geçmediğinde loading başlamamalıdır")
    }

    @Test
    fun `gecersiz eposta girildiginde emailError atanmalidir`() {
        // Arrange: E-postada '@' karakteri yok
        val viewModel = createViewModel()
        viewModel.onEvent(RegisterEvent.NameChanged("Yusuf"))
        viewModel.onEvent(RegisterEvent.UsernameChanged("yusuf"))
        viewModel.onEvent(RegisterEvent.EmailChanged("gecersizepostam"))
        viewModel.onEvent(RegisterEvent.PasswordChanged("123456"))
        viewModel.onEvent(RegisterEvent.ConfirmPasswordChanged("123456"))

        // Act
        viewModel.onEvent(RegisterEvent.SendCodeClicked)

        // Assert
        assertNotNull(viewModel.state.value.emailError, "Geçersiz e-postada emailError atanmalıdır")
    }

    @Test
    fun `sifre 6 karakterden kisa girildiginde passwordError atanmalidir`() {
        // Arrange: Şifre 5 karakter
        val viewModel = createViewModel()
        viewModel.onEvent(RegisterEvent.NameChanged("Yusuf"))
        viewModel.onEvent(RegisterEvent.UsernameChanged("yusuf"))
        viewModel.onEvent(RegisterEvent.EmailChanged("yusuf@planora.com"))
        viewModel.onEvent(RegisterEvent.PasswordChanged("12345"))
        viewModel.onEvent(RegisterEvent.ConfirmPasswordChanged("12345"))

        // Act
        viewModel.onEvent(RegisterEvent.SendCodeClicked)

        // Assert
        assertNotNull(viewModel.state.value.passwordError, "6 karakterden kısa şifrede hata atanmalıdır")
    }

    @Test
    fun `sifreler eslesmediginde confirmPasswordError atanmalidir`() {
        // Arrange: Şifre ile şifre tekrarı farklı
        val viewModel = createViewModel()
        viewModel.onEvent(RegisterEvent.NameChanged("Yusuf"))
        viewModel.onEvent(RegisterEvent.UsernameChanged("yusuf"))
        viewModel.onEvent(RegisterEvent.EmailChanged("yusuf@planora.com"))
        viewModel.onEvent(RegisterEvent.PasswordChanged("123456"))
        viewModel.onEvent(RegisterEvent.ConfirmPasswordChanged("654321"))

        // Act
        viewModel.onEvent(RegisterEvent.SendCodeClicked)

        // Assert
        assertNotNull(viewModel.state.value.confirmPasswordError, "Şifreler uyuşmadığında hata atanmalıdır")
    }

    @Test
    fun `dogrulama kodu 6 haneli degilken RegisterClicked cagrildiginda codeError atanmalidir`() {
        // Arrange: Kod 3 haneli
        val viewModel = createViewModel()
        viewModel.onEvent(RegisterEvent.CodeChanged("123"))

        // Act: Kayıt Ol tıklandı
        viewModel.onEvent(RegisterEvent.RegisterClicked)

        // Assert
        assertNotNull(viewModel.state.value.codeError, "6 haneli olmayan kodda hata atanmalıdır")
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `TogglePasswordVisibility eventi sifre gorunurlugunu degistirmelidir`() {
        // Arrange
        val viewModel = createViewModel()
        assertFalse(viewModel.state.value.isPasswordVisible)

        // Act 1: Aç
        viewModel.onEvent(RegisterEvent.TogglePasswordVisibility)
        assertTrue(viewModel.state.value.isPasswordVisible)

        // Act 2: Kapat
        viewModel.onEvent(RegisterEvent.TogglePasswordVisibility)
        assertFalse(viewModel.state.value.isPasswordVisible)
    }

    @Test
    fun `BackToFormClicked cagrildiginda isCodeSent false olmali ve kod alani temizlenmelidir`() {
        // Arrange
        val viewModel = createViewModel()
        viewModel.onEvent(RegisterEvent.CodeChanged("123456"))

        // Act
        viewModel.onEvent(RegisterEvent.BackToFormClicked)

        // Assert
        assertFalse(viewModel.state.value.isCodeSent)
        assertEquals("", viewModel.state.value.code)
        assertNull(viewModel.state.value.codeError)
    }

    @Test
    fun `ClearForm cagrildiginda tum state fabrika ayarlarina donmelidir`() {
        // Arrange: Formu doldur
        val viewModel = createViewModel()
        viewModel.onEvent(RegisterEvent.NameChanged("Yusuf"))
        viewModel.onEvent(RegisterEvent.UsernameChanged("yusuf"))
        viewModel.onEvent(RegisterEvent.EmailChanged("yusuf@planora.com"))

        // Act
        viewModel.onEvent(RegisterEvent.ClearForm)

        // Assert
        assertEquals("", viewModel.state.value.name)
        assertEquals("", viewModel.state.value.username)
        assertEquals("", viewModel.state.value.email)
    }
}
