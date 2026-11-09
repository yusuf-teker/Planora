package com.yusufteker.planora.core.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.yusufteker.planora.core.preferences.SessionPreferences
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject
import platform.AuthenticationServices.ASWebAuthenticationPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Foundation.NSBundle
import platform.Foundation.NSDictionary
import platform.Foundation.NSURL
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem
import platform.Foundation.dictionaryWithContentsOfFile
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.darwin.NSObject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

@Serializable
private data class GoogleTokenResponse(
    val access_token: String? = null,
    val token_type: String? = null,
    val expires_in: Long? = null,
    val refresh_token: String? = null,
    val error: String? = null,
    val error_description: String? = null
)

/**
 * Presentation context provider required by [ASWebAuthenticationSession] to anchor the modal on iOS.
 */
private class WebAuthPresentationContextProvider(
    private val window: UIWindow
) : NSObject(), ASWebAuthenticationPresentationContextProvidingProtocol {
    override fun presentationAnchorForWebAuthenticationSession(session: ASWebAuthenticationSession): UIWindow {
        return window
    }
}

/**
 * iOS actual implementation of [rememberGoogleTasksLauncher].
 *
 * Utilizes Apple's native [ASWebAuthenticationSession] with standard OAuth 2.0 PKCE (RFC 7636)
 * to securely authorize with Google and fetch tasks via [GoogleTasksClient].
 *
 * Persists OAuth access and refresh tokens in [SessionPreferences] so that subsequent syncs
 * do not prompt the user for authorization repeatedly.
 */
@Composable
actual fun rememberGoogleTasksLauncher(
    onResult: (List<CalendarImportItem>?, String?) -> Unit
): GoogleTasksLauncher {
    val coroutineScope = rememberCoroutineScope()
    val sessionPreferences = koinInject<SessionPreferences>()

    return remember {
        object : GoogleTasksLauncher {
            override fun launch() {
                val plistPath = NSBundle.mainBundle.pathForResource("GoogleService-Info", ofType = "plist")
                val plist = plistPath?.let { NSDictionary.dictionaryWithContentsOfFile(it) }

                val clientId = plist?.get("CLIENT_ID") as? String
                val reversedClientId = plist?.get("REVERSED_CLIENT_ID") as? String

                if (clientId.isNullOrBlank()) {
                    onResult(
                        null,
                        "GoogleService-Info.plist dosyasında CLIENT_ID bulunamadı. Lütfen Firebase Console'dan iOS için güncel GoogleService-Info.plist dosyasını ekleyin."
                    )
                    return
                }

                // Standard iOS OAuth redirect URI is reversed client ID or custom app scheme
                val redirectUri = if (!reversedClientId.isNullOrBlank()) {
                    "$reversedClientId:/oauth2redirect"
                } else {
                    "planora:/oauth2redirect"
                }

                val callbackScheme = if (!reversedClientId.isNullOrBlank()) {
                    reversedClientId
                } else {
                    "planora"
                }

                // Check for existing valid access token or refresh token first to avoid repeated prompts
                coroutineScope.launch {
                    val cachedToken = sessionPreferences.getGoogleAccessToken()
                    val cachedEmail = sessionPreferences.getGoogleAccountEmail() ?: "Google Account"

                    if (!cachedToken.isNullOrBlank()) {
                        try {
                            val items = withContext(Dispatchers.IO) {
                                GoogleTasksClient.fetchAllGoogleData(cachedToken, cachedEmail)
                            }
                            onResult(items, null)
                            return@launch
                        } catch (e: Exception) {
                            println("[GoogleTasksLauncher] Cached token fetch failed, attempting silent refresh: ${e.message}")
                        }
                    }

                    val refreshToken = sessionPreferences.getGoogleRefreshToken()
                    if (!refreshToken.isNullOrBlank()) {
                        val newTokens = refreshAccessToken(clientId, refreshToken)
                        val newAccessToken = newTokens?.access_token
                        if (!newAccessToken.isNullOrBlank()) {
                            sessionPreferences.saveGoogleTokens(
                                accessToken = newAccessToken,
                                refreshToken = newTokens.refresh_token ?: refreshToken,
                                expiresInSeconds = newTokens.expires_in ?: 3600L,
                                email = cachedEmail
                            )
                            try {
                                val items = withContext(Dispatchers.IO) {
                                    GoogleTasksClient.fetchAllGoogleData(newAccessToken, cachedEmail)
                                }
                                onResult(items, null)
                                return@launch
                            } catch (e: Exception) {
                                println("[GoogleTasksLauncher] Refreshed token fetch failed: ${e.message}")
                            }
                        }
                    }

                    // Fallback to interactive OAuth session
                    startWebAuthSession(
                        clientId = clientId,
                        redirectUri = redirectUri,
                        callbackScheme = callbackScheme,
                        sessionPreferences = sessionPreferences,
                        onResult = onResult
                    )
                }
            }

            private fun startWebAuthSession(
                clientId: String,
                redirectUri: String,
                callbackScheme: String,
                sessionPreferences: SessionPreferences,
                onResult: (List<CalendarImportItem>?, String?) -> Unit
            ) {
                // 1. Generate PKCE verifier and challenge
                val codeVerifier = generateCodeVerifier()
                val codeChallenge = generateCodeChallenge(codeVerifier)

                // 2. Build Google OAuth 2.0 URL with offline access to receive a refresh token
                val components = NSURLComponents.componentsWithString("https://accounts.google.com/o/oauth2/v2/auth") ?: run {
                    onResult(null, "OAuth URL oluşturulamadı.")
                    return
                }

                components.queryItems = listOf(
                    NSURLQueryItem.queryItemWithName("client_id", value = clientId),
                    NSURLQueryItem.queryItemWithName("redirect_uri", value = redirectUri),
                    NSURLQueryItem.queryItemWithName("response_type", value = "code"),
                    NSURLQueryItem.queryItemWithName("scope", value = "https://www.googleapis.com/auth/tasks.readonly https://www.googleapis.com/auth/calendar.events.readonly email"),
                    NSURLQueryItem.queryItemWithName("access_type", value = "offline"),
                    NSURLQueryItem.queryItemWithName("prompt", value = "consent"),
                    NSURLQueryItem.queryItemWithName("code_challenge", value = codeChallenge),
                    NSURLQueryItem.queryItemWithName("code_challenge_method", value = "S256")
                )

                val authUrl = components.URL ?: run {
                    onResult(null, "OAuth URL adresi geçersiz.")
                    return
                }

                // 3. Find key window for presentation anchor
                val activeWindow = findKeyWindow()
                if (activeWindow == null) {
                    onResult(null, "Uygulama penceresi bulunamadı.")
                    return
                }

                val presentationProvider = WebAuthPresentationContextProvider(activeWindow)

                // 4. Start ASWebAuthenticationSession
                val session = ASWebAuthenticationSession(
                    uRL = authUrl,
                    callbackURLScheme = callbackScheme
                ) { callbackUrl, error ->
                    if (error != null) {
                        if (error.code == 1L) { // ASWebAuthenticationSessionErrorCodeCanceledLogin
                            onResult(null, "Google ile giriş iptal edildi.")
                        } else {
                            onResult(null, "Giriş hatası: ${error.localizedDescription}")
                        }
                        return@ASWebAuthenticationSession
                    }

                    if (callbackUrl == null) {
                        onResult(null, "Google yanıt vermedi.")
                        return@ASWebAuthenticationSession
                    }

                    val callbackComponents = NSURLComponents.componentsWithURL(callbackUrl, resolvingAgainstBaseURL = false)
                    val authCode = callbackComponents?.queryItems
                        ?.filterIsInstance<NSURLQueryItem>()
                        ?.firstOrNull { it.name == "code" }
                        ?.value

                    if (authCode.isNullOrBlank()) {
                        onResult(null, "Yetkilendirme kodu alınamadı.")
                        return@ASWebAuthenticationSession
                    }

                    // 5. Exchange code for access token in coroutine
                    coroutineScope.launch {
                        try {
                            val tokenResp = exchangeCodeForToken(
                                clientId = clientId,
                                code = authCode,
                                codeVerifier = codeVerifier,
                                redirectUri = redirectUri
                            )

                            val token = tokenResp?.access_token
                            if (token.isNullOrBlank()) {
                                onResult(null, "Google erişim belirteci alınamadı.")
                                return@launch
                            }

                            // Cache tokens securely so future calls don't require user consent
                            sessionPreferences.saveGoogleTokens(
                                accessToken = token,
                                refreshToken = tokenResp.refresh_token,
                                expiresInSeconds = tokenResp.expires_in ?: 3600L,
                                email = "Google Account"
                            )

                            val items = withContext(Dispatchers.IO) {
                                GoogleTasksClient.fetchAllGoogleData(token, "Google Account")
                            }

                            onResult(items, null)
                        } catch (e: Exception) {
                            onResult(null, "Google verileri hatası: ${e.message}")
                        }
                    }
                }

                session.presentationContextProvider = presentationProvider
                session.prefersEphemeralWebBrowserSession = false
                session.start()
            }
        }
    }
}

/**
 * Finds the currently active key window in the iOS UI hierarchy.
 */
private fun findKeyWindow(): UIWindow? {
    return UIApplication.sharedApplication.connectedScenes
        .filterIsInstance<UIWindowScene>()
        .firstOrNull { it.activationState == 0L } // UISceneActivationStateForegroundActive
        ?.windows
        ?.filterIsInstance<UIWindow>()
        ?.firstOrNull { it.isKeyWindow() }
        ?: UIApplication.sharedApplication.keyWindow
}

/**
 * Exchanges the PKCE authorization code for an OAuth 2.0 access token via Google token endpoint.
 */
private suspend fun exchangeCodeForToken(
    clientId: String,
    code: String,
    codeVerifier: String,
    redirectUri: String
): GoogleTokenResponse? = withContext(Dispatchers.IO) {
    val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    try {
        val response = client.submitForm(
            url = "https://oauth2.googleapis.com/token",
            formParameters = parameters {
                append("client_id", clientId)
                append("code", code)
                append("code_verifier", codeVerifier)
                append("grant_type", "authorization_code")
                append("redirect_uri", redirectUri)
            }
        )

        val rawBody = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw Exception("Token hatası (${response.status.value}): $rawBody")
        }

        Json { ignoreUnknownKeys = true; isLenient = true }
            .decodeFromString<GoogleTokenResponse>(rawBody)
    } finally {
        client.close()
    }
}

/**
 * Silently refreshes the OAuth 2.0 access token using a stored refresh token.
 */
private suspend fun refreshAccessToken(
    clientId: String,
    refreshToken: String
): GoogleTokenResponse? = withContext(Dispatchers.IO) {
    val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    try {
        val response = client.submitForm(
            url = "https://oauth2.googleapis.com/token",
            formParameters = parameters {
                append("client_id", clientId)
                append("refresh_token", refreshToken)
                append("grant_type", "refresh_token")
            }
        )

        val rawBody = response.bodyAsText()
        if (!response.status.isSuccess()) {
            null
        } else {
            Json { ignoreUnknownKeys = true; isLenient = true }
                .decodeFromString<GoogleTokenResponse>(rawBody)
        }
    } catch (e: Exception) {
        null
    } finally {
        client.close()
    }
}

/**
 * Generates a 32-byte cryptographically random string (base64url without padding) for PKCE.
 */
@OptIn(ExperimentalEncodingApi::class)
private fun generateCodeVerifier(): String {
    val bytes = Random.Default.nextBytes(32)
    return Base64.UrlSafe.encode(bytes).trimEnd('=')
}

/**
 * Computes the S256 code challenge for PKCE: BASE64URL-ENCODE(SHA256(code_verifier)).
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalEncodingApi::class)
private fun generateCodeChallenge(verifier: String): String {
    val bytes = verifier.encodeToByteArray()
    val digest = ByteArray(CC_SHA256_DIGEST_LENGTH.toInt())
    bytes.usePinned { inputPinned ->
        digest.usePinned { digestPinned ->
            CC_SHA256(
                inputPinned.addressOf(0),
                bytes.size.toUInt(),
                digestPinned.addressOf(0).reinterpret()
            )
        }
    }
    return Base64.UrlSafe.encode(digest).trimEnd('=')
}
