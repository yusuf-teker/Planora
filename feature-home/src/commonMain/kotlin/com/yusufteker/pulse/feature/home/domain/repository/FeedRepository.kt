package com.yusufteker.pulse.feature.home.domain.repository

import app.cash.paging.ExperimentalPagingApi
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.PagingData
import app.cash.paging.PagingSource
import app.cash.paging.PagingState
import app.cash.paging.map
import app.cash.sqldelight.Query
import com.yusufteker.pulse.core.database.PostEntity
import com.yusufteker.pulse.core.database.PulseDatabase
import com.yusufteker.pulse.feature.home.data.api.FeedApi
import com.yusufteker.pulse.feature.home.data.paging.FeedRemoteMediator
import com.yusufteker.pulse.feature.home.domain.model.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.math.max

// 1. YEREL VERİTABANI SAYFALAMA KAYNAĞI (PagingSource):
// Paging 3 kütüphanesi veritabanından veri okumak için bu sınıfı kullanır.
class FeedPagingSource(
    private val database: PulseDatabase,
    private val topic: String?
) : PagingSource<Int, PostEntity>(), Query.Listener {

    private val query = database.pulseDatabaseQueries.getAllPosts(topic = topic, limit = 0, offset = 0)

    init {
        query.addListener(this)
        registerInvalidatedCallback {
            query.removeListener(this)
        }
    }

    override fun queryResultsChanged() {
        invalidate()
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PostEntity> {
        return withContext(Dispatchers.IO) {
            try {
                val key = params.key ?: 0
                val limit = params.loadSize.toLong()
                val offset = key.toLong()

                val count = database.pulseDatabaseQueries.countAllPosts(topic).executeAsOne()
                val data = database.pulseDatabaseQueries.getAllPosts(topic = topic, limit = limit, offset = offset).executeAsList()

                val nextKey = if (offset + data.size >= count) null else key + data.size
                val prevKey = if (key <= 0) null else max(0, key - params.loadSize)

                LoadResult.Page(
                    data = data,
                    prevKey = prevKey,
                    nextKey = nextKey
                )
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }
    }

    override fun getRefreshKey(state: PagingState<Int, PostEntity>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(state.config.pageSize) ?: anchorPage?.nextKey?.minus(state.config.pageSize)
        }
    }
}

// 2. REPOSITORY PATTERN
// Temiz Mimari'nin kalbidir. SocialViewModel veriyi doğrudan API veya Veritabanından istemez,
// Gelip bu sınıftan (Repository) ister. Repository, verinin nereden alınacağını koordine eder.
class FeedRepository(
    private val database: PulseDatabase,
    private val feedApi: FeedApi
) {

    @OptIn(ExperimentalPagingApi::class)
    fun getFeed(topic: String?): Flow<PagingData<Post>> {
        // Pager nesnesi, Sayfalama (Paging) işleminin orkestra şefidir.
        // Hem RemoteMediator'a (API) hem de PagingSource'a (Veritabanı) bağlanır.
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                initialLoadSize = 20 // Paging3 default olarak ilk açılışta 3 sayfa (60 item) çeker, bunu engellemek için 20'ye sabitledik.
            ),
            remoteMediator = FeedRemoteMediator(
                database,
                feedApi,
                topic
            ),
            pagingSourceFactory = {
                FeedPagingSource(database, topic)
            }
        )
            .flow
            .map { pagingData ->
                // Veritabanından gelen saf SQL nesnelerini (PostEntity),
                // Uygulamanın anladığı saf iş nesnelerine (Post) dönüştürür.
                // Bu sayede UI (Arayüz) veritabanı detaylarıyla uğraşmaz.
                pagingData.map { entity ->
                    Post(
                        id = entity.id,
                        authorId = entity.authorId,
                        authorName = entity.authorName,
                        authorUsername = entity.authorUsername,
                        content = entity.content,
                        createdAt = entity.createdAt,
                        likesCount = entity.likesCount.toInt(),
                        commentsCount = entity.commentsCount.toInt(),
                        isLikedByMe = entity.isLikedByMe == 1L,
                        topic = entity.topic
                    )
                }
            }
    }
}