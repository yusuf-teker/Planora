package com.yusufteker.planora.feature.auth.data.repository

import io.github.aakira.napier.Napier
import com.yusufteker.planora.core.preferences.SessionPreferences
import com.yusufteker.planora.feature.auth.domain.repository.AuthRepository
import com.yusufteker.planora.shared.api.AuthRequest
import com.yusufteker.planora.shared.api.AuthResponse
import com.yusufteker.planora.shared.api.RegisterRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import com.yusufteker.planora.core.database.clearAll
import com.yusufteker.planora.core.network.parseApiException

import com.yusufteker.planora.core.database.PlanoraDatabase

/**
 * Implementation of [AuthRepository] that communicates with the Ktor Backend.
 */
class AuthRepositoryImpl(
    private val httpClient: HttpClient,
    private val sessionPreferences: SessionPreferences,
    private val planoraDatabase: PlanoraDatabase,
    private val registerFcmTokenUseCase: com.yusufteker.planora.core.domain.usecase.RegisterFcmTokenUseCase? = null
) : AuthRepository {

    override suspend fun login(request: AuthRequest): Result<AuthResponse> {
        return try {
            // Sunucuya POST isteği gönderiyoruz.
            val response: AuthResponse = httpClient.post("auth/login") {
                setBody(request) // request objesini otomatik JSON'a çevirir (ContentNegotiation eklentisi sayesinde)
            }.body()

            // Sadece farklı bir kullanıcı giriş yaparsa eski veritabanını temizle
            val lastLoggedUserId = sessionPreferences.getLastLoggedUserId()
            if (lastLoggedUserId != null && lastLoggedUserId != response.userId.toString()) {
                planoraDatabase.planoraDatabaseQueries.clearAll()
            }
            sessionPreferences.setLastLoggedUserId(response.userId.toString())
            Napier.d(tag = "Screen", message = { "Login OK | isim: '${response.name}', avatar: '${response.avatarId}'" })
            sessionPreferences.saveTokens(response.accessToken, response.refreshToken)
            sessionPreferences.saveUserProfile(response.userId.toString(), response.name, response.avatarId, response.profileImageUrl, username = response.username)
            Napier.d(tag = "Screen", message = { "DataStore'a kaydedildi: '${response.name}'" })
            
            try {
                registerFcmTokenUseCase?.invoke()
            } catch (e: Exception) {
                Napier.e("Failed to register FCM token after login", e)
            }
            
            Result.success(response)
        } catch (e: Exception) {
            // Ağ hatası, yanlış şifre (401) veya sunucu kapalıysa (500) hata olarak döner.
            val parsed = e.parseApiException()
            Napier.e("Login request failed with exception: ${parsed.message}", parsed, tag = "HTTP_LOG")
            Result.failure(parsed)
        }
    }

    override suspend fun register(request: RegisterRequest): Result<AuthResponse> {
        return try {
            val response: AuthResponse = httpClient.post("auth/register") {
                setBody(request)
            }.body()
            val lastLoggedUserId = sessionPreferences.getLastLoggedUserId()
            if (lastLoggedUserId != null && lastLoggedUserId != response.userId.toString()) {
                planoraDatabase.planoraDatabaseQueries.clearAll()
            }
            sessionPreferences.setLastLoggedUserId(response.userId.toString())
            sessionPreferences.saveTokens(response.accessToken, response.refreshToken)
            sessionPreferences.saveUserProfile(response.userId.toString(), response.name, response.avatarId, response.profileImageUrl, username = response.username)
            
            try {
                registerFcmTokenUseCase?.invoke()
            } catch (e: Exception) {
                Napier.e("Failed to register FCM token after register", e)
            }
            
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e.parseApiException())
        }
    }

    override suspend fun hasValidSession(): Boolean {
        // En basit kontrol: Eğer refresh token varsa oturum açılmış demektir.
        // Token'ın gerçekten geçerli olup olmadığı Backend'e yapılacak ilk protected istekte (Ktor Auth ile) anlaşılacaktır.
        return sessionPreferences.getRefreshToken() != null
    }

    override suspend fun logout() {
        sessionPreferences.clearSession()
    }

    override suspend fun updateProfile(name: String, avatarId: String): Result<Unit> {
        return try {
            httpClient.put("auth/profile") {
                setBody(com.yusufteker.planora.shared.api.UpdateProfileRequest(name, avatarId))
            }
            // Update local DataStore upon successful server update
            val currentUserId = sessionPreferences.getUserId() ?: "guest"
            val currentProfileImageUrl = sessionPreferences.getUserProfileImageUrl()
            val currentUsername = sessionPreferences.getUserHandle()
            sessionPreferences.saveUserProfile(currentUserId, name, avatarId, currentProfileImageUrl, username = currentUsername)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e.parseApiException())
        }
    }

    override suspend fun fetchMyProfile(): Result<Unit> {
        return try {
            val profile = httpClient.get("auth/me").body<com.yusufteker.planora.shared.api.UserProfileResponse>()
            sessionPreferences.saveUserProfile(profile.id.toString(), profile.name, profile.avatarId, profile.profileImageUrl, profile.followersCount, profile.followingCount, username = profile.username)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e.parseApiException())
        }
    }

    override suspend fun registerFcmToken(token: String): Result<Unit> {
        return try {
            sessionPreferences.saveFcmToken(token)
            if (registerFcmTokenUseCase != null) {
                registerFcmTokenUseCase.invoke()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e.parseApiException())
        }
    }

    override suspend fun forgotPassword(email: String): Result<Unit> {
        return try {
            httpClient.post("auth/forgot-password") {
                setBody(com.yusufteker.planora.shared.api.ForgotPasswordRequest(email))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            val parsed = e.parseApiException()
            Napier.e("ForgotPassword request failed: ${parsed.message}", parsed, tag = "HTTP_LOG")
            Result.failure(parsed)
        }
    }

    override suspend fun sendRegisterCode(email: String, username: String): Result<Unit> {
        return try {
            httpClient.post("auth/send-register-code") {
                setBody(com.yusufteker.planora.shared.api.SendRegisterCodeRequest(email, username))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            val parsed = e.parseApiException()
            Napier.e("SendRegisterCode request failed: ${parsed.message}", parsed, tag = "HTTP_LOG")
            Result.failure(parsed)
        }
    }

    override suspend fun resetPassword(email: String, code: String, newPassword: String): Result<Unit> {
        return try {
            httpClient.post("auth/reset-password") {
                setBody(com.yusufteker.planora.shared.api.ResetPasswordRequest(email, code, newPassword))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            val parsed = e.parseApiException()
            Napier.e("ResetPassword request failed: ${parsed.message}", parsed, tag = "HTTP_LOG")
            Result.failure(parsed)
        }
    }
}



