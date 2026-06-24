package com.yusufteker.pulse.feature.auth.data.repository

import io.github.aakira.napier.Napier
import com.yusufteker.pulse.core.preferences.SessionPreferences
import com.yusufteker.pulse.feature.auth.domain.repository.AuthRepository
import com.yusufteker.pulse.shared.api.AuthRequest
import com.yusufteker.pulse.shared.api.AuthResponse
import com.yusufteker.pulse.shared.api.RegisterRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody

/**
 * Implementation of [AuthRepository] that communicates with the Ktor Backend.
 */
class AuthRepositoryImpl(
    private val httpClient: HttpClient,
    private val sessionPreferences: SessionPreferences
) : AuthRepository {

    override suspend fun login(request: AuthRequest): Result<AuthResponse> {
        return try {
            // Sunucuya POST isteği gönderiyoruz.
            val response: AuthResponse = httpClient.post("auth/login") {
                setBody(request) // request objesini otomatik JSON'a çevirir (ContentNegotiation eklentisi sayesinde)
            }.body()

            // Giriş başarılıysa token'ları güvenli depoya kaydet.
            Napier.d(tag = "Screen", message = { "Login OK | isim: '${response.name}', avatar: '${response.avatarId}'" })
            sessionPreferences.saveTokens(response.accessToken, response.refreshToken)
            sessionPreferences.saveUserProfile(response.name, response.avatarId)
            Napier.d(tag = "Screen", message = { "DataStore'a kaydedildi: '${response.name}'" })
            Result.success(response)
        } catch (e: Exception) {
            // Ağ hatası, yanlış şifre (401) veya sunucu kapalıysa (500) hata olarak döner.
            Result.failure(e)
        }
    }

    override suspend fun register(request: RegisterRequest): Result<AuthResponse> {
        return try {
            val response: AuthResponse = httpClient.post("auth/register") {
                setBody(request)
            }.body()
            sessionPreferences.saveTokens(response.accessToken, response.refreshToken)
            sessionPreferences.saveUserProfile(response.name, response.avatarId)
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
}
