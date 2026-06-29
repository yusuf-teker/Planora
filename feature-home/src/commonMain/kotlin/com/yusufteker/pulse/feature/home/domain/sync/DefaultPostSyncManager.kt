package com.yusufteker.pulse.feature.home.domain.sync

import com.yusufteker.pulse.core.database.PulseDatabase
import com.yusufteker.pulse.feature.home.data.api.FeedApi
import com.yusufteker.pulse.shared.api.CreatePostRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

/**
 * Bu sınıf KMP (Ortak Kod) için varsayılan bir senkronizasyon yöneticisidir.
 * Arka planda çalışmak yerine uygulama açıkken Coroutine kullanarak çalışır.
 * İleride Android WorkManager bağlandığında bu mantık WorkWorker içine taşınabilir.
 */
class DefaultPostSyncManager(
    private val database: PulseDatabase,
    private val api: FeedApi
) : PostSyncManager {

    private val scope = CoroutineScope(Dispatchers.IO)

    override suspend fun syncPendingPosts() {
        // Coroutine başlatarak işlemi arka plana atıyoruz (UI'ı dondurmamak için)
        scope.launch {
            try {
                // 1. Veritabanından "Gönderilmeyi Bekleyen" (Taslak olmayan) postları çek.
                val pendingPosts = database.pulseDatabaseQueries.getPendingPostsToSync().executeAsList()

                // 2. Her bir post için API'ye istek at.
                for (post in pendingPosts) {
                    val request = CreatePostRequest(content = post.content, topic = post.topic)
                    val result = api.createPost(request)

                    if (result.isSuccess) {
                        // 3. Başarılı olursa, yerel veritabanındaki kuyruktan sil.
                        database.pulseDatabaseQueries.deletePendingPost(post.id)
                        
                        // İsteğe bağlı olarak: postEntity tablosuna eklenebilir veya 
                        // feed sayfası pull-to-refresh yapıldığında yeni veri otomatik gelir.
                    } else {
                        // Başarısız olursa silmeyiz, bir sonraki senkronizasyonda tekrar dener.
                        // "Offline-first" mantığının özü budur.
                    }
                }
            } catch (e: Exception) {
                // Hata durumunda (Örn: İnternet yok), postlar veritabanında kalmaya devam eder.
            }
        }
    }
}
