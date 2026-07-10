package com.yusufteker.pulse.core.utils

import com.yusufteker.pulse.core.domain.usecase.RegisterFcmTokenUseCase
import com.yusufteker.pulse.core.preferences.SessionPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object IosNotificationBridge : KoinComponent {
    private val sessionPreferences: SessionPreferences by inject()
    private val registerFcmTokenUseCase: RegisterFcmTokenUseCase by inject()

    fun setFcmToken(token: String) {
        CoroutineScope(Dispatchers.Main).launch {
            sessionPreferences.saveFcmToken(token)
            // If the user is logged in, register with the server immediately
            val userId = sessionPreferences.userIdFlow.first()
            if (userId != null) {
                registerFcmTokenUseCase()
            }
        }
    }
}
