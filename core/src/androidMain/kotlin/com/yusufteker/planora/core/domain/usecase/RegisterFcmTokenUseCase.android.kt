package com.yusufteker.planora.core.domain.usecase

import com.yusufteker.planora.core.data.api.FcmApi
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.messaging.messaging
import io.github.aakira.napier.Napier

actual class RegisterFcmTokenUseCase actual constructor(
    private val fcmApi: FcmApi
) {
    actual suspend operator fun invoke() {
        try {
            val token = Firebase.messaging.getToken()
            Napier.d("Obtained FCM Token: $token", tag = "RegisterFcmToken")
            val platform = getPlatformName()
            val result = fcmApi.registerToken(token, platform)
            if (result.isSuccess) {
                Napier.d("FCM Token registered successfully", tag = "RegisterFcmToken")
            } else {
                Napier.e("Failed to register FCM token", result.exceptionOrNull(), tag = "RegisterFcmToken")
            }
        } catch (e: Exception) {
            Napier.e("Error getting FCM token", e, tag = "RegisterFcmToken")
        }
    }
}
