package com.yusufteker.pulse.core.domain.usecase

import com.yusufteker.pulse.core.data.api.FcmApi
expect class RegisterFcmTokenUseCase(
    fcmApi: FcmApi
) {
    suspend operator fun invoke()
}

expect fun getPlatformName(): String
