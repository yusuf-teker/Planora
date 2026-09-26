package com.yusufteker.planora.core.domain.usecase

import com.yusufteker.planora.core.database.PlanoraDatabase
import com.yusufteker.planora.core.database.clearAll
import com.yusufteker.planora.core.preferences.SessionPreferences
import com.yusufteker.planora.core.reminder.ReminderManager
import com.yusufteker.planora.shared.api.LogoutRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * UseCase to handle user logout across the entire application.
 *
 * Execution Steps:
 * 1. Retrieves current device's [SessionPreferences.getRefreshToken] and [SessionPreferences.getFcmToken].
 * 2. Sends a revocation request to `auth/logout` on the backend server so the refresh token
 *    and FCM token are revoked in the database.
 * 3. Cancels all scheduled local task alarms via [ReminderManager.cancelAllReminders].
 * 4. Clears all user data from the local SQLite database to prevent data leaks between accounts.
 * 5. Clears access/refresh tokens and profile preferences in DataStore/SecureSettings.
 *
 * @param httpClient The configured HTTP client for networking.
 * @param sessionPreferences Preferences manager for credentials and profile cache.
 * @param database The local SQLDelight database.
 * @param reminderManager Optional platform reminder scheduler to cancel active alarms.
 */
class LogoutUseCase(
    private val httpClient: HttpClient,
    private val sessionPreferences: SessionPreferences,
    private val database: PlanoraDatabase,
    private val reminderManager: ReminderManager? = null
) {
    /**
     * Executes the logout flow asynchronously on [Dispatchers.IO].
     */
    suspend operator fun invoke() {
        withContext(Dispatchers.IO) {
            try {
                val refreshToken = sessionPreferences.getRefreshToken()
                val fcmToken = sessionPreferences.getFcmToken()
                if (refreshToken != null || fcmToken != null) {
                    httpClient.post("auth/logout") {
                        setBody(LogoutRequest(refreshToken = refreshToken, fcmToken = fcmToken))
                    }
                }
            } catch (_: Exception) {
                // Network failures or offline state must not block local logout
            } finally {
                reminderManager?.cancelAllReminders()
                database.planoraDatabaseQueries.clearAll()
                sessionPreferences.clearSession()
            }
        }
    }
}
