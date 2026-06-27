package com.yusufteker.pulse.feature.home.data.paging

import app.cash.paging.ExperimentalPagingApi
import app.cash.paging.LoadType
import app.cash.paging.PagingState
import app.cash.paging.RemoteMediator
import com.yusufteker.pulse.core.database.PostEntity
import com.yusufteker.pulse.core.database.PulseDatabase
import com.yusufteker.pulse.feature.home.data.api.FeedApi

// 1. REMOTE MEDIATOR NEDİR?
// Burası, API (Ağ) ile Yerel Veritabanı (SQLDelight) arasındaki köprüdür.
// Kullanıcı listeyi kaydırıp sonuna geldiğinde Paging sistemi API isteğini buraya yönlendirir.
@OptIn(ExperimentalPagingApi::class)
class FeedRemoteMediator(
    private val database: PulseDatabase,
    private val feedApi: FeedApi
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
                    val remoteKey = database.pulseDatabaseQueries.getRemoteKey("feed").executeAsOneOrNull()
                    if (remoteKey?.nextPage == null) {
                        return MediatorResult.Success(endOfPaginationReached = true)
                    }
                    remoteKey.nextPage?.toIntOrNull()?:1
                }
            }

            // 3. API'DEN VERİ ÇEKİLMESİ
            val responseResult = feedApi.getPosts(page = page, limit = state.config.pageSize)
            
            if (responseResult.isFailure) {
                return MediatorResult.Error(responseResult.exceptionOrNull() ?: Exception("Unknown error"))
            }

            val response = responseResult.getOrNull()!!
            val posts = response.posts
            val endOfPaginationReached = posts.isEmpty() || !response.hasMore

            // 4. VERİLERİN YEREL VERİTABANINA YAZILMASI (Offline-First Kuralı)
            // Ağdan dönen veriyi UI'a göndermeyiz. Sadece veritabanına kaydederiz.
            // Çünkü UI (SocialScreen), doğrudan veritabanını dinler!
            database.transaction {
                if (loadType == LoadType.REFRESH) {
                    database.pulseDatabaseQueries.deleteAllPosts()
                    database.pulseDatabaseQueries.deleteAllRemoteKeys()
                }

                database.pulseDatabaseQueries.insertRemoteKey(
                    id = "feed",
                    nextPage = response.nextCursor
                )

                posts.forEach { post ->
                    database.pulseDatabaseQueries.insertPost(
                        id = post.id,
                        authorId = post.authorId,
                        authorName = post.authorName,
                        authorUsername = post.authorUsername,
                        content = post.content,
                        createdAt = post.createdAt,
                        likesCount = post.likesCount.toLong(),
                        commentsCount = post.commentsCount.toLong(),
                        isLikedByMe = if (post.isLikedByMe) 1L else 0L
                    )
                }
            }
            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}
