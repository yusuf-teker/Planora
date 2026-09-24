package com.yusufteker.planora.feature.home.util

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.russhwolf.settings.Settings
import com.yusufteker.planora.core.preferences.SecureSettings
import com.yusufteker.planora.core.preferences.SessionPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import okio.Path.Companion.toPath
import kotlin.random.Random

/**
 * %100 Saf Kotlin InMemory Settings implementasyonu.
 * Testler için harici kütüphane bağımlılığı olmaksızın çalışır.
 */
class InMemorySettings : Settings {
    private val storage = mutableMapOf<String, Any>()

    override val keys: Set<String> get() = storage.keys
    override val size: Int get() = storage.size

    override fun clear() = storage.clear()
    override fun remove(key: String) { storage.remove(key) }
    override fun hasKey(key: String): Boolean = storage.containsKey(key)

    override fun putInt(key: String, value: Int) { storage[key] = value }
    override fun getInt(key: String, defaultValue: Int): Int = storage[key] as? Int ?: defaultValue
    override fun getIntOrNull(key: String): Int? = storage[key] as? Int

    override fun putLong(key: String, value: Long) { storage[key] = value }
    override fun getLong(key: String, defaultValue: Long): Long = storage[key] as? Long ?: defaultValue
    override fun getLongOrNull(key: String): Long? = storage[key] as? Long

    override fun putString(key: String, value: String) { storage[key] = value }
    override fun getString(key: String, defaultValue: String): String = storage[key] as? String ?: defaultValue
    override fun getStringOrNull(key: String): String? = storage[key] as? String

    override fun putFloat(key: String, value: Float) { storage[key] = value }
    override fun getFloat(key: String, defaultValue: Float): Float = storage[key] as? Float ?: defaultValue
    override fun getFloatOrNull(key: String): Float? = storage[key] as? Float

    override fun putDouble(key: String, value: Double) { storage[key] = value }
    override fun getDouble(key: String, defaultValue: Double): Double = storage[key] as? Double ?: defaultValue
    override fun getDoubleOrNull(key: String): Double? = storage[key] as? Double

    override fun putBoolean(key: String, value: Boolean) { storage[key] = value }
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = storage[key] as? Boolean ?: defaultValue
    override fun getBooleanOrNull(key: String): Boolean? = storage[key] as? Boolean
}

/**
 * Testler için %100 InMemory Fake SessionPreferences implementasyonu.
 * Disk I/O gecikmelerini ve Coroutine TestDispatcher kilitlenmelerini engeller.
 */
open class FakeSessionPreferences : SessionPreferences(
    dataStore = PreferenceDataStoreFactory.createWithPath(produceFile = { "/tmp/fake_ds_${Random.nextLong()}.preferences_pb".toPath() }),
    secureSettings = SecureSettings(InMemorySettings())
) {
    private val _isPremiumState = MutableStateFlow(false)
    private val _premiumUntilState = MutableStateFlow<String?>(null)

    override val isPremiumFlow: Flow<Boolean> = kotlinx.coroutines.flow.combine(_isPremiumState, _premiumUntilState) { isPrem, untilStr ->
        if (!isPrem) return@combine false
        if (!untilStr.isNullOrBlank()) {
            try {
                val untilMs = kotlinx.datetime.Instant.parse(untilStr).toEpochMilliseconds()
                val nowMs = com.yusufteker.planora.core.utils.getCurrentTimeMs()
                if (nowMs > untilMs) return@combine false
            } catch (_: Exception) {}
        }
        true
    }

    var userIdOverride: String? = "101"
    var userNameOverride: String? = "Test User"

    override suspend fun isPremium(): Boolean {
        if (!_isPremiumState.value) return false
        val untilStr = _premiumUntilState.value
        if (!untilStr.isNullOrBlank()) {
            try {
                val untilMs = kotlinx.datetime.Instant.parse(untilStr).toEpochMilliseconds()
                val nowMs = com.yusufteker.planora.core.utils.getCurrentTimeMs()
                if (nowMs > untilMs) {
                    setPremium(false, null)
                    return false
                }
            } catch (_: Exception) {}
        }
        return true
    }

    override suspend fun setPremium(isPremium: Boolean, premiumUntil: String?) {
        _isPremiumState.value = isPremium
        _premiumUntilState.value = premiumUntil
    }

    suspend fun setPremium(isPremium: Boolean) {
        setPremium(isPremium, null)
    }

    override val premiumUntilFlow: Flow<String?> = _premiumUntilState

    override suspend fun getPremiumUntil(): String? = _premiumUntilState.value

    override suspend fun getUserId(): String? = userIdOverride

    override suspend fun getUserName(): String? = userNameOverride

    private var _fakeAiQuota: com.yusufteker.planora.shared.ai.AiQuotaDto? = null

    override suspend fun getAiQuota(isPremium: Boolean): com.yusufteker.planora.shared.ai.AiQuotaDto {
        return _fakeAiQuota ?: com.yusufteker.planora.shared.ai.AiQuotaDto(
            isPremium = isPremium,
            dailyRemaining = if (isPremium) 50 else 1,
            dailyLimit = if (isPremium) 50 else 1,
            weeklyRemaining = if (isPremium) 300 else 3,
            weeklyLimit = if (isPremium) 300 else 3
        )
    }

    override suspend fun saveAiQuota(quota: com.yusufteker.planora.shared.ai.AiQuotaDto) {
        _fakeAiQuota = quota
    }

    override suspend fun decrementAiQuota(isPremium: Boolean): com.yusufteker.planora.shared.ai.AiQuotaDto {
        val current = getAiQuota(isPremium)
        val updated = current.copy(
            dailyRemaining = (current.dailyRemaining - 1).coerceAtLeast(0),
            weeklyRemaining = (current.weeklyRemaining - 1).coerceAtLeast(0)
        )
        _fakeAiQuota = updated
        return updated
    }
}

/**
 * Testler için InMemory SessionPreferences üretici fonksiyonu.
 */
fun createTestSessionPreferences(): SessionPreferences = FakeSessionPreferences()

typealias TestSessionPreferences = FakeSessionPreferences
