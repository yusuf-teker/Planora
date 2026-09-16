package com.yusufteker.planora.feature.auth.fakes

import com.yusufteker.planora.feature.auth.domain.repository.AuthRepository
import com.yusufteker.planora.shared.api.AuthRequest
import com.yusufteker.planora.shared.api.AuthResponse
import com.yusufteker.planora.shared.api.RegisterRequest

/**
 * Testlerde gerçek sunucu yerine kullanılan sahte (Fake) AuthRepository.
 */
class FakeAuthRepository(
    var shouldSucceed: Boolean = true,
    var wasLoginCalled: Boolean = false,
    var wasRegisterCalled: Boolean = false,
    var wasSendCodeCalled: Boolean = false
) : AuthRepository {

    override suspend fun login(request: AuthRequest): Result<AuthResponse> {
        wasLoginCalled = true
        return if (shouldSucceed) {
            Result.success(
                AuthResponse(
                    accessToken = "fake_access_token",
                    refreshToken = "fake_refresh_token",
                    userId = 1,
                    name = "Yusuf Teker",
                    username = "yusuf",
                    avatarId = "avatar_1"
                )
            )
        } else {
            Result.failure(Exception("Giriş başarısız"))
        }
    }

    override suspend fun register(request: RegisterRequest): Result<AuthResponse> {
        wasRegisterCalled = true
        return if (shouldSucceed) {
            Result.success(
                AuthResponse(
                    accessToken = "fake_access_token",
                    refreshToken = "fake_refresh_token",
                    userId = 1,
                    name = request.name,
                    username = request.username,
                    avatarId = "avatar_1"
                )
            )
        } else {
            Result.failure(Exception("Kayıt başarısız"))
        }
    }

    override suspend fun sendRegisterCode(email: String, username: String): Result<Unit> {
        wasSendCodeCalled = true
        return if (shouldSucceed) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Kod gönderilemedi"))
        }
    }

    override suspend fun hasValidSession(): Boolean = true
    override suspend fun logout() {}
    override suspend fun updateProfile(name: String, avatarId: String): Result<Unit> = Result.success(Unit)
    override suspend fun fetchMyProfile(): Result<Unit> = Result.success(Unit)
    override suspend fun registerFcmToken(token: String): Result<Unit> = Result.success(Unit)
    override suspend fun forgotPassword(email: String): Result<Unit> = Result.success(Unit)
    override suspend fun resetPassword(email: String, code: String, newPassword: String): Result<Unit> = Result.success(Unit)
}
