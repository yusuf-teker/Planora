package com.yusufteker.planora.feature.home.data.paging

import app.cash.paging.ExperimentalPagingApi
import app.cash.paging.LoadType
import app.cash.paging.PagingState
import app.cash.paging.RemoteMediator
import com.yusufteker.planora.core.database.PostEntity
import com.yusufteker.planora.core.database.PlanoraDatabase
import com.yusufteker.planora.feature.home.data.api.FeedApi

// 1. REMOTE MEDIATOR NEDİR?
// Burası, API (Ağ) ile Yerel Veritabanı (SQLDelight) arasındaki köprüdür.
// Kullanıcı listeyi kaydırıp sonuna geldiğinde Paging sistemi API isteğini buraya yönlendirir.
@OptIn(ExperimentalPagingApi::class)
class FeedRemoteMediator(
    private val localDatabase: PlanoraDatabase,
    private val feedApi: FeedApi,
    private val topic: String?,
    private val ownerId: String
) : RemoteMediator<Int, PostEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostEntity>
    ): MediatorResult {
        return try {
            // 2. KAYDIRMA (SCROLL) OLAYININ YAKALANMASI:
            // loadType bize kullanıcının ne yaptığını söyler.
            val page = when (loadType) {
                // REFRESH: Kullanıcı listeyi en yukarıdan tutup aşağı çekti (Yenileme)
                LoadType.REFRESH -> 1
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                // APPEND: Kullanıcı listeyi aşağı kaydırdı ve listenin sonuna yaklaştı.
                LoadType.APPEND -> {
                    // Sayfa sonuna gelindiğinde sıradaki sayfanın ne olduğunu veritabanından (Remote Key tablosundan) okuruz.
                    val remoteKeyId = if (topic != null) "feed_$topic" else "feed_all"
                    val remoteKey = localDatabase.planoraDatabaseQueries.getRemoteKey(id = remoteKeyId, ownerId = ownerId).executeAsOneOrNull()
                    if (remoteKey?.nextPage == null) {
                        return MediatorResult.Success(endOfPaginationReached = true)
                    }
                    remoteKey.nextPage?.toIntOrNull()?:1
                }
            }

            // 3. API'DEN VERİ ÇEKİLMESİ
            val responseResult = feedApi.getPosts(page = page, limit = state.config.pageSize, topic = topic)
            
            if (responseResult.isFailure) {
                return MediatorResult.Error(responseResult.exceptionOrNull() ?: Exception("Unknown error"))
            }

            val response = responseResult.getOrNull()!!
            val posts = response.posts
            val endOfPaginationReached = posts.isEmpty() || !response.hasMore

            // 4. VERİLERİN YEREL VERİTABANINA YAZILMASI (Offline-First Kuralı)
            // Ağdan dönen veriyi UI'a göndermeyiz. Sadece veritabanına kaydederiz.
            // Çünkü UI (SocialScreen), doğrudan veritabanını dinler!
            localDatabase.transaction {
                if (loadType == LoadType.REFRESH) {
                    if (topic == null) {
                        localDatabase.planoraDatabaseQueries.deleteAllPosts(ownerId = ownerId)
                        localDatabase.planoraDatabaseQueries.deleteAllRemoteKeys(ownerId = ownerId)
                    }
                }

                val remoteKeyId = if (topic != null) "feed_$topic" else "feed_all"
                localDatabase.planoraDatabaseQueries.insertRemoteKey(
                    id = remoteKeyId,
                    ownerId = ownerId,
                    nextPage = response.nextCursor
                )

                posts.forEach { post ->
                    localDatabase.planoraDatabaseQueries.insertPost(
                        id = post.id,
                        ownerId = ownerId,
                        authorId = post.authorId,
                        authorName = post.authorName,
                        authorUsername = post.authorUsername,
                        content = post.content,
                        createdAt = post.createdAt,
                        likesCount = post.likesCount.toLong(),
                        commentsCount = post.commentsCount.toLong(),
                        isLikedByMe = if (post.isLikedByMe) 1L else 0L,
                        isBookmarkedByMe = if (post.isBookmarkedByMe) 1L else 0L,
                        topic = post.topic ?: "Genel"
                    )
                }
            }
            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}
