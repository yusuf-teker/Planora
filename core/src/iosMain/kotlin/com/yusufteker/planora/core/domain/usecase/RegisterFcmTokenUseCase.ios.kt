package com.yusufteker.planora.core.domain.usecase

import com.yusufteker.planora.core.data.api.FcmApi
import com.yusufteker.planora.core.preferences.SessionPreferences
import io.github.aakira.napier.Napier
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual class RegisterFcmTokenUseCase actual constructor(
    private val fcmApi: FcmApi
) : KoinComponent {
    private val sessionPreferences: SessionPreferences?
        get() = runCatching { getKoin().get<SessionPreferences>() }.getOrNull()

    actual suspend operator fun invoke() {
        try {
            val prefs = sessionPreferences
            if (prefs == null) {
                Napier.w("SessionPreferences not available for FCM registration", tag = "RegisterFcmToken")
                return
            }
            val token = prefs.getFcmToken()
            if (token != null) {
                Napier.d("Registering iOS FCM Token: $token", tag = "RegisterFcmToken")
                val result = fcmApi.registerToken(token, "ios")
                if (result.isSuccess) {
                    Napier.d("iOS FCM Token registered successfully", tag = "RegisterFcmToken")
                } else {
                    Napier.e("Failed to register iOS FCM token", result.exceptionOrNull(), tag = "RegisterFcmToken")
                }
            } else {
                Napier.w("No iOS FCM Token found in SessionPreferences", tag = "RegisterFcmToken")
            }
        } catch (e: Exception) {
            Napier.e("Error registering iOS FCM token", e, tag = "RegisterFcmToken")
        }
    }
}
