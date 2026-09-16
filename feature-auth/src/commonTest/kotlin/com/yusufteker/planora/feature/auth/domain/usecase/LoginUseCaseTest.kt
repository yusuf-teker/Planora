package com.yusufteker.planora.feature.auth.domain.usecase

import com.yusufteker.planora.feature.auth.domain.repository.AuthRepository
import com.yusufteker.planora.shared.api.AuthRequest
import com.yusufteker.planora.shared.api.AuthResponse
import com.yusufteker.planora.shared.api.RegisterRequest
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Repository'nin çağrılıp çağrılmadığını takip eden sahte repository.
 */
private class MockableAuthRepository : AuthRepository {
    var wasLoginCalled = false
    var responseToReturn: Result<AuthResponse> = Result.success(
        AuthResponse(
            accessToken = "access_123",
            refreshToken = "refresh_123",
            userId = 10,
            name = "Ahmet Yılmaz",
            username = "ahmetyilmaz",
            avatarId = "avatar_2"
        )
    )

    override suspend fun login(request: AuthRequest): Result<AuthResponse> {
        wasLoginCalled = true
        return responseToReturn
    }

    override suspend fun register(request: RegisterRequest): Result<AuthResponse> = responseToReturn
    override suspend fun hasValidSession(): Boolean = true
    override suspend fun logout() {}
    override suspend fun updateProfile(name: String, avatarId: String): Result<Unit> = Result.success(Unit)
    override suspend fun fetchMyProfile(): Result<Unit> = Result.success(Unit)
    override suspend fun registerFcmToken(token: String): Result<Unit> = Result.success(Unit)
    override suspend fun forgotPassword(email: String): Result<Unit> = Result.success(Unit)
    override suspend fun sendRegisterCode(email: String, username: String): Result<Unit> = Result.success(Unit)
    override suspend fun resetPassword(email: String, code: String, newPassword: String): Result<Unit> = Result.success(Unit)
}

/**
 * Pulse uygulamasının LoginUseCase iş mantığını test eden birim testi.
 * 
 * Amaç: Boş alan kontrolünü ve repository çağrısını cihaz/emülatör olmadan doğrulamak.
 */
class LoginUseCaseTest {

    @Test
    fun `bos eposta girildiginde repository cagrilmadan hata donmelidir`() = runBlocking {
        // 1. Arrange (Hazırlık)
        val fakeRepo = MockableAuthRepository()
        val useCase = LoginUseCase(fakeRepo)
        val request = AuthRequest(identifier = "", password = "password123")

        // 2. Act (Eylem)
        val result = useCase(request)

        // 3. Assert (Doğrulama)
        assertTrue(result.isFailure, "Boş e-posta ile istek başarısız olmalıdır")
        assertFalse(fakeRepo.wasLoginCalled, "Boş e-postada sunucuya/repository'ye istek atılmamalıdır")
    }

    @Test
    fun `bos sifre girildiginde repository cagrilmadan hata donmelidir`() = runBlocking {
        // Arrange
        val fakeRepo = MockableAuthRepository()
        val useCase = LoginUseCase(fakeRepo)
        val request = AuthRequest(identifier = "yusuf@planora.com", password = "")

        // Act
        val result = useCase(request)

        // Assert
        assertTrue(result.isFailure, "Boş şifre ile istek başarısız olmalıdır")
        assertFalse(fakeRepo.wasLoginCalled, "Boş şifrede repository çağrılmamalıdır")
    }

    @Test
    fun `gecerli bilgiler girildiginde repository cagrilmali ve basarili sonuc donmelidir`() = runBlocking {
        // Arrange
        val fakeRepo = MockableAuthRepository()
        val useCase = LoginUseCase(fakeRepo)
        val request = AuthRequest(identifier = "yusuf@planora.com", password = "secretPassword")

        // Act
        val result = useCase(request)

        // Assert
        assertTrue(result.isSuccess, "Geçerli bilgilerde işlem başarılı olmalıdır")
        assertTrue(fakeRepo.wasLoginCalled, "Repository login metodu çağrılmış olmalıdır")
        assertEquals(10, result.getOrNull()?.userId)
        assertEquals("Ahmet Yılmaz", result.getOrNull()?.name)
    }
}
