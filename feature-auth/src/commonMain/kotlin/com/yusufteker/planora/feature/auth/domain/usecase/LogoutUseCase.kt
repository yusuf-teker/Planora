package com.yusufteker.planora.feature.auth.domain.usecase

import com.yusufteker.planora.feature.auth.domain.repository.AuthRepository
import com.yusufteker.planora.core.database.PlanoraDatabase
import com.yusufteker.planora.core.database.clearAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class LogoutUseCase(
    private val authRepository: AuthRepository,
    private val planoraDatabase: PlanoraDatabase
) {
    suspend operator fun invoke() {
        withContext(Dispatchers.IO) {
            authRepository.logout()
            // Güvenlik: Başka hesaba geçildiğinde verilerin karışmaması için yerel DB'yi temizliyoruz
            planoraDatabase.planoraDatabaseQueries.clearAll()
        }
    }
}
