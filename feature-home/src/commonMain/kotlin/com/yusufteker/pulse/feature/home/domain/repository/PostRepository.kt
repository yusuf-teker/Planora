package com.yusufteker.pulse.feature.home.domain.repository

import com.yusufteker.pulse.core.database.PulseDatabase
import com.yusufteker.pulse.feature.home.domain.sync.PostSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList

/**
 * Gönderi oluşturma işlemlerini yöneten Repository.
 * Temiz Mimari (Clean Architecture) kurallarına göre, UI katmanı doğrudan
 * veritabanına veya API'ye erişmez, bu repository ile iletişim kurar.
 */
class PostRepository(
    private val localDatabase: PulseDatabase,
    private val syncManager: PostSyncManager
) {

    /**
     * Kullanıcı yeni bir post yazdığında bu fonksiyon çağrılır.
     * 
     * Offline-First Mantığı:
     * 1. API'ye göndermeden önce doğrudan yerel SQLite veritabanına kaydederiz.
     * 2. Kaydedilen veriyi "Gönderilmeyi Bekliyor" (isDraft=0) veya "Taslak" (isDraft=1) olarak işaretleriz.
     * 3. Eğer taslak değilse, SyncManager'a "Kuyruktaki işleri başlat" emri veririz.
     */
    suspend fun createPost(content: String, isDraft: Boolean, topic: String) {
        withContext(Dispatchers.IO) {
            // Benzersiz bir ID oluşturuyoruz (Geçici olarak zaman damgası + rastgele sayı kullanıyoruz)
            val createdAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            val localId = "local_${createdAt}_${(0..10000).random()}"
            
            val isDraftInt = if (isDraft) 1L else 0L

            // 1. Yerel veritabanına kaydet
            localDatabase.pulseDatabaseQueries.insertPendingPost(
                id = localId,
                content = content,
                isDraft = isDraftInt,
                createdAt = createdAt,
                topic = topic
            )

            // 2. Eğer taslak değilse (Yani kullanıcı Paylaş butonuna bastıysa), 
            // senkronizasyon işlemini tetikle.
            if (!isDraft) {
                syncManager.syncPendingPosts()
            }
        }
    }

    /**
     * Tüm bekleyen gönderileri (taslaklar ve gönderilmeyi bekleyenler) döndürür.
     */
    fun getAllPendingPosts(): kotlinx.coroutines.flow.Flow<List<com.yusufteker.pulse.core.database.PendingPostEntity>> {
        return localDatabase.pulseDatabaseQueries.getAllPendingPosts().asFlow()
            .mapToList(Dispatchers.IO)
    }

    /**
     * Belirtilen ID'ye sahip bekleyen gönderiyi döndürür.
     */
    suspend fun getPendingPostById(id: String): com.yusufteker.pulse.core.database.PendingPostEntity? {
        return withContext(Dispatchers.IO) {
            localDatabase.pulseDatabaseQueries.getAllPendingPosts().executeAsList().find { it.id == id }
        }
    }

    /**
     * Var olan bir taslağı günceller veya tekrar gönderim kuyruğuna sokar.
     */
    suspend fun updatePendingPost(id: String, content: String, isDraft: Boolean, topic: String) {
        withContext(Dispatchers.IO) {
            val isDraftInt = if (isDraft) 1L else 0L
            val existingPost = localDatabase.pulseDatabaseQueries.getAllPendingPosts().executeAsList().find { it.id == id }
            
            if (existingPost != null) {
                localDatabase.pulseDatabaseQueries.insertPendingPost(
                    id = existingPost.id,
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
            localDatabase.pulseDatabaseQueries.deletePendingPost(id)
        }
    }
}
