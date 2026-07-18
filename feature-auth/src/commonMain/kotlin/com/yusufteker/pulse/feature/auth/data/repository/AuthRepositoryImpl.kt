package com.yusufteker.pulse.feature.auth.data.repository

import io.github.aakira.napier.Napier
import com.yusufteker.pulse.core.preferences.SessionPreferences
import com.yusufteker.pulse.feature.auth.domain.repository.AuthRepository
import com.yusufteker.pulse.shared.api.AuthRequest
import com.yusufteker.pulse.shared.api.AuthResponse
import com.yusufteker.pulse.shared.api.RegisterRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import com.yusufteker.pulse.core.database.clearAll

import com.yusufteker.pulse.core.database.PulsyDatabase

/**
 * Implementation of [AuthRepository] that communicates with the Ktor Backend.
 */
class AuthRepositoryImpl(
    private val httpClient: HttpClient,
    private val sessionPreferences: SessionPreferences,
    private val pulsyDatabase: PulsyDatabase
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
                pulsyDatabase.pulsyDatabaseQueries.clearAll()
            }
            sessionPreferences.setLastLoggedUserId(response.userId.toString())
            Napier.d(tag = "Screen", message = { "Login OK | isim: '${response.name}', avatar: '${response.avatarId}'" })
            sessionPreferences.saveTokens(response.accessToken, response.refreshToken)
            sessionPreferences.saveUserProfile(response.userId.toString(), response.name, response.avatarId, response.profileImageUrl)
            Napier.d(tag = "Screen", message = { "DataStore'a kaydedildi: '${response.name}'" })
            
            val fcmToken = sessionPreferences.getFcmToken()
            if (fcmToken != null) {
                try {
                    httpClient.post("fcm/register") {
                        setBody(com.yusufteker.pulse.shared.api.RegisterFcmTokenRequest(fcmToken, "android"))
                    }
                } catch (e: Exception) {
                    Napier.e("Failed to register FCM token after login", e)
                }
            }
            
            Result.success(response)
        } catch (e: Exception) {
            // Ağ hatası, yanlış şifre (401) veya sunucu kapalıysa (500) hata olarak döner.
            Napier.e("Login request failed with exception: ${e.message}", e, tag = "HTTP_LOG")
            Result.failure(e)
        }
    }

    override suspend fun register(request: RegisterRequest): Result<AuthResponse> {
        return try {
            val response: AuthResponse = httpClient.post("auth/register") {
                setBody(request)
            }.body()
            val lastLoggedUserId = sessionPreferences.getLastLoggedUserId()
            if (lastLoggedUserId != null && lastLoggedUserId != response.userId.toString()) {
                pulsyDatabase.pulsyDatabaseQueries.clearAll()
            }
            sessionPreferences.setLastLoggedUserId(response.userId.toString())
            sessionPreferences.saveTokens(response.accessToken, response.refreshToken)
            sessionPreferences.saveUserProfile(response.userId.toString(), response.name, response.avatarId, response.profileImageUrl)
            
            val fcmToken = sessionPreferences.getFcmToken()
            if (fcmToken != null) {
                try {
                    httpClient.post("fcm/register") {
                        setBody(com.yusufteker.pulse.shared.api.RegisterFcmTokenRequest(fcmToken, "android"))
                    }
                } catch (e: Exception) {
                    Napier.e("Failed to register FCM token after register", e)
                }
            }
            
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
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
                setBody(com.yusufteker.pulse.shared.api.UpdateProfileRequest(name, avatarId))
            }
            // Update local DataStore upon successful server update
            val currentUserId = sessionPreferences.getUserId() ?: "guest"
            val currentProfileImageUrl = sessionPreferences.getUserProfileImageUrl()
            sessionPreferences.saveUserProfile(currentUserId, name, avatarId, currentProfileImageUrl)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchMyProfile(): Result<Unit> {
        return try {
            val profile = httpClient.get("auth/me").body<com.yusufteker.pulse.shared.api.UserProfileResponse>()
            sessionPreferences.saveUserProfile(profile.id.toString(), profile.name, profile.avatarId, profile.profileImageUrl, profile.followersCount, profile.followingCount)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun registerFcmToken(token: String): Result<Unit> {
        return try {
            httpClient.post("fcm/register") {
                setBody(com.yusufteker.pulse.shared.api.RegisterFcmTokenRequest(token, "android"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
