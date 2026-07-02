package com.yusufteker.pulse.core.domain.usecase

import com.yusufteker.pulse.core.data.api.FcmApi

actual class RegisterFcmTokenUseCase actual constructor(
    private val fcmApi: FcmApi
) {
    actual suspend operator fun invoke() {
        // iOS implementation deferred to Phase 2
    }
}
