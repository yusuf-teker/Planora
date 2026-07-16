package com.yusufteker.pulse.core.preferences

import androidx.datastore.core.DataStore

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.yusufteker.pulse.core.utils.TimelineViewOption

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Manages user session tokens (Access & Refresh) using SecureSettings (Keychain/EncryptedPrefs)
 * and non-sensitive user profile data using DataStore.
 */
class SessionPreferences(
    private val dataStore: DataStore<Preferences>,
    private val secureSettings: SecureSettings,
) {
    // Keys for Settings (Tokens)
    private val accessTokenKeyString = "access_token"
    private val refreshTokenKeyString = "refresh_token"
    private val fcmTokenKeyString = "fcm_token"

    // Keys for DataStore (Profile info)
    private val userNameKey = stringPreferencesKey("user_name")
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



    suspend fun getAccessToken(): String? {
        return secureSettings.settings.getStringOrNull(accessTokenKeyString)
    }

    suspend fun getRefreshToken(): String? {
        return secureSettings.settings.getStringOrNull(refreshTokenKeyString)
    }

    // Flow tabanlı: DataStore değişince otomatik güncellenir
    val userNameFlow: kotlinx.coroutines.flow.Flow<String?> =
        dataStore.data.map { it[userNameKey] }.distinctUntilChanged()

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

    suspend fun getUserName(): String? {
        return dataStore.data.map { it[userNameKey] }.first()
    }

    suspend fun getUserAvatar(): String? {
        return dataStore.data.map { it[userAvatarKey] }.first()
    }
    
    suspend fun getUserProfileImageUrl(): String? {
        return dataStore.data.map { it[userProfileImageUrlKey] }.first()
    }
    
    suspend fun getUserId(): String? {
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

    suspend fun saveUserProfile(userId: String, name: String, avatarId: String, profileImageUrl: String?, followersCount: Int = 0, followingCount: Int = 0) {
        dataStore.edit { prefs ->
            prefs[userIdKey] = userId
            prefs[userNameKey] = name
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
            prefs.remove(userAvatarKey)
            prefs.remove(userProfileImageUrlKey)
            prefs.remove(followersCountKey)
            prefs.remove(followingCountKey)
        }
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

    suspend fun getShowCompleted(): Boolean {
        return dataStore.data.map { it[filterShowCompletedKey] ?: true }.first()
    }

    suspend fun saveFilterOptions(showOnlyNextRecurring: Boolean, showCompleted: Boolean) {
        dataStore.edit { prefs ->
            prefs[filterShowOnlyNextRecurringKey] = showOnlyNextRecurring
            prefs[filterShowCompletedKey] = showCompleted
        }
    }

     suspend fun getViewOption(): TimelineViewOption {
         return try {
             TimelineViewOption.valueOf(dataStore.data.map { it[viewOptionKey] }.first() ?: TimelineViewOption.RELATIVE.name)
         } catch (e: Exception) {
             TimelineViewOption.RELATIVE
         }
    }
    suspend fun saveViewOption(timelineViewOption: TimelineViewOption) {
        dataStore.edit { prefs ->
            prefs[viewOptionKey] = timelineViewOption.name
        }
    }


   
}
