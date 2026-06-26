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
    return HttpClient {
        // Sunucu adresini ve formatı varsayılan olarak ayarlıyoruz. (Android emülatörü için 10.0.2.2, iOS için localhost)
        // Not: Gerçekte bunu bir build config üzerinden (Environment Variable) vermek gerekir.
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
                // Sunucuya yapılacak her istekten önce DataStore'dan Access Token'ı çekip Authorization header'ına ekler.
                loadTokens {
                    val accessToken = sessionPreferences.getAccessToken()
                    val refreshToken = sessionPreferences.getRefreshToken()
                    if (accessToken != null && refreshToken != null) {
                        BearerTokens(accessToken, refreshToken)
                    } else {
                        null
                    }
                }

                // Login veya Register ise Authorization header ekleme, çünkü bu endpoint'ler token istemez.
                sendWithoutRequest { request -> 
                    val path = request.url.buildString()
                    !path.contains("auth/login") && !path.contains("auth/register")
                }

                // Eğer sunucudan 401 Unauthorized dönerse (örneğin Access Token'ın süresi 15 dk dolduğunda),
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
}
