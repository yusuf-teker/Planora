package com.yusufteker.pulse.core.network

import com.yusufteker.pulse.core.preferences.SessionPreferences
import com.yusufteker.pulse.shared.api.AuthResponse
import com.yusufteker.pulse.shared.api.RefreshTokenRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Creates and configures the Ktor HTTP Client.
 * It includes the Auth plugin which automatically intercepts 401 Unauthorized responses,
 * requests a new token, and retries the original request seamlessly.
 */
fun createHttpClient(sessionPreferences: SessionPreferences): HttpClient {
    val client = HttpClient {
        // Sunucu adresini ve formatı varsayılan olarak ayarlıyoruz. (Android emülatörü için 10.0.2.2, iOS için localhost)
        defaultRequest {
            url(getBaseUrl())
            contentType(ContentType.Application.Json)
        }

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    io.github.aakira.napier.Napier.d(message = message, tag = "HTTP_LOG")
                }
            }
            level = LogLevel.ALL
        }

        install(Auth) {
            bearer {
                // Gelen istek 401 Unauthorized dönerse (örneğin Access Token'ın süresi 15 dk dolduğunda),
                // bu blok tetiklenir ve yeni token alır.
                refreshTokens {
                    val refreshToken = sessionPreferences.getRefreshToken() ?: return@refreshTokens null
                    
                    try {
                        val response: AuthResponse = client.post("auth/refresh") {
                            markAsRefreshTokenRequest() // Ktor'a bunun refresh request olduğunu söyleyerek sonsuz döngüyü önleriz
                            setBody(RefreshTokenRequest(refreshToken))
                        }.body()

                        // Yeni tokenları DataStore'a kaydet
                        sessionPreferences.saveTokens(response.accessToken, response.refreshToken)
                        
                        BearerTokens(response.accessToken, response.refreshToken)
                    } catch (e: Exception) {
                        // Eğer refresh token da geçersizse (30 günü dolmuşsa) null dön. Uygulama kullanıcıyı çıkış yapmalı.
                        sessionPreferences.clearSession()
                        null
                    }
                }
            }
        }
    }

    // Ktor'un Auth eklentisindeki cache sorununu çözmek için (hesap değiştirildiğinde eski tokenin gönderilmesi),
    // tokeni Ktor'a önbellekletmek (loadTokens) yerine her istek öncesi güncel tokeni DataStore'dan anlık olarak çekiyoruz.
    client.requestPipeline.intercept(io.ktor.client.request.HttpRequestPipeline.State) {
        val requestBuilder = context
        val path = requestBuilder.url.buildString()
        if (!path.contains("auth/login") && !path.contains("auth/register")) {
            val token = sessionPreferences.getAccessToken()
            if (token != null) {
                requestBuilder.headers.remove(io.ktor.http.HttpHeaders.Authorization)
                requestBuilder.headers.append(io.ktor.http.HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }
    
    return client
}
