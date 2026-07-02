package com.yusufteker.pulse.feature.home.domain.repository

import com.yusufteker.pulse.core.database.PulsyDatabase
import com.yusufteker.pulse.core.preferences.SessionPreferences
import com.yusufteker.pulse.feature.home.domain.sync.PostSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * Gönderi oluşturma işlemlerini yöneten Repository.
 * Temiz Mimari (Clean Architecture) kurallarına göre, UI katmanı doğrudan
 * veritabanına veya API'ye erişmez, bu repository ile iletişim kurar.
 */
import kotlin.random.Random

class PostRepository(
    private val localDatabase: PulsyDatabase,
    private val syncManager: PostSyncManager,
    private val sessionPreferences: SessionPreferences
) {

    /**
     * Kullanıcı yeni bir post yazdığında bu fonksiyon çağrılır.
     */
    suspend fun createPost(content: String, isDraft: Boolean, topic: String) {
        withContext(Dispatchers.IO) {
            val ownerId = sessionPreferences.getOwnerId()
            val createdAt = Random.nextLong(1000000L, 9000000L)
            val localId = "local_${createdAt}_${(0..10000).random()}"
            val isDraftInt = if (isDraft) 1L else 0L

            localDatabase.pulsyDatabaseQueries.insertPendingPost(
                id = localId,
                ownerId = ownerId,
                content = content,
                isDraft = isDraftInt,
                createdAt = createdAt,
                topic = topic
            )

            if (!isDraft) {
                syncManager.syncPendingPosts()
            }
        }
    }

    /**
     * Tüm bekleyen gönderileri (taslaklar ve gönderilmeyi bekleyenler) döndürür.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getAllPendingPosts(): Flow<List<com.yusufteker.pulse.core.database.PendingPostEntity>> {
        return sessionPreferences.userIdFlow.flatMapLatest { userId ->
            val ownerId = userId ?: "guest"
            localDatabase.pulsyDatabaseQueries.getAllPendingPosts(ownerId).asFlow().mapToList(Dispatchers.IO)
        }
    }

    /**
     * Belirtilen ID'ye sahip bekleyen gönderiyi döndürür.
     */
    suspend fun getPendingPostById(id: String): com.yusufteker.pulse.core.database.PendingPostEntity? {
        return withContext(Dispatchers.IO) {
            val ownerId = sessionPreferences.getOwnerId()
            localDatabase.pulsyDatabaseQueries.getAllPendingPosts(ownerId).executeAsList().find { it.id == id }
        }
    }

    /**
     * Var olan bir taslağı günceller veya tekrar gönderim kuyruğuna sokar.
     */
    suspend fun updatePendingPost(id: String, content: String, isDraft: Boolean, topic: String) {
        withContext(Dispatchers.IO) {
            val ownerId = sessionPreferences.getOwnerId()
            val isDraftInt = if (isDraft) 1L else 0L
            val existingPost = localDatabase.pulsyDatabaseQueries.getAllPendingPosts(ownerId).executeAsList().find { it.id == id }
            
            if (existingPost != null) {
                localDatabase.pulsyDatabaseQueries.insertPendingPost(
                    id = existingPost.id,
                    ownerId = existingPost.ownerId,
                    content = content,
                    isDraft = isDraftInt,
                    createdAt = existingPost.createdAt, // Mevcut oluşturulma tarihini koru
                    topic = topic
                )
            }

            if (!isDraft) {
                syncManager.syncPendingPosts()
            }
        }
    }

    /**
     * Bekleyen gönderiyi (veya taslağı) veritabanından siler.
     */
    suspend fun deletePendingPost(id: String) {
        withContext(Dispatchers.IO) {
            localDatabase.pulsyDatabaseQueries.deletePendingPost(id)
        }
    }
}
