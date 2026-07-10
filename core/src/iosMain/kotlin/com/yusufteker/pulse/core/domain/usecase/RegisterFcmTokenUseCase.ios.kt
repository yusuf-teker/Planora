package com.yusufteker.pulse.core.domain.usecase

import com.yusufteker.pulse.core.data.api.FcmApi
import com.yusufteker.pulse.core.preferences.SessionPreferences
import io.github.aakira.napier.Napier
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual class RegisterFcmTokenUseCase actual constructor(
    private val fcmApi: FcmApi
) : KoinComponent {
    private val sessionPreferences: SessionPreferences by inject()

    actual suspend operator fun invoke() {
        try {
            val token = sessionPreferences.getFcmToken()
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
