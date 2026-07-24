package com.yusufteker.planora.core.domain.usecase

import com.yusufteker.planora.core.data.api.FcmApi
expect class RegisterFcmTokenUseCase(
    fcmApi: FcmApi
) {
    suspend operator fun invoke()
}

expect fun getPlatformName(): String
