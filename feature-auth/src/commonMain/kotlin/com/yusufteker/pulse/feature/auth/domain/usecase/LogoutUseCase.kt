package com.yusufteker.pulse.feature.auth.domain.usecase

import com.yusufteker.pulse.feature.auth.domain.repository.AuthRepository
import com.yusufteker.pulse.core.database.PulseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class LogoutUseCase(
    private val authRepository: AuthRepository,
    private val pulseDatabase: PulseDatabase
) {
    suspend operator fun invoke() {
        withContext(Dispatchers.IO) {
            authRepository.logout()
            // We NO LONGER clear local data on logout.
            // Data is scoped to ownerId, allowing for offline access and multi-tenant support.
        }
    }
}
