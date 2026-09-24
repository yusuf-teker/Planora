package com.yusufteker.planora.core.preferences

import androidx.datastore.core.DataStore

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.yusufteker.planora.core.utils.TimelineViewOption

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.yusufteker.planora.shared.ai.AiQuotaDto

/**
 * Manages user session tokens (Access & Refresh) using SecureSettings (Keychain/EncryptedPrefs)
 * and non-sensitive user profile data using DataStore.
 */
open class SessionPreferences(
    private val dataStore: DataStore<Preferences>,
    private val secureSettings: SecureSettings,
) {
    companion object {
        const val DEFAULT_AI_DAILY_LIMIT_FREE = 1
        const val DEFAULT_AI_DAILY_LIMIT_PREMIUM = 50
        const val DEFAULT_AI_WEEKLY_LIMIT_FREE = 3
        const val DEFAULT_AI_WEEKLY_LIMIT_PREMIUM = 300
    }
    // Keys for Settings (Tokens)
    private val accessTokenKeyString = "access_token"
    private val refreshTokenKeyString = "refresh_token"
    private val fcmTokenKeyString = "fcm_token"

    // Keys for DataStore (Profile info)
    private val userNameKey = stringPreferencesKey("user_name")
    private val userHandleKey = stringPreferencesKey("user_handle")
    private val userAvatarKey = stringPreferencesKey("user_avatar")
    private val userProfileImageUrlKey = stringPreferencesKey("user_profile_image_url")
    private val userIdKey = stringPreferencesKey("user_id")
    private val followersCountKey = intPreferencesKey("followers_count")
    private val followingCountKey = intPreferencesKey("following_count")
    private val pendingFollowRequestsCountKey = intPreferencesKey("pending_follow_requests_count")
    private val pendingCalendarRequestsCountKey = intPreferencesKey("pending_calendar_requests_count")
    private val appRunKey = androidx.datastore.preferences.core.booleanPreferencesKey("has_run_before")
    private val lastLoggedUserIdKey = stringPreferencesKey("last_logged_user_id")
    private val filterShowOnlyNextRecurringKey = androidx.datastore.preferences.core.booleanPreferencesKey("filter_show_only_next_recurring")
    private val filterShowCompletedKey = androidx.datastore.preferences.core.booleanPreferencesKey("filter_show_completed")
    private val viewOptionKey = stringPreferencesKey("view_option")
    private val lastSeenOverviewIndexKey = intPreferencesKey("last_seen_overview_index")
    private val isPremiumKey = booleanPreferencesKey("is_premium")
    private val premiumUntilKey = stringPreferencesKey("premium_until")
    private val aiDailyRemainingKey = intPreferencesKey("ai_daily_remaining")
    private val aiDailyLimitKey = intPreferencesKey("ai_daily_limit")
    private val aiWeeklyRemainingKey = intPreferencesKey("ai_weekly_remaining")
    private val aiWeeklyLimitKey = intPreferencesKey("ai_weekly_limit")
    private val aiLastQuotaDateKey = stringPreferencesKey("ai_last_quota_date")

    suspend fun getAccessToken(): String? {
        return secureSettings.settings.getStringOrNull(accessTokenKeyString)
    }

    suspend fun getRefreshToken(): String? {
        return secureSettings.settings.getStringOrNull(refreshTokenKeyString)
    }

    // Flow tabanlı: DataStore değişince otomatik güncellenir
    val userNameFlow: kotlinx.coroutines.flow.Flow<String?> =
        dataStore.data.map { it[userNameKey] }.distinctUntilChanged()

    val userHandleFlow: kotlinx.coroutines.flow.Flow<String?> =
        dataStore.data.map { it[userHandleKey] }.distinctUntilChanged()

    val userAvatarFlow: kotlinx.coroutines.flow.Flow<String?> =
        dataStore.data.map { it[userAvatarKey] }.distinctUntilChanged()
        
    val userProfileImageUrlFlow: kotlinx.coroutines.flow.Flow<String?> =
        dataStore.data.map { it[userProfileImageUrlKey] }.distinctUntilChanged()
        
    val userIdFlow: kotlinx.coroutines.flow.Flow<String?> =
        dataStore.data.map { it[userIdKey] }.distinctUntilChanged()
        
    val followersCountFlow: kotlinx.coroutines.flow.Flow<Int> =
        dataStore.data.map { it[followersCountKey] ?: 0 }.distinctUntilChanged()
        
    val followingCountFlow: kotlinx.coroutines.flow.Flow<Int> =
        dataStore.data.map { it[followingCountKey] ?: 0 }.distinctUntilChanged()

    val pendingFollowRequestsCountFlow: kotlinx.coroutines.flow.Flow<Int> =
        dataStore.data.map { it[pendingFollowRequestsCountKey] ?: 0 }.distinctUntilChanged()

    suspend fun updatePendingFollowRequestsCount(count: Int) {
        dataStore.edit { prefs ->
            prefs[pendingFollowRequestsCountKey] = count
        }
    }

    suspend fun incrementPendingFollowRequestsCount() {
        dataStore.edit { prefs ->
            val current = prefs[pendingFollowRequestsCountKey] ?: 0
            prefs[pendingFollowRequestsCountKey] = current + 1
        }
    }

    val pendingCalendarRequestsCountFlow: kotlinx.coroutines.flow.Flow<Int> =
        dataStore.data.map { it[pendingCalendarRequestsCountKey] ?: 0 }.distinctUntilChanged()

    suspend fun updatePendingCalendarRequestsCount(count: Int) {
        dataStore.edit { prefs ->
            prefs[pendingCalendarRequestsCountKey] = count
        }
    }

    suspend fun incrementPendingCalendarRequestsCount() {
        dataStore.edit { prefs ->
            val current = prefs[pendingCalendarRequestsCountKey] ?: 0
            prefs[pendingCalendarRequestsCountKey] = current + 1
        }
    }

    open suspend fun getUserName(): String? {
        return dataStore.data.map { it[userNameKey] }.first()
    }

    suspend fun getUserHandle(): String? {
        return dataStore.data.map { it[userHandleKey] }.first()
    }

    suspend fun getUserAvatar(): String? {
        return dataStore.data.map { it[userAvatarKey] }.first()
    }
    
    suspend fun getUserProfileImageUrl(): String? {
        return dataStore.data.map { it[userProfileImageUrlKey] }.first()
    }
    
    open suspend fun getUserId(): String? {
        return dataStore.data.map { it[userIdKey] }.first()
    }
    
    suspend fun getOwnerId(): String {
        return getUserId() ?: "guest"
    }

    suspend fun isFirstRun(): Boolean {
        val hasRun = dataStore.data.map { it[appRunKey] }.first()
        return hasRun != true
    }

    suspend fun setHasRunBefore(hasRun: Boolean) {
        dataStore.edit { prefs ->
            prefs[appRunKey] = hasRun
        }
    }

    suspend fun getLastSeenOverviewIndex(): Int {
        return dataStore.data.map { it[lastSeenOverviewIndexKey] ?: 0 }.first()
    }

    suspend fun setLastSeenOverviewIndex(index: Int) {
        dataStore.edit { prefs ->
            prefs[lastSeenOverviewIndexKey] = index
        }
    }

    suspend fun getLastLoggedUserId(): String? {
        return dataStore.data.first()[lastLoggedUserIdKey]
    }

    suspend fun setLastLoggedUserId(userId: String) {
        dataStore.edit { prefs ->
            prefs[lastLoggedUserIdKey] = userId
        }
    }

    suspend fun markAppAsRun() {
        dataStore.edit { prefs ->
            prefs[appRunKey] = true
        }
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        secureSettings.settings.putString(accessTokenKeyString, accessToken)
        secureSettings.settings.putString(refreshTokenKeyString, refreshToken)
    }

    suspend fun saveFcmToken(token: String) {
        secureSettings.settings.putString(fcmTokenKeyString, token)
    }

    suspend fun getFcmToken(): String? {
        return secureSettings.settings.getStringOrNull(fcmTokenKeyString)
    }

    suspend fun saveUserProfile(userId: String, name: String, avatarId: String, profileImageUrl: String?, followersCount: Int = 0, followingCount: Int = 0, username: String? = null) {
        dataStore.edit { prefs ->
            prefs[userIdKey] = userId
            prefs[userNameKey] = name
            if (!username.isNullOrBlank()) {
                prefs[userHandleKey] = username
            }
            prefs[userAvatarKey] = avatarId
            if (profileImageUrl != null) {
                prefs[userProfileImageUrlKey] = profileImageUrl
            } else {
                prefs.remove(userProfileImageUrlKey)
            }
            prefs[followersCountKey] = followersCount
            prefs[followingCountKey] = followingCount
        }
    }

    suspend fun updateProfileData(name: String, avatarId: String, profileImageUrl: String?) {
        dataStore.edit { prefs ->
            prefs[userNameKey] = name
            prefs[userAvatarKey] = avatarId
            if (profileImageUrl != null) {
                prefs[userProfileImageUrlKey] = profileImageUrl
            } else {
                prefs.remove(userProfileImageUrlKey)
            }
        }
    }

    suspend fun clearSession() {
        // Clear secure tokens
        secureSettings.settings.remove(accessTokenKeyString)
        secureSettings.settings.remove(refreshTokenKeyString)
        
        // Clear DataStore profile info (Logout means you become guest, data stays in DB but you can't see it until you login)
        dataStore.edit { prefs ->
            prefs.remove(userIdKey)
            prefs.remove(userNameKey)
            prefs.remove(userHandleKey)
            prefs.remove(userAvatarKey)
            prefs.remove(userProfileImageUrlKey)
            prefs.remove(followersCountKey)
            prefs.remove(followingCountKey)
            prefs.remove(isPremiumKey)
            prefs.remove(premiumUntilKey)
            prefs.remove(aiDailyRemainingKey)
            prefs.remove(aiDailyLimitKey)
            prefs.remove(aiWeeklyRemainingKey)
            prefs.remove(aiWeeklyLimitKey)
            prefs.remove(aiLastQuotaDateKey)
        }
    }

    /**
     * Flow emitting whether the user's Premium membership is currently active.
     * Evaluates both the boolean flag and validates [premiumUntil] against the current time.
     * If the subscription duration has elapsed, immediately emits false.
     */
    open val isPremiumFlow: kotlinx.coroutines.flow.Flow<Boolean> =
        dataStore.data.map { prefs ->
            val isPrem = prefs[isPremiumKey] ?: false
            if (!isPrem) return@map false
            val untilStr = prefs[premiumUntilKey]
            if (!untilStr.isNullOrBlank()) {
                try {
                    val untilMs = Instant.parse(untilStr).toEpochMilliseconds()
                    val nowMs = com.yusufteker.planora.core.utils.getCurrentTimeMs()
                    if (nowMs > untilMs) {
                        return@map false
                    }
                } catch (_: Exception) {
                    // If parsing fails, fall back to isPrem
                }
            }
            true
        }.distinctUntilChanged()

    /**
     * Checks if the user is currently Premium.
     * Automatically invalidates and resets local state if [premiumUntil] has expired.
     */
    open suspend fun isPremium(): Boolean {
        val prefs = dataStore.data.first()
        val isPrem = prefs[isPremiumKey] ?: false
        if (!isPrem) return false
        val untilStr = prefs[premiumUntilKey]
        if (!untilStr.isNullOrBlank()) {
            try {
                val untilMs = Instant.parse(untilStr).toEpochMilliseconds()
                val nowMs = com.yusufteker.planora.core.utils.getCurrentTimeMs()
                if (nowMs > untilMs) {
                    // Auto-downgrade local state immediately
                    setPremium(isPremium = false, premiumUntil = null)
                    return false
                }
            } catch (_: Exception) {
                // If parsing fails, fall back to isPrem
            }
        }
        return true
    }

    /**
     * Updates the local Premium membership status and expiration timestamp.
     */
    open suspend fun setPremium(isPremium: Boolean, premiumUntil: String? = null) {
        dataStore.edit { prefs ->
            prefs[isPremiumKey] = isPremium
            if (premiumUntil != null) {
                prefs[premiumUntilKey] = premiumUntil
            } else {
                prefs.remove(premiumUntilKey)
            }
        }
    }

    /**
     * Flow emitting the current subscription expiration timestamp string (ISO-8601).
     */
    open val premiumUntilFlow: kotlinx.coroutines.flow.Flow<String?> =
        dataStore.data.map { it[premiumUntilKey] }.distinctUntilChanged()

    /**
     * Returns the current subscription expiration timestamp string (ISO-8601), if any.
     */
    open suspend fun getPremiumUntil(): String? {
        return dataStore.data.first()[premiumUntilKey]
    }
    
    suspend fun updateFollowCounts(followersDelta: Int, followingDelta: Int) {
        dataStore.edit { prefs ->
            val currentFollowers = prefs[followersCountKey] ?: 0
            val currentFollowing = prefs[followingCountKey] ?: 0
            prefs[followersCountKey] = currentFollowers + followersDelta
            prefs[followingCountKey] = currentFollowing + followingDelta
        }
    }

    suspend fun getShowOnlyNextRecurring(): Boolean {
        return dataStore.data.map { it[filterShowOnlyNextRecurringKey] ?: true }.first()
    }

    private val filterShowRoomTasksKey = booleanPreferencesKey("filter_show_room_tasks")

    suspend fun getShowCompleted(): Boolean {
        return dataStore.data.map { it[filterShowCompletedKey] ?: true }.first()
    }

    suspend fun getShowRoomTasks(): Boolean {
        return dataStore.data.map { it[filterShowRoomTasksKey] ?: true }.first()
    }

    suspend fun saveFilterOptions(showOnlyNextRecurring: Boolean, showCompleted: Boolean, showRoomTasks: Boolean = true) {
        dataStore.edit { prefs ->
            prefs[filterShowOnlyNextRecurringKey] = showOnlyNextRecurring
            prefs[filterShowCompletedKey] = showCompleted
            prefs[filterShowRoomTasksKey] = showRoomTasks
        }
    }

     suspend fun getViewOption(): TimelineViewOption {
         return try {
             TimelineViewOption.valueOf(dataStore.data.map { it[viewOptionKey] }.first() ?: TimelineViewOption.DATE.name)
         } catch (e: Exception) {
             TimelineViewOption.DATE
         }
    }
    suspend fun saveViewOption(timelineViewOption: TimelineViewOption) {
        dataStore.edit { prefs ->
            prefs[viewOptionKey] = timelineViewOption.name
        }
    }

    /**
     * Yerel olarak saklanan AI kullanım kotasını çeker.
     * Gün değiştiğinde günlük kotayı otomatik olarak limitine sıfırlar.
     *
     * @param isPremium Kullanıcının Premium durumu (limitleri belirler)
     * @return Güncel [AiQuotaDto] kota bilgisi
     */
    open suspend fun getAiQuota(isPremium: Boolean): AiQuotaDto {
        val prefs = dataStore.data.first()
        val defaultDailyLimit = if (isPremium) DEFAULT_AI_DAILY_LIMIT_PREMIUM else DEFAULT_AI_DAILY_LIMIT_FREE
        val defaultWeeklyLimit = if (isPremium) DEFAULT_AI_WEEKLY_LIMIT_PREMIUM else DEFAULT_AI_WEEKLY_LIMIT_FREE

        val todayDate = try {
            val nowMs = com.yusufteker.planora.core.utils.getCurrentTimeMs()
            Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        } catch (_: Exception) {
            ""
        }

        val lastDate = prefs[aiLastQuotaDateKey] ?: ""
        val isNewDay = lastDate.isNotBlank() && lastDate != todayDate

        val dailyLimit = prefs[aiDailyLimitKey] ?: defaultDailyLimit
        val weeklyLimit = prefs[aiWeeklyLimitKey] ?: defaultWeeklyLimit

        val dailyRemaining = if (isNewDay) dailyLimit else (prefs[aiDailyRemainingKey] ?: dailyLimit)
        val weeklyRemaining = prefs[aiWeeklyRemainingKey] ?: weeklyLimit

        return AiQuotaDto(
            isPremium = isPremium,
            dailyRemaining = dailyRemaining,
            dailyLimit = dailyLimit,
            weeklyRemaining = weeklyRemaining,
            weeklyLimit = weeklyLimit
        )
    }

    /**
     * Sunucudan veya işlem sonrasından gelen güncel AI kotasını yerel DataStore'a kaydeder.
     *
     * @param quota Kaydedilecek kota modeli
     */
    open suspend fun saveAiQuota(quota: AiQuotaDto) {
        val todayDate = try {
            val nowMs = com.yusufteker.planora.core.utils.getCurrentTimeMs()
            Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        } catch (_: Exception) {
            ""
        }

        dataStore.edit { prefs ->
            prefs[aiDailyRemainingKey] = quota.dailyRemaining
            prefs[aiDailyLimitKey] = quota.dailyLimit
            prefs[aiWeeklyRemainingKey] = quota.weeklyRemaining
            prefs[aiWeeklyLimitKey] = quota.weeklyLimit
            if (todayDate.isNotBlank()) {
                prefs[aiLastQuotaDateKey] = todayDate
            }
        }
    }

    /**
     * Her AI mesajı gönderildiğinde kotadan 1 adet düşer ve güncellenmiş durumu kaydeder.
     *
     * @param isPremium Kullanıcının Premium durumu
     * @return 1 adet düşülmüş yeni [AiQuotaDto]
     */
    open suspend fun decrementAiQuota(isPremium: Boolean): AiQuotaDto {
        val current = getAiQuota(isPremium)
        val updated = current.copy(
            dailyRemaining = (current.dailyRemaining - 1).coerceAtLeast(0),
            weeklyRemaining = (current.weeklyRemaining - 1).coerceAtLeast(0)
        )
        saveAiQuota(updated)
        return updated
    }
}
