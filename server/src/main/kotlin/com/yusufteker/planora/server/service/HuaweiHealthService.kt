package com.yusufteker.planora.server.service

import com.yusufteker.planora.server.AppConfig
import com.yusufteker.planora.server.database.DatabaseFactory.dbQuery
import com.yusufteker.planora.server.database.tables.HealthHuaweiTokensTable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs
import kotlin.math.roundToInt

@Serializable
data class HuaweiAuthUrlResponse(
    val authUrl: String,
    val configured: Boolean = true,
    val message: String? = null
)

@Serializable
data class HuaweiConnectionStatusResponse(
    val connected: Boolean,
    val configured: Boolean,
    val needsReauth: Boolean = false,
    val apiBaseUrl: String? = null,
    val lastSyncedAtMillis: Long? = null,
    val grantedScopes: String? = null
)

@Serializable
data class HuaweiDeviceWeightDto(
    val externalId: String,
    val date: String,
    val weightKg: Double,
    val bodyFatPercent: Double? = null,
    val leanBodyMassKg: Double? = null,
    val boneMassKg: Double? = null,
    val muscleMassKg: Double? = null,
    val musclePercent: Double? = null,
    val impedanceOhm: Int? = null,
    val measuredAtMillis: Long,
    val sourcePackage: String = "com.huawei.health",
    val sourceName: String = "Huawei Health"
)

@Serializable
data class HuaweiDeviceWorkoutDto(
    val externalId: String,
    val date: String,
    val category: String = "OTHER",
    val customTitle: String? = null,
    val durationMinutes: Int,
    val caloriesBurned: Int,
    val distanceMeters: Double? = null,
    val steps: Int? = null,
    val avgHeartRateBpm: Int? = null,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val sourcePackage: String = "com.huawei.health",
    val sourceName: String = "Huawei Health"
)

@Serializable
data class HuaweiSyncResponse(
    val connected: Boolean,
    val needsReauth: Boolean = false,
    val weights: List<HuaweiDeviceWeightDto> = emptyList(),
    val workouts: List<HuaweiDeviceWorkoutDto> = emptyList(),
    val platformName: String = "Huawei Health",
    val syncedAtMillis: Long = System.currentTimeMillis(),
    val warnings: List<String> = emptyList()
)

data class HuaweiCallbackResult(
    val success: Boolean,
    val userId: Int? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null
)

internal data class HuaweiTokenRecord(
    val userId: Int,
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
    val scope: String?,
    val apiBaseUrl: String,
    val lastSyncedAt: Long?
)

internal data class HuaweiDailySummaryBucket(
    val date: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val steps: Int,
    val caloriesKcal: Int,
    val distanceMeters: Double
)

private sealed interface HuaweiApiCallResult {
    data class Success(val statusCode: Int, val body: String) : HuaweiApiCallResult
    data class HttpError(val statusCode: Int, val body: String) : HuaweiApiCallResult
    data class NeedsReauth(val reason: String) : HuaweiApiCallResult
}

object HuaweiHealthService {

    private const val OAUTH_AUTHORIZE_URL = "https://oauth-login.cloud.huawei.com/oauth2/v3/authorize"
    private const val OAUTH_TOKEN_URL = "https://oauth-login.cloud.huawei.com/oauth2/v3/token"
    private const val OAUTH_REVOKE_URL = "https://oauth-login.cloud.huawei.com/oauth2/v3/revoke"
    private const val DEFAULT_REGION_BASE_URL = "https://health-api.cloud.huawei.eu"
    private const val STATE_TTL_MS = 15L * 60_000L
    private const val TOKEN_REFRESH_MARGIN_MS = 120_000L

    private val DEFAULT_SCOPES = listOf(
        "openid",
        "https://www.huawei.com/healthkit/step.read",
        "https://www.huawei.com/healthkit/distance.read",
        "https://www.huawei.com/healthkit/calories.read",
        "https://www.huawei.com/healthkit/activity.read",
        "https://www.huawei.com/healthkit/activityrecord.read",
        "https://www.huawei.com/healthkit/bodyfat.read",
        "https://www.huawei.com/healthkit/heartrate.read",
        "https://www.huawei.com/healthkit/stress.read",
        "https://www.huawei.com/healthkit/sleep.read",
        "https://www.huawei.com/healthkit/historydata.open.week"
    )

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val refreshMutexes = ConcurrentHashMap<Int, Mutex>()

    fun isConfigured(): Boolean = !AppConfig.huaweiClientSecret.isNullOrBlank()

    private fun resolvedScopes(): String {
        val custom = AppConfig.huaweiScopes?.trim()?.takeIf { it.isNotBlank() }
        return custom ?: DEFAULT_SCOPES.joinToString(" ")
    }

    // ─────────────────────────────────────────────────────────────
    // Stateless HMAC-SHA256 OAuth State
    // ─────────────────────────────────────────────────────────────

    internal fun signState(
        userId: Int,
        issuedAtMs: Long = System.currentTimeMillis(),
        secret: String = AppConfig.jwtSecret
    ): String {
        val nonce = UUID.randomUUID().toString().replace("-", "").take(12)
        val payload = "$userId:$issuedAtMs:$nonce"
        val payloadB64 = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payload.toByteArray(StandardCharsets.UTF_8))
        val signatureBytes = hmacSha256(payload.toByteArray(StandardCharsets.UTF_8), secret)
        val sigB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes)
        return "$payloadB64.$sigB64"
    }

    internal fun verifyState(
        state: String,
        nowMs: Long = System.currentTimeMillis(),
        secret: String = AppConfig.jwtSecret,
        ttlMs: Long = STATE_TTL_MS
    ): Int? {
        val parts = state.trim().split('.')
        if (parts.size != 2) return null
        val payloadBytes = runCatching { Base64.getUrlDecoder().decode(parts[0]) }.getOrNull() ?: return null
        val providedSig = runCatching { Base64.getUrlDecoder().decode(parts[1]) }.getOrNull() ?: return null
        val expectedSig = hmacSha256(payloadBytes, secret)
        if (!MessageDigest.isEqual(providedSig, expectedSig)) return null

        val payload = String(payloadBytes, StandardCharsets.UTF_8)
        val segments = payload.split(':')
        if (segments.size < 2) return null
        val userId = segments[0].toIntOrNull()?.takeIf { it > 0 } ?: return null
        val issuedAtMs = segments[1].toLongOrNull()?.takeIf { it > 0L } ?: return null
        val ageMs = nowMs - issuedAtMs
        if (ageMs < -60_000L || ageMs > ttlMs) return null
        return userId
    }

    private fun hmacSha256(data: ByteArray, secret: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(data)
    }

    // ─────────────────────────────────────────────────────────────
    // OAuth 2.0 Authorization, Callback, Refresh & Disconnect
    // ─────────────────────────────────────────────────────────────

    fun buildAuthorizationUrl(userId: Int): String {
        val state = signState(userId)
        val clientId = AppConfig.huaweiClientId.trim()
        val redirectUri = AppConfig.huaweiRedirectUri.trim()
        println("[HuaweiHealthService] Building OAuth authorize URL for userId=$userId client_id=$clientId redirect_uri=$redirectUri")
        val params = listOf(
            "response_type" to "code",
            "client_id" to clientId,
            "redirect_uri" to redirectUri,
            "scope" to resolvedScopes(),
            "access_type" to "offline",
            "state" to state
        )
        val query = params.joinToString("&") { (k, v) ->
            "${urlEncode(k)}=${urlEncode(v)}"
        }
        return "$OAUTH_AUTHORIZE_URL?$query"
    }

    suspend fun getConnectionStatus(userId: Int): HuaweiConnectionStatusResponse {
        val configured = isConfigured()
        val tokenRow = loadTokenRecord(userId)
            ?: return HuaweiConnectionStatusResponse(
                connected = false,
                configured = configured,
                needsReauth = false
            )
        return HuaweiConnectionStatusResponse(
            connected = tokenRow.refreshToken.isNotBlank() || tokenRow.accessToken.isNotBlank(),
            configured = configured,
            needsReauth = tokenRow.refreshToken.isBlank(),
            apiBaseUrl = tokenRow.apiBaseUrl,
            lastSyncedAtMillis = tokenRow.lastSyncedAt,
            grantedScopes = tokenRow.scope
        )
    }

    suspend fun handleOAuthCallback(code: String?, state: String?): HuaweiCallbackResult {
        if (code.isNullOrBlank() || state.isNullOrBlank()) {
            return HuaweiCallbackResult(
                success = false,
                errorCode = "missing_params",
                errorMessage = "Missing authorization code or state."
            )
        }
        val userId = verifyState(state)
            ?: return HuaweiCallbackResult(
                success = false,
                errorCode = "invalid_state",
                errorMessage = "Authorization session expired or invalid."
            )
        val clientSecret = AppConfig.huaweiClientSecret?.takeIf { it.isNotBlank() }
            ?: return HuaweiCallbackResult(
                success = false,
                userId = userId,
                errorCode = "server_not_configured",
                errorMessage = "HUAWEI_CLIENT_SECRET is not configured on the server."
            )

        val formBody = buildFormBody(
            "grant_type" to "authorization_code",
            "client_id" to AppConfig.huaweiClientId,
            "client_secret" to clientSecret,
            "code" to code,
            "redirect_uri" to AppConfig.huaweiRedirectUri
        )

        val response = runCatching {
            val request = HttpRequest.newBuilder()
                .uri(URI.create(OAUTH_TOKEN_URL))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build()
            httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        }.getOrElse { err ->
            println("[HuaweiHealthService] Token exchange error for userId=$userId: ${err.message}")
            return HuaweiCallbackResult(
                success = false,
                userId = userId,
                errorCode = "network_error",
                errorMessage = err.message ?: "Could not reach Huawei OAuth server."
            )
        }

        val parsed = runCatching { json.parseToJsonElement(response.body()).jsonObject }.getOrNull()
        if (response.statusCode() !in 200..299 || parsed == null) {
            val errDesc = parsed?.stringOrNull("error_description")
                ?: parsed?.stringOrNull("error")
                ?: "HTTP ${response.statusCode()}"
            println("[HuaweiHealthService] Token exchange rejected (${response.statusCode()}) for userId=$userId: $errDesc")
            return HuaweiCallbackResult(
                success = false,
                userId = userId,
                errorCode = parsed?.stringOrNull("error") ?: "token_exchange_failed",
                errorMessage = errDesc
            )
        }

        val accessToken = parsed.stringOrNull("access_token").orEmpty()
        if (accessToken.isBlank()) {
            return HuaweiCallbackResult(
                success = false,
                userId = userId,
                errorCode = "missing_access_token",
                errorMessage = "Huawei OAuth did not return an access_token."
            )
        }
        val existingRow = loadTokenRecord(userId)
        val refreshToken = parsed.stringOrNull("refresh_token")?.takeIf { it.isNotBlank() }
            ?: existingRow?.refreshToken.orEmpty()
        val expiresInSec = parsed["expires_in"]?.jsonPrimitive?.longOrNull ?: 3600L
        val scope = parsed.stringOrNull("scope") ?: resolvedScopes()
        val nowMs = System.currentTimeMillis()
        val expiresAt = nowMs + (expiresInSec.coerceAtLeast(60L) * 1000L)
        val baseUrl = existingRow?.apiBaseUrl ?: AppConfig.huaweiApiBaseUrl.ifBlank { DEFAULT_REGION_BASE_URL }

        upsertTokenRecord(
            userId = userId,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = expiresAt,
            scope = scope,
            apiBaseUrl = baseUrl,
            nowMs = nowMs
        )

        return HuaweiCallbackResult(success = true, userId = userId)
    }

    suspend fun disconnect(userId: Int) {
        val existing = loadTokenRecord(userId)
        if (existing != null && existing.refreshToken.isNotBlank()) {
            runCatching {
                val formBody = buildFormBody(
                    "token" to existing.refreshToken,
                    "token_type_hint" to "refresh_token",
                    "client_id" to AppConfig.huaweiClientId,
                    "client_secret" to (AppConfig.huaweiClientSecret ?: "")
                )
                val req = HttpRequest.newBuilder()
                    .uri(URI.create(OAUTH_REVOKE_URL))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build()
                httpClient.send(req, HttpResponse.BodyHandlers.ofString())
            }
        }
        deleteTokenRecord(userId)
    }

    private suspend fun refreshAccessToken(
        userId: Int,
        staleAccessToken: String? = null
    ): Result<HuaweiTokenRecord> {
        val mutex = refreshMutexes.computeIfAbsent(userId) { Mutex() }
        return mutex.withLock {
            val current = loadTokenRecord(userId)
                ?: return@withLock Result.failure(IllegalStateException("Huawei account is not connected."))
            val nowMs = System.currentTimeMillis()
            if (staleAccessToken != null &&
                current.accessToken != staleAccessToken &&
                current.expiresAt > nowMs + 60_000L
            ) {
                return@withLock Result.success(current)
            }
            if (current.refreshToken.isBlank()) {
                return@withLock Result.failure(IllegalStateException("Missing Huawei refresh token."))
            }
            val clientSecret = AppConfig.huaweiClientSecret?.takeIf { it.isNotBlank() }
                ?: return@withLock Result.failure(IllegalStateException("HUAWEI_CLIENT_SECRET is not configured."))

            val formBody = buildFormBody(
                "grant_type" to "refresh_token",
                "client_id" to AppConfig.huaweiClientId,
                "client_secret" to clientSecret,
                "refresh_token" to current.refreshToken
            )

            val response = runCatching {
                val req = HttpRequest.newBuilder()
                    .uri(URI.create(OAUTH_TOKEN_URL))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build()
                httpClient.send(req, HttpResponse.BodyHandlers.ofString())
            }.getOrElse { err ->
                return@withLock Result.failure(err)
            }

            val parsed = runCatching { json.parseToJsonElement(response.body()).jsonObject }.getOrNull()
            if (response.statusCode() !in 200..299 || parsed == null) {
                val errMsg = parsed?.stringOrNull("error_description")
                    ?: parsed?.stringOrNull("error")
                    ?: "HTTP ${response.statusCode()}"
                return@withLock Result.failure(IllegalStateException("Huawei refresh failed: $errMsg"))
            }

            val newAccess = parsed.stringOrNull("access_token")?.takeIf { it.isNotBlank() }
                ?: return@withLock Result.failure(IllegalStateException("Empty access_token on Huawei refresh."))
            val newRefresh = parsed.stringOrNull("refresh_token")?.takeIf { it.isNotBlank() }
                ?: current.refreshToken
            val expiresInSec = parsed["expires_in"]?.jsonPrimitive?.longOrNull ?: 3600L
            val updatedAt = System.currentTimeMillis()
            val newExpiresAt = updatedAt + (expiresInSec.coerceAtLeast(60L) * 1000L)
            val newScope = parsed.stringOrNull("scope") ?: current.scope

            upsertTokenRecord(
                userId = userId,
                accessToken = newAccess,
                refreshToken = newRefresh,
                expiresAt = newExpiresAt,
                scope = newScope,
                apiBaseUrl = current.apiBaseUrl,
                nowMs = updatedAt
            )

            Result.success(
                current.copy(
                    accessToken = newAccess,
                    refreshToken = newRefresh,
                    expiresAt = newExpiresAt,
                    scope = newScope
                )
            )
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Sync Data (Workouts, Daily Steps & Calories, Body Weight/Fat)
    // ─────────────────────────────────────────────────────────────

    suspend fun syncHealthData(
        userId: Int,
        days: Int = 7,
        timeZoneId: String = "Europe/Istanbul"
    ): HuaweiSyncResponse {
        val nowMs = System.currentTimeMillis()
        val tokenRow = loadTokenRecord(userId)
            ?: return HuaweiSyncResponse(
                connected = false,
                needsReauth = false,
                syncedAtMillis = nowMs
            )

        if (!isConfigured()) {
            return HuaweiSyncResponse(
                connected = true,
                needsReauth = false,
                syncedAtMillis = nowMs,
                warnings = listOf("Huawei Health Kit server secret is not configured.")
            )
        }

        val zone = runCatching { ZoneId.of(timeZoneId) }.getOrElse { ZoneId.of("Europe/Istanbul") }
        val clampedDays = days.coerceIn(1, 7)
        val today = LocalDate.now(zone)
        // Stay strictly within 7 days to comply with historydata.open.week
        val maxWeekStartMs = nowMs - (7L * 86_400_000L) + 120_000L
        val requestedStartMs = today.minusDays((clampedDays - 1).toLong())
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
        val startMs = maxOf(requestedStartMs, maxWeekStartMs)
        val endMs = nowMs

        val warnings = mutableListOf<String>()
        val workouts = mutableListOf<HuaweiDeviceWorkoutDto>()
        var needsReauth = false

        // 1. Fetch structured Activity Records (Workouts)
        var cursor: String? = null
        var pageCount = 0
        do {
            val cursorQuery = cursor?.takeIf { it.isNotBlank() }?.let { "&cursor=${urlEncode(it)}" } ?: ""
            val path = "/healthkit/v2/activityRecords?startTime=$startMs&endTime=$endMs$cursorQuery"
            when (val res = executeHuaweiRequest(userId, "GET", path)) {
                is HuaweiApiCallResult.NeedsReauth -> {
                    needsReauth = true
                    warnings.add(res.reason)
                    break
                }
                is HuaweiApiCallResult.HttpError -> {
                    warnings.add("activityRecords (${res.statusCode}): ${extractHuaweiErrorSummary(res.body)}")
                    break
                }
                is HuaweiApiCallResult.Success -> {
                    val (pageWorkouts, nextCursor) = parseActivityRecordsWithCursor(res.body, zone)
                    workouts.addAll(pageWorkouts)
                    cursor = nextCursor
                }
            }
            pageCount++
        } while (!cursor.isNullOrBlank() && pageCount < 5)

        if (needsReauth) {
            return HuaweiSyncResponse(
                connected = true,
                needsReauth = true,
                syncedAtMillis = nowMs,
                warnings = warnings
            )
        }

        // 2. Fetch daily Steps, Calories & Distance via sampleSet:polymerize
        val tzOffset = formatZoneOffset(zone)
        val combinedPolymerizeBody = buildPolymerizeRequestJson(
            dataTypeNames = listOf(
                "com.huawei.continuous.steps.delta",
                "com.huawei.continuous.calories.burnt",
                "com.huawei.continuous.distance.delta"
            ),
            startMs = startMs,
            endMs = endMs,
            timeZoneOffset = tzOffset
        )

        var dailySummaryBuckets: List<HuaweiDailySummaryBucket> = emptyList()
        when (val summaryRes = executeHuaweiRequest(userId, "POST", "/healthkit/v2/sampleSet:polymerize", combinedPolymerizeBody)) {
            is HuaweiApiCallResult.NeedsReauth -> {
                needsReauth = true
                warnings.add(summaryRes.reason)
            }
            is HuaweiApiCallResult.Success -> {
                dailySummaryBuckets = parseDailySummaryPolymerizeJson(summaryRes.body, zone)
            }
            is HuaweiApiCallResult.HttpError -> {
                // Fallback to steps only if one of the secondary scopes wasn't approved
                val stepsOnlyBody = buildPolymerizeRequestJson(
                    dataTypeNames = listOf("com.huawei.continuous.steps.delta"),
                    startMs = startMs,
                    endMs = endMs,
                    timeZoneOffset = tzOffset
                )
                when (val fallbackRes = executeHuaweiRequest(userId, "POST", "/healthkit/v2/sampleSet:polymerize", stepsOnlyBody)) {
                    is HuaweiApiCallResult.NeedsReauth -> {
                        needsReauth = true
                        warnings.add(fallbackRes.reason)
                    }
                    is HuaweiApiCallResult.Success -> {
                        dailySummaryBuckets = parseDailySummaryPolymerizeJson(fallbackRes.body, zone)
                    }
                    is HuaweiApiCallResult.HttpError -> {
                        warnings.add("dailySummary (${fallbackRes.statusCode}): ${extractHuaweiErrorSummary(fallbackRes.body)}")
                    }
                }
            }
        }

        if (needsReauth) {
            return HuaweiSyncResponse(
                connected = true,
                needsReauth = true,
                workouts = workouts,
                syncedAtMillis = nowMs,
                warnings = warnings
            )
        }

        val dailyStepWorkouts = subtractWorkoutsFromDailySummaries(
            dailyBuckets = dailySummaryBuckets,
            structuredWorkouts = workouts,
            nowMs = nowMs
        )

        // 3. Fetch Body Weight & Body Composition (wrapped independently so partial scope grants still sync workouts)
        val weights = runCatching {
            val weightBody = buildPolymerizeRequestJson(
                dataTypeNames = listOf("com.huawei.instantaneous.body_weight"),
                startMs = startMs,
                endMs = endMs,
                timeZoneOffset = null
            )
            when (val weightRes = executeHuaweiRequest(userId, "POST", "/healthkit/v2/sampleSet:polymerize", weightBody)) {
                is HuaweiApiCallResult.Success -> parseBodyWeightsPolymerizeJson(weightRes.body, zone)
                is HuaweiApiCallResult.HttpError -> {
                    if (weightRes.statusCode != 404) {
                        warnings.add("bodyWeight (${weightRes.statusCode}): ${extractHuaweiErrorSummary(weightRes.body)}")
                    }
                    emptyList()
                }
                is HuaweiApiCallResult.NeedsReauth -> {
                    needsReauth = true
                    emptyList()
                }
            }
        }.getOrElse { err ->
            warnings.add("bodyWeight parse error: ${err.message}")
            emptyList()
        }

        updateLastSyncedAt(userId, nowMs)

        return HuaweiSyncResponse(
            connected = true,
            needsReauth = needsReauth,
            weights = weights,
            workouts = workouts + dailyStepWorkouts,
            platformName = "Huawei Health",
            syncedAtMillis = nowMs,
            warnings = warnings
        )
    }

    private suspend fun executeHuaweiRequest(
        userId: Int,
        method: String,
        pathAndQuery: String,
        jsonBody: String? = null
    ): HuaweiApiCallResult {
        var tokenRow = loadTokenRecord(userId)
            ?: return HuaweiApiCallResult.NeedsReauth("Huawei account is not connected.")

        val nowMs = System.currentTimeMillis()
        if (tokenRow.expiresAt <= nowMs + TOKEN_REFRESH_MARGIN_MS) {
            val refreshed = refreshAccessToken(userId, tokenRow.accessToken)
            tokenRow = refreshed.getOrElse { err ->
                return HuaweiApiCallResult.NeedsReauth(err.message ?: "Huawei session expired.")
            }
        }

        var currentAccessToken = tokenRow.accessToken
        var currentBaseUrl = tokenRow.apiBaseUrl.ifBlank { DEFAULT_REGION_BASE_URL }.trimEnd('/')
        var didRefresh = false
        var didRegionRedirect = false

        while (true) {
            val targetUrl = "$currentBaseUrl$pathAndQuery"
            val builder = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .timeout(Duration.ofSeconds(25))
                .header("Authorization", "Bearer $currentAccessToken")
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("x-client-id", AppConfig.huaweiClientId)
                .header("x-version", "1.0.0")

            val request = if (method.equals("POST", ignoreCase = true)) {
                builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody ?: "{}")).build()
            } else {
                builder.GET().build()
            }

            val response = runCatching {
                httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            }.getOrElse { err ->
                return HuaweiApiCallResult.HttpError(503, err.message ?: "Network error")
            }

            val status = response.statusCode()
            val body = response.body().orEmpty()

            if (status == 401 && !didRefresh) {
                didRefresh = true
                val refreshed = refreshAccessToken(userId, staleAccessToken = currentAccessToken)
                val updated = refreshed.getOrElse { err ->
                    return HuaweiApiCallResult.NeedsReauth(err.message ?: "Huawei token refresh failed.")
                }
                currentAccessToken = updated.accessToken
                currentBaseUrl = updated.apiBaseUrl.ifBlank { currentBaseUrl }.trimEnd('/')
                continue
            }

            if (status == 403 && !didRegionRedirect) {
                val locationHeader = response.headers().firstValue("Location").orElse(null)
                    ?: response.headers().firstValue("location").orElse(null)
                val redirectedBase = extractBaseUrl(locationHeader)
                if (redirectedBase != null && !redirectedBase.equals(currentBaseUrl, ignoreCase = true)) {
                    didRegionRedirect = true
                    currentBaseUrl = redirectedBase
                    updateApiBaseUrl(userId, redirectedBase)
                    continue
                }
            }

            return if (status in 200..299) {
                HuaweiApiCallResult.Success(status, body)
            } else if (status == 401) {
                HuaweiApiCallResult.NeedsReauth("Huawei authorization expired (401).")
            } else {
                HuaweiApiCallResult.HttpError(status, body)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Pure Parsing & Mapping Helpers (Unit-Testable)
    // ─────────────────────────────────────────────────────────────

    internal fun normalizeTimestampMillis(raw: Long): Long {
        return when {
            raw <= 0L -> 0L
            raw > 1_000_000_000_000_000L -> raw / 1_000_000L
            raw < 100_000_000_000L -> raw * 1000L
            else -> raw
        }
    }

    internal fun parseActivityRecordsJson(
        jsonString: String,
        zone: ZoneId = ZoneId.of("Europe/Istanbul")
    ): List<HuaweiDeviceWorkoutDto> = parseActivityRecordsWithCursor(jsonString, zone).first

    internal fun parseActivityRecordsWithCursor(
        jsonString: String,
        zone: ZoneId = ZoneId.of("Europe/Istanbul")
    ): Pair<List<HuaweiDeviceWorkoutDto>, String?> {
        if (jsonString.isBlank()) return emptyList<HuaweiDeviceWorkoutDto>() to null
        val root = runCatching { json.parseToJsonElement(jsonString).jsonObject }.getOrNull()
            ?: return emptyList<HuaweiDeviceWorkoutDto>() to null

        val recordsArray = root["activityRecord"]?.jsonArray
            ?: root["activityRecords"]?.jsonArray
            ?: emptyList()

        val workouts = recordsArray.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val rawId = obj.stringOrNull("id")?.takeIf { it.isNotBlank() }
            val name = obj.stringOrNull("name")?.trim()?.takeIf { it.isNotBlank() }
            val desc = obj.stringOrNull("desc")?.trim()?.takeIf { it.isNotBlank() }
            val rawStart = obj.longOrNull("startTime") ?: 0L
            val rawEnd = obj.longOrNull("endTime") ?: 0L
            val startMs = normalizeTimestampMillis(rawStart)
            val endMs = normalizeTimestampMillis(rawEnd).let { if (it >= startMs) it else startMs }
            if (startMs <= 0L) return@mapNotNull null

            val durationMinutes = (((endMs - startMs) / 60_000.0).roundToInt()).coerceAtLeast(1)
            val dateIso = Instant.ofEpochMilli(startMs).atZone(zone).toLocalDate().toString()

            val activityTypeInt = obj["activityType"]?.jsonPrimitive?.intOrNull
                ?: obj["activityTypeId"]?.jsonPrimitive?.intOrNull
            val activityTypeStr = obj.stringOrNull("activityType") ?: obj.stringOrNull("activityTypeName")

            var caloriesBurned = 0
            var distanceMeters: Double? = null
            var steps: Int? = null
            var avgHeartRateBpm: Int? = null

            val summaryObj = obj["activitySummary"] as? JsonObject
            val dataSummary = summaryObj?.get("dataSummary") as? JsonArray
            dataSummary?.forEach { dsElem ->
                val dsObj = dsElem as? JsonObject ?: return@forEach
                val dtName = dsObj.stringOrNull("dataTypeName")?.lowercase().orEmpty()
                val values = (dsObj["value"] as? JsonArray).orEmpty()

                when {
                    dtName.contains("calories") -> {
                        val kcal = findFieldNumericValue(values, listOf("calories_total", "calories", "total", "calorie"))
                        if (kcal != null && kcal > 0.0) {
                            caloriesBurned = maxOf(caloriesBurned, kcal.roundToInt())
                        }
                    }
                    dtName.contains("distance") -> {
                        val dist = findFieldNumericValue(values, listOf("distance", "distance_total", "total"))
                        if (dist != null && dist > 0.0) {
                            distanceMeters = maxOf(distanceMeters ?: 0.0, dist)
                        }
                    }
                    dtName.contains("steps") -> {
                        val st = findFieldNumericValue(values, listOf("steps_total", "steps", "step", "total"))
                        if (st != null && st > 0.0) {
                            steps = maxOf(steps ?: 0, st.roundToInt())
                        }
                    }
                    dtName.contains("heart_rate") -> {
                        val hr = findFieldNumericValue(values, listOf("avg", "heart_rate", "bpm"))
                        val rounded = hr?.roundToInt()
                        if (rounded != null && rounded in 40..230) {
                            avgHeartRateBpm = rounded
                        }
                    }
                }
            }

            val category = mapHuaweiActivityType(
                activityType = activityTypeInt,
                rawTitle = listOfNotNull(activityTypeStr, name, desc).joinToString(" ")
            )

            HuaweiDeviceWorkoutDto(
                externalId = rawId ?: "hw_act_${startMs}",
                date = dateIso,
                category = category,
                customTitle = desc ?: name,
                durationMinutes = durationMinutes,
                caloriesBurned = caloriesBurned.coerceIn(0, 15_000),
                distanceMeters = distanceMeters,
                steps = steps,
                avgHeartRateBpm = avgHeartRateBpm,
                startTimeMillis = startMs,
                endTimeMillis = endMs,
                sourcePackage = "com.huawei.health",
                sourceName = "Huawei Health"
            )
        }

        val hasMore = root["hasMoreData"]?.jsonPrimitive?.booleanOrNull == true
        val nextCursor = if (hasMore) root.stringOrNull("cursor") else null
        return workouts to nextCursor
    }

    internal fun parseDailySummaryPolymerizeJson(
        jsonString: String,
        zone: ZoneId = ZoneId.of("Europe/Istanbul")
    ): List<HuaweiDailySummaryBucket> {
        if (jsonString.isBlank()) return emptyList()
        val root = runCatching { json.parseToJsonElement(jsonString).jsonObject }.getOrNull()
            ?: return emptyList()
        val groups = (root["group"] as? JsonArray).orEmpty()

        val byDate = linkedMapOf<String, HuaweiDailySummaryBucket>()

        for (groupElem in groups) {
            val groupObj = groupElem as? JsonObject ?: continue
            val groupStartMs = normalizeTimestampMillis(groupObj.longOrNull("startTime") ?: 0L)
            val groupEndMs = normalizeTimestampMillis(groupObj.longOrNull("endTime") ?: groupStartMs)
            if (groupStartMs <= 0L) continue

            val dateIso = Instant.ofEpochMilli(groupStartMs).atZone(zone).toLocalDate().toString()
            var stepsSum = 0
            var caloriesSum = 0.0
            var distanceSum = 0.0

            val sampleSets = (groupObj["sampleSet"] as? JsonArray).orEmpty()
            for (setElem in sampleSets) {
                val setObj = setElem as? JsonObject ?: continue
                val setDataType = (setObj.stringOrNull("dataTypeName")
                    ?: setObj.stringOrNull("dataCollectorId"))?.lowercase().orEmpty()
                val points = (setObj["samplePoints"] as? JsonArray).orEmpty()
                for (ptElem in points) {
                    val ptObj = ptElem as? JsonObject ?: continue
                    val dtName = (ptObj.stringOrNull("dataTypeName") ?: setDataType).lowercase()
                    val values = (ptObj["value"] as? JsonArray).orEmpty()
                    when {
                        dtName.contains("steps") -> {
                            val v = findFieldNumericValue(values, listOf("steps", "steps_delta", "steps_total", "total"))
                            if (v != null && v > 0.0) stepsSum += v.roundToInt()
                        }
                        dtName.contains("calories") -> {
                            val v = findFieldNumericValue(values, listOf("calories", "calories_total", "total"))
                            if (v != null && v > 0.0) caloriesSum += v
                        }
                        dtName.contains("distance") -> {
                            val v = findFieldNumericValue(values, listOf("distance", "distance_delta", "distance_total", "total"))
                            if (v != null && v > 0.0) distanceSum += v
                        }
                    }
                }
            }

            val existing = byDate[dateIso]
            byDate[dateIso] = if (existing == null) {
                HuaweiDailySummaryBucket(
                    date = dateIso,
                    startTimeMillis = groupStartMs,
                    endTimeMillis = maxOf(groupEndMs, groupStartMs),
                    steps = stepsSum,
                    caloriesKcal = caloriesSum.roundToInt(),
                    distanceMeters = distanceSum
                )
            } else {
                existing.copy(
                    startTimeMillis = minOf(existing.startTimeMillis, groupStartMs),
                    endTimeMillis = maxOf(existing.endTimeMillis, groupEndMs),
                    steps = existing.steps + stepsSum,
                    caloriesKcal = existing.caloriesKcal + caloriesSum.roundToInt(),
                    distanceMeters = existing.distanceMeters + distanceSum
                )
            }
        }

        return byDate.values.toList()
    }

    internal fun subtractWorkoutsFromDailySummaries(
        dailyBuckets: List<HuaweiDailySummaryBucket>,
        structuredWorkouts: List<HuaweiDeviceWorkoutDto>,
        nowMs: Long = System.currentTimeMillis()
    ): List<HuaweiDeviceWorkoutDto> {
        return dailyBuckets.mapNotNull { day ->
            val dayWorkouts = structuredWorkouts.filter { it.date == day.date }
            val exerciseSteps = dayWorkouts.sumOf { it.steps ?: 0 }
            val remainingSteps = (day.steps - exerciseSteps).coerceAtLeast(0)
            if (remainingSteps < 20) return@mapNotNull null

            val exerciseDistance = dayWorkouts.sumOf { it.distanceMeters ?: 0.0 }
            val remainingDistanceMeters = day.distanceMeters
                .takeIf { it > 0.0 }
                ?.let { (it - exerciseDistance).coerceAtLeast(0.0) }
                ?.takeIf { it >= 15.0 }
                ?: (remainingSteps * 0.75)

            val exerciseKcal = dayWorkouts.sumOf { it.caloriesBurned }
            val remainingActiveKcal = (day.caloriesKcal - exerciseKcal)
                .takeIf { it in 5..6000 } ?: 0

            val estimatedDurationMinutes = (remainingSteps / 100).coerceAtLeast(1)

            HuaweiDeviceWorkoutDto(
                externalId = "hw_steps_${day.date}",
                date = day.date,
                category = "DAILY_STEPS",
                customTitle = "$remainingSteps Adım • Günlük Hareket",
                durationMinutes = estimatedDurationMinutes,
                caloriesBurned = remainingActiveKcal,
                distanceMeters = remainingDistanceMeters,
                steps = remainingSteps,
                avgHeartRateBpm = null,
                startTimeMillis = day.startTimeMillis,
                endTimeMillis = day.endTimeMillis.coerceAtMost(nowMs).coerceAtLeast(day.startTimeMillis),
                sourcePackage = "com.huawei.health",
                sourceName = "Huawei Health"
            )
        }
    }

    internal fun parseBodyWeightsPolymerizeJson(
        jsonString: String,
        zone: ZoneId = ZoneId.of("Europe/Istanbul")
    ): List<HuaweiDeviceWeightDto> {
        if (jsonString.isBlank()) return emptyList()
        val root = runCatching { json.parseToJsonElement(jsonString).jsonObject }.getOrNull()
            ?: return emptyList()
        val groups = (root["group"] as? JsonArray).orEmpty()

        val result = mutableListOf<HuaweiDeviceWeightDto>()

        for (groupElem in groups) {
            val groupObj = groupElem as? JsonObject ?: continue
            val groupTimeMs = normalizeTimestampMillis(groupObj.longOrNull("startTime") ?: 0L)
            val sampleSets = (groupObj["sampleSet"] as? JsonArray).orEmpty()

            for (setElem in sampleSets) {
                val setObj = setElem as? JsonObject ?: continue
                val points = (setObj["samplePoints"] as? JsonArray).orEmpty()
                for (ptElem in points) {
                    val ptObj = ptElem as? JsonObject ?: continue
                    val measuredMs = normalizeTimestampMillis(
                        ptObj.longOrNull("startTime")
                            ?: ptObj.longOrNull("endTime")
                            ?: groupTimeMs
                    )
                    if (measuredMs <= 0L) continue

                    val values = (ptObj["value"] as? JsonArray).orEmpty()
                    val weightKg = findFieldNumericValue(values, listOf("body_weight", "weight"))
                        ?.takeIf { it in 20.0..350.0 } ?: continue
                    val bodyFatPercent = findFieldNumericValue(values, listOf("body_fat_rate", "body_fat"))
                        ?.takeIf { it in 2.0..70.0 }
                    val muscleMassKg = findFieldNumericValue(
                        values,
                        listOf("muscle_mass", "skeletal_musclel_mass", "skeletal_muscle_mass")
                    )?.takeIf { it in 1.0..150.0 }
                    val boneMassKg = findFieldNumericValue(values, listOf("bone_salt", "bone_mass"))
                        ?.takeIf { it in 0.2..15.0 }
                    val impedanceOhm = findFieldNumericValue(values, listOf("impedance"))
                        ?.roundToInt()
                        ?.takeIf { it in 100..2500 }

                    val dateIso = Instant.ofEpochMilli(measuredMs).atZone(zone).toLocalDate().toString()
                    result.add(
                        HuaweiDeviceWeightDto(
                            externalId = "hw_weight_$measuredMs",
                            date = dateIso,
                            weightKg = weightKg,
                            bodyFatPercent = bodyFatPercent,
                            boneMassKg = boneMassKg,
                            muscleMassKg = muscleMassKg,
                            impedanceOhm = impedanceOhm,
                            measuredAtMillis = measuredMs,
                            sourcePackage = "com.huawei.health",
                            sourceName = "Huawei Health"
                        )
                    )
                }
            }
        }

        return result
    }

    internal fun mapHuaweiActivityType(activityType: Int?, rawTitle: String?): String {
        val text = rawTitle.orEmpty().lowercase()
        return when {
            text.contains("tennis") || text.contains("tenis") -> "TENNIS"
            text.contains("basketball") || text.contains("basketbol") -> "BASKETBALL"
            text.contains("football") || text.contains("soccer") || text.contains("futbol") -> "FOOTBALL"
            text.contains("volleyball") || text.contains("voleybol") -> "VOLLEYBALL"
            text.contains("pilates") -> "PILATES"
            text.contains("yoga") || text.contains("stretch") || text.contains("esneme") -> "YOGA"
            text.contains("swim") || text.contains("yüzme") || text.contains("yuzme") || text.contains("pool") -> "SWIMMING"
            text.contains("elliptical") || text.contains("eliptik") -> "ELLIPTICAL"
            text.contains("rowing") || text.contains("kürek") || text.contains("kurek") -> "ROWING"
            text.contains("cycling") || text.contains("biking") || text.contains("bike") || text.contains("spin") || text.contains("bisiklet") -> "CYCLING"
            text.contains("hiking") || text.contains("trekking") || text.contains("mountaineering") || text.contains("doğa yürüyüş") -> "HIKING"
            text.contains("hiit") || text.contains("interval") || text.contains("crossfit") || text.contains("tabata") || text.contains("jump rope") || text.contains("rope skipping") -> "HIIT"
            text.contains("boxing") || text.contains("martial") || text.contains("karate") || text.contains("taekwondo") || text.contains("judo") || text.contains("kickbox") || text.contains("mma") || text.contains("boks") -> "MARTIAL_ARTS"
            text.contains("dance") || text.contains("zumba") || text.contains("ballet") || text.contains("dans") -> "DANCE"
            text.contains("calisthenics") || text.contains("core training") || text.contains("core_training") || text.contains("gymnastics") || text.contains("aerobics") -> "CALISTHENICS"
            text.contains("strength") || text.contains("weight") || text.contains("bodybuilding") || text.contains("fitness") || text.contains("ağırlık") -> "STRENGTH"
            text.contains("treadmill") || text.contains("running") || text.contains("jogging") || text.contains("run") || text.contains("koşu") -> "RUNNING"
            text.contains("walking") || text.contains("walk") || text.contains("yürüyüş") -> "WALKING"
            else -> when (activityType) {
                21 -> "BASKETBALL"
                56, 57 -> "RUNNING"
                90, 97 -> "WALKING"
                117 -> "DANCE"
                118 -> "CALISTHENICS"
                else -> "OTHER"
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // DB Persistence Helpers
    // ─────────────────────────────────────────────────────────────

    private suspend fun loadTokenRecord(userId: Int): HuaweiTokenRecord? = dbQuery {
        HealthHuaweiTokensTable
            .selectAll()
            .where { HealthHuaweiTokensTable.userId eq userId }
            .limit(1)
            .firstOrNull()
            ?.let { row ->
                HuaweiTokenRecord(
                    userId = row[HealthHuaweiTokensTable.userId].value,
                    accessToken = row[HealthHuaweiTokensTable.accessToken],
                    refreshToken = row[HealthHuaweiTokensTable.refreshToken],
                    expiresAt = row[HealthHuaweiTokensTable.expiresAt],
                    scope = row[HealthHuaweiTokensTable.scope],
                    apiBaseUrl = row[HealthHuaweiTokensTable.apiBaseUrl],
                    lastSyncedAt = row[HealthHuaweiTokensTable.lastSyncedAt]
                )
            }
    }

    private suspend fun upsertTokenRecord(
        userId: Int,
        accessToken: String,
        refreshToken: String,
        expiresAt: Long,
        scope: String?,
        apiBaseUrl: String,
        nowMs: Long
    ) = dbQuery {
        val existing = HealthHuaweiTokensTable
            .selectAll()
            .where { HealthHuaweiTokensTable.userId eq userId }
            .limit(1)
            .firstOrNull()

        if (existing != null) {
            HealthHuaweiTokensTable.update({ HealthHuaweiTokensTable.userId eq userId }) {
                it[HealthHuaweiTokensTable.accessToken] = accessToken
                it[HealthHuaweiTokensTable.refreshToken] = refreshToken
                it[HealthHuaweiTokensTable.expiresAt] = expiresAt
                it[HealthHuaweiTokensTable.scope] = scope
                it[HealthHuaweiTokensTable.apiBaseUrl] = apiBaseUrl
                it[HealthHuaweiTokensTable.updatedAt] = nowMs
            }
        } else {
            HealthHuaweiTokensTable.insert {
                it[HealthHuaweiTokensTable.userId] = userId
                it[HealthHuaweiTokensTable.accessToken] = accessToken
                it[HealthHuaweiTokensTable.refreshToken] = refreshToken
                it[HealthHuaweiTokensTable.expiresAt] = expiresAt
                it[HealthHuaweiTokensTable.scope] = scope
                it[HealthHuaweiTokensTable.apiBaseUrl] = apiBaseUrl
                it[HealthHuaweiTokensTable.lastSyncedAt] = null
                it[HealthHuaweiTokensTable.createdAt] = Instant.now()
                it[HealthHuaweiTokensTable.updatedAt] = nowMs
            }
        }
    }

    private suspend fun updateApiBaseUrl(userId: Int, newBaseUrl: String) = dbQuery {
        HealthHuaweiTokensTable.update({ HealthHuaweiTokensTable.userId eq userId }) {
            it[apiBaseUrl] = newBaseUrl
            it[updatedAt] = System.currentTimeMillis()
        }
    }

    private suspend fun updateLastSyncedAt(userId: Int, syncedAtMs: Long) = dbQuery {
        HealthHuaweiTokensTable.update({ HealthHuaweiTokensTable.userId eq userId }) {
            it[lastSyncedAt] = syncedAtMs
            it[updatedAt] = syncedAtMs
        }
    }

    private suspend fun deleteTokenRecord(userId: Int) = dbQuery {
        HealthHuaweiTokensTable.deleteWhere { HealthHuaweiTokensTable.userId eq userId }
    }

    // ─────────────────────────────────────────────────────────────
    // JSON & HTTP Formatting Helpers
    // ─────────────────────────────────────────────────────────────

    private fun buildPolymerizeRequestJson(
        dataTypeNames: List<String>,
        startMs: Long,
        endMs: Long,
        timeZoneOffset: String?
    ): String {
        val obj = buildJsonObject {
            putJsonArray("polymerizeWith") {
                dataTypeNames.forEach { name ->
                    addJsonObject {
                        put("dataTypeName", name)
                    }
                }
            }
            put("startTime", startMs)
            put("endTime", endMs)
            if (!timeZoneOffset.isNullOrBlank()) {
                putJsonObject("groupByTime") {
                    putJsonObject("groupPeriod") {
                        put("unit", "day")
                        put("value", 1)
                        put("timeZone", timeZoneOffset)
                    }
                }
            }
        }
        return json.encodeToString(obj)
    }

    private fun formatZoneOffset(zone: ZoneId): String {
        val totalSec = ZonedDateTime.now(zone).offset.totalSeconds
        val sign = if (totalSec >= 0) "+" else "-"
        val absSec = abs(totalSec)
        val hh = (absSec / 3600).toString().padStart(2, '0')
        val mm = ((absSec % 3600) / 60).toString().padStart(2, '0')
        return "$sign$hh$mm"
    }

    private fun extractBaseUrl(locationHeader: String?): String? {
        val raw = locationHeader?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return runCatching {
            val uri = URI.create(raw)
            val scheme = uri.scheme?.takeIf { it.startsWith("http", ignoreCase = true) } ?: return null
            val authority = uri.authority?.takeIf { it.isNotBlank() } ?: return null
            "$scheme://$authority"
        }.getOrNull()
    }

    private fun extractHuaweiErrorSummary(body: String): String {
        if (body.isBlank()) return "Empty error response"
        val obj = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
        val errObj = obj?.get("error") as? JsonObject
        val code = errObj?.get("code")?.jsonPrimitive?.contentOrNull
            ?: obj?.stringOrNull("error")
        val msg = errObj?.stringOrNull("message")
            ?: obj?.stringOrNull("error_description")
            ?: obj?.stringOrNull("message")
        return listOfNotNull(code, msg).joinToString(": ").ifBlank { body.take(180) }
    }

    private fun findFieldNumericValue(values: List<JsonElement>, preferredFieldNames: List<String>): Double? {
        if (values.isEmpty()) return null
        val objs = values.mapNotNull { it as? JsonObject }
        for (preferred in preferredFieldNames) {
            val match = objs.firstOrNull {
                it.stringOrNull("fieldName")?.equals(preferred, ignoreCase = true) == true
            }
            if (match != null) {
                extractNumericValue(match)?.let { return it }
            }
        }
        if (objs.size == 1) {
            return extractNumericValue(objs.first())
        }
        return null
    }

    private fun extractNumericValue(obj: JsonObject): Double? {
        val keys = listOf("floatValue", "doubleValue", "integerValue", "longValue", "value")
        for (key in keys) {
            val prim = obj[key] as? JsonPrimitive ?: continue
            prim.doubleOrNull?.let { return it }
            prim.longOrNull?.let { return it.toDouble() }
            prim.contentOrNull?.toDoubleOrNull()?.let { return it }
        }
        return null
    }

    private fun JsonObject.stringOrNull(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.longOrNull(key: String): Long? {
        val prim = this[key] as? JsonPrimitive ?: return null
        return prim.longOrNull ?: prim.contentOrNull?.toLongOrNull()
    }

    private fun buildFormBody(vararg pairs: Pair<String, String>): String =
        pairs.joinToString("&") { (k, v) -> "${urlEncode(k)}=${urlEncode(v)}" }

    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")
}
