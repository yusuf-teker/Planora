package com.yusufteker.pulse.feature.auth.domain.usecase

import com.yusufteker.pulse.feature.auth.domain.repository.AuthRepository
import com.yusufteker.pulse.core.database.PulsyDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class LogoutUseCase(
    private val authRepository: AuthRepository,
    private val pulsyDatabase: PulsyDatabase
) {
    suspend operator fun invoke() {
        withContext(Dispatchers.IO) {
            authRepository.logout()
            // Güvenlik: Başka hesaba geçildiğinde verilerin karışmaması için yerel DB'yi temizliyoruz
            pulsyDatabase.pulsyDatabaseQueries.clearAll()
        }
    }
}
