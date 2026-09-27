package com.yusufteker.planora.admin.data

import com.yusufteker.planora.shared.api.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Planora Admin API istemcisi.
 * Ktor backend'deki /admin rotaları ile haberleşir.
 */
class AdminApiClient {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    var baseUrl: String = "https://pulse-7b4z.onrender.com"
    var adminToken: String? = null
    var adminSecretKey: String = ""

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }

    private fun HttpRequestBuilder.attachAuth() {
        if (!adminToken.isNullOrBlank()) {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }
        if (adminSecretKey.isNotBlank()) {
            header("X-Admin-Secret", adminSecretKey)
        }
    }

    /**
     * Admin hesabı ile giriş yapar ve dönen JWT token'ı saklar.
     */
    suspend fun login(identifier: String, password: String): Result<AuthResponse> = runCatching {
        val response = client.post("$baseUrl/auth/login") {
            setBody(AuthRequest(identifier = identifier, password = password))
        }
        if (!response.status.isSuccess()) {
            throw Exception("Giriş başarısız: HTTP ${response.status.value}")
        }
        val auth = response.body<AuthResponse>()
        if (auth.role != "ADMIN" && adminSecretKey.isBlank()) {
            throw Exception("Bu hesaba ait ADMIN rolü bulunmuyor!")
        }
        adminToken = auth.accessToken
        auth
    }

    /**
     * Genel sistem istatistiklerini getirir.
     */
    suspend fun getStats(): Result<AdminDashboardStatsDto> = runCatching {
        val response = client.get("$baseUrl/admin/stats") {
            attachAuth()
        }
        if (!response.status.isSuccess()) {
            throw Exception("İstatistikler alınamadı: HTTP ${response.status.value}")
        }
        response.body<AdminDashboardStatsDto>()
    }

    /**
     * Kullanıcıları arar ve listeler.
     */
    suspend fun searchUsers(query: String? = null): Result<List<AdminUserSummaryDto>> = runCatching {
        val response = client.get("$baseUrl/admin/users") {
            attachAuth()
            if (!query.isNullOrBlank()) {
                parameter("query", query)
            }
        }
        if (!response.status.isSuccess()) {
            throw Exception("Kullanıcılar alınamadı: HTTP ${response.status.value}")
        }
        response.body<List<AdminUserSummaryDto>>()
    }

    /**
     * Seçilen kullanıcının görevlerini getirir.
     */
    suspend fun getUserTasks(userId: Int): Result<AdminUserTasksResponse> = runCatching {
        val response = client.get("$baseUrl/admin/users/$userId/tasks") {
            attachAuth()
        }
        if (!response.status.isSuccess()) {
            throw Exception("Görevler alınamadı: HTTP ${response.status.value}")
        }
        response.body<AdminUserTasksResponse>()
    }

    /**
     * Kullanıcı rolünü günceller (ADMIN / USER).
     */
    suspend fun updateUserRole(userId: Int, newRole: String): Result<AdminUserSummaryDto> = runCatching {
        val response = client.put("$baseUrl/admin/users/$userId/role") {
            attachAuth()
            setBody(UpdateUserRoleRequest(role = newRole))
        }
        if (!response.status.isSuccess()) {
            throw Exception("Rol güncellenemedi: HTTP ${response.status.value}")
        }
        response.body<AdminUserSummaryDto>()
    }

    /**
     * Kullanıcıya Premium atar veya kaldırır.
     */
    suspend fun setPremium(userId: Int, isPremium: Boolean, days: Int? = null): Result<SetPremiumResponse> = runCatching {
        val response = client.post("$baseUrl/admin/users/set-premium") {
            attachAuth()
            setBody(SetPremiumRequest(userId = userId, isPremium = isPremium, days = days))
        }
        if (!response.status.isSuccess()) {
            throw Exception("Premium güncellenemedi: HTTP ${response.status.value}")
        }
        response.body<SetPremiumResponse>()
    }

    /**
     * Belirli bir kullanıcıya doğrudan Push bildirimi gönderir.
     */
    suspend fun sendPushToUser(userId: Int, title: String, body: String): Result<AdminSendPushResponse> = runCatching {
        val response = client.post("$baseUrl/admin/push") {
            attachAuth()
            setBody(AdminSendPushRequest(userId = userId, title = title, body = body))
        }
        if (!response.status.isSuccess()) {
            throw Exception("Bildirim gönderilemedi: HTTP ${response.status.value}")
        }
        response.body<AdminSendPushResponse>()
    }

    /**
     * Tüm aktif FCM cihazı olan kullanıcılara toplu duyuru gönderir.
     */
    suspend fun sendBroadcastPush(title: String, body: String): Result<AdminSendPushResponse> = runCatching {
        val response = client.post("$baseUrl/admin/push") {
            attachAuth()
            setBody(AdminSendPushRequest(broadcastAll = true, title = title, body = body))
        }
        if (!response.status.isSuccess()) {
            throw Exception("Toplu bildirim gönderilemedi: HTTP ${response.status.value}")
        }
        response.body<AdminSendPushResponse>()
    }
}
